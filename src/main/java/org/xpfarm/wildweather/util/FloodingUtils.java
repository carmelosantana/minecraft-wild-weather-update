package org.xpfarm.wildweather.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.xpfarm.wildweather.config.PluginConfig;

/**
 * Utility class for flooding mechanics
 */
public class FloodingUtils {

    /**
     * Check if a location can be flooded
     * @param location The location to check
     * @param config Plugin configuration
     * @return true if the location can be flooded
     */
    public static boolean canFloodLocation(Location location, PluginConfig config) {
        Block block = location.getBlock();
        
        // Check if the location is suitable for flooding
        if (!isFloodableArea(block)) {
            return false;
        }
        
        // Check altitude constraints
        if (location.getY() < config.getMonsoonMinBiomeAltitude()) {
            return false;
        }
        
        // Check if we're in a valid biome
        return config.getMonsoonEnabledBiomes().contains(
            block.getBiome().name().toLowerCase()
        );
    }

    /**
     * Find a suitable block for flooding near the given location
     * @param location The starting location
     * @return A suitable block for flooding or null if none found
     */
    public static Block findSuitableFloodBlock(Location location) {
        Block startBlock = location.getBlock();
        
        // Check the ground level first
        Block groundBlock = location.getWorld().getHighestBlockAt(location);
        if (isFloodableGround(groundBlock)) {
            return groundBlock.getRelative(BlockFace.UP);
        }
        
        // Check surrounding area
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Block testBlock = startBlock.getRelative(x, 0, z);
                if (isFloodableGround(testBlock)) {
                    return testBlock.getRelative(BlockFace.UP);
                }
            }
        }
        
        return null;
    }

    /**
     * Add flood water to a block
     * @param block The block to add water to
     * @param config Plugin configuration
     * @return true if water was added
     */
    public static boolean addFloodWater(Block block, PluginConfig config) {
        if (block.getType() == Material.AIR) {
            // Place new water
            block.setType(Material.WATER);
            Levelled waterData = (Levelled) block.getBlockData();
            waterData.setLevel(0); // Full water block
            block.setBlockData(waterData);
            return true;
        } else if (block.getType() == Material.WATER) {
            // Increase water level if possible
            Levelled waterData = (Levelled) block.getBlockData();
            int currentLevel = waterData.getLevel();
            int maxLevel = config.getMaxWaterLevel();
            
            if (currentLevel > 0) { // 0 is full, 7 is minimum
                waterData.setLevel(Math.max(0, currentLevel - 1));
                block.setBlockData(waterData);
                return true;
            }
        }
        
        return false;
    }

    /**
     * Drain flood water from a block
     * @param block The block to drain water from
     * @param originalLevel The original water level (-1 if no water originally)
     * @return true if the block was completely drained
     */
    public static boolean drainFloodWater(Block block, Integer originalLevel) {
        if (block.getType() != Material.WATER) {
            return true; // Already drained
        }
        
        Levelled waterData = (Levelled) block.getBlockData();
        int currentLevel = waterData.getLevel();
        
        if (originalLevel == null || originalLevel == -1) {
            // No water originally, so drain completely
            if (currentLevel < 7) {
                waterData.setLevel(currentLevel + 1);
                block.setBlockData(waterData);
                return false;
            } else {
                // Remove water completely
                block.setType(Material.AIR);
                return true;
            }
        } else {
            // Restore to original level
            if (currentLevel < originalLevel) {
                waterData.setLevel(currentLevel + 1);
                block.setBlockData(waterData);
                return currentLevel + 1 >= originalLevel;
            } else {
                return true; // Already at original level
            }
        }
    }

    /**
     * Check if an area is suitable for flooding
     * @param block The block to check
     * @return true if the area can be flooded
     */
    private static boolean isFloodableArea(Block block) {
        // Check if the block and surrounding area is suitable
        Material type = block.getType();
        
        // Don't flood in protected areas (you could add WorldGuard integration here)
        // Don't flood near important structures
        
        return !isProtectedBlock(type) && hasLowElevation(block);
    }

    /**
     * Check if a block is suitable as ground for flooding
     * @param block The block to check
     * @return true if it's suitable ground
     */
    private static boolean isFloodableGround(Block block) {
        Material type = block.getType();
        
        return type == Material.GRASS_BLOCK ||
               type == Material.DIRT ||
               type == Material.COARSE_DIRT ||
               type == Material.PODZOL ||
               type == Material.SAND ||
               type == Material.CLAY ||
               type == Material.STONE ||
               type == Material.COBBLESTONE;
    }

    /**
     * Check if a block type should be protected from flooding
     * @param type The material type
     * @return true if the block should be protected
     */
    private static boolean isProtectedBlock(Material type) {
        // Protect player-built structures and important blocks
        return type.name().contains("DOOR") ||
               type.name().contains("BED") ||
               type.name().contains("CHEST") ||
               type.name().contains("FURNACE") ||
               type.name().contains("ANVIL") ||
               type.name().contains("ENCHANTING") ||
               type.name().contains("CRAFTING") ||
               type == Material.BEACON ||
               type == Material.CONDUIT ||
               type == Material.SPAWNER;
    }

    /**
     * Check if a block is at low elevation (suitable for water accumulation)
     * @param block The block to check
     * @return true if it's low elevation
     */
    private static boolean hasLowElevation(Block block) {
        Location loc = block.getLocation();
        int currentY = loc.getBlockY();
        
        // Check if this is a low point relative to surrounding area
        int higherBlocks = 0;
        int totalChecked = 0;
        
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (x == 0 && z == 0) continue;
                
                int surroundingY = loc.getWorld().getHighestBlockYAt(
                    loc.getBlockX() + x, loc.getBlockZ() + z
                );
                
                if (surroundingY > currentY) {
                    higherBlocks++;
                }
                totalChecked++;
            }
        }
        
        // If more than 60% of surrounding blocks are higher, this is a good flooding spot
        return (double) higherBlocks / totalChecked > 0.6;
    }

    /**
     * Get the water spread chance based on surrounding conditions
     * @param block The block to check around
     * @param baseChance The base spread chance
     * @return Modified spread chance
     */
    public static double getWaterSpreadChance(Block block, double baseChance) {
        // Increase chance if there's already water nearby
        int nearbyWater = countNearbyWater(block, 2);
        double modifier = 1.0 + (nearbyWater * 0.1);
        
        // Increase chance if it's lower elevation
        if (hasLowElevation(block)) {
            modifier += 0.3;
        }
        
        return Math.min(1.0, baseChance * modifier);
    }

    /**
     * Count water blocks near the given block
     * @param block The center block
     * @param radius The radius to check
     * @return Number of water blocks found
     */
    private static int countNearbyWater(Block block, int radius) {
        int count = 0;
        Location center = block.getLocation();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block nearby = center.clone().add(x, y, z).getBlock();
                    if (nearby.getType() == Material.WATER) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }
}
