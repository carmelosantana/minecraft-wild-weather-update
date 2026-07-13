package org.xpfarm.wildweather.events;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.xpfarm.wildweather.WildWeatherPlugin;
import org.xpfarm.wildweather.util.FloodingUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Monsoon weather event - Heavy rain with flooding effects
 */
public class MonsoonEvent extends WeatherEvent {
    
    private final Set<Location> floodedBlocks;
    private final Map<Location, Integer> originalWaterLevels;
    private BukkitRunnable drainageTask;
    private BukkitRunnable floodingTask;
    private BukkitRunnable effectsTask;
    
    public MonsoonEvent(WildWeatherPlugin plugin) {
        super(plugin, "Monsoon", plugin.getPluginConfig().getMonsoonDuration());
        this.floodedBlocks = new HashSet<>();
        this.originalWaterLevels = new HashMap<>();
    }

    @Override
    protected void onStart() {
        // Set weather to rain with thunder
        world.setStorm(true);
        world.setThundering(true);
        world.setWeatherDuration((int) duration);
        world.setThunderDuration((int) duration);
        
        // Start flooding effects
        startFloodingTask();
        
        // Start drainage task
        startDrainageTask();
        
        // Start visual and sound effects
        startEffectsTask();
        
        // Notify nearby players
        notifyNearbyPlayers();
    }

    @Override
    protected void onTick() {
        // Main tick logic is handled by the scheduled tasks
        // This method can be used for additional per-tick operations if needed
    }

    @Override
    protected void onEnd() {
        // Stop all tasks
        if (floodingTask != null && !floodingTask.isCancelled()) {
            floodingTask.cancel();
        }
        if (drainageTask != null && !drainageTask.isCancelled()) {
            drainageTask.cancel();
        }
        if (effectsTask != null && !effectsTask.isCancelled()) {
            effectsTask.cancel();
        }
        
        // Gradually restore weather
        world.setStorm(false);
        world.setThundering(false);
        
        // Begin accelerated drainage
        if (plugin.getPluginConfig().isNaturalDrainageEnabled()) {
            startAcceleratedDrainage();
        }
        
        // Notify nearby players
        notifyEventEnd();
    }

    @Override
    public boolean canTriggerAt(Location location) {
        if (location.getWorld().hasStorm()) {
            Biome biome = location.getBlock().getBiome();
            return getAllowedBiomes().contains(biome) && 
                   location.getY() >= plugin.getPluginConfig().getMonsoonMinBiomeAltitude();
        }
        return false;
    }

    @Override
    public List<Biome> getAllowedBiomes() {
        List<Biome> biomes = new ArrayList<>();
        List<String> configBiomes = plugin.getPluginConfig().getMonsoonEnabledBiomes();
        
        // Check if "all" is specified
        if (configBiomes.contains("all") || configBiomes.contains("ALL")) {
            // Return all available biomes
            for (Biome biome : Biome.values()) {
                biomes.add(biome);
            }
            return biomes;
        }
        
        // Parse specific biomes
        for (String biomeName : configBiomes) {
            try {
                biomes.add(Biome.valueOf(biomeName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid biome name in config: " + biomeName);
            }
        }
        return biomes;
    }

    @Override
    public double getTriggerChance() {
        return plugin.getPluginConfig().getMonsoonChance();
    }

    /**
     * Start the flooding task that creates water accumulation
     */
    private void startFloodingTask() {
        if (!plugin.getPluginConfig().isMonsoonFloodingEnabled()) return;
        
        floodingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                
                int radius = 64; // Flooding radius around center
                int blocksProcessed = 0;
                int maxBlocks = plugin.getPluginConfig().getMaxBlocksPerTick();
                
                for (int x = centerLocation.getBlockX() - radius; x <= centerLocation.getBlockX() + radius && blocksProcessed < maxBlocks; x++) {
                    for (int z = centerLocation.getBlockZ() - radius; z <= centerLocation.getBlockZ() + radius && blocksProcessed < maxBlocks; z++) {
                        Location loc = new Location(world, x, centerLocation.getY(), z);
                        
                        // Check if location is suitable for flooding
                        if (FloodingUtils.canFloodLocation(loc, plugin.getPluginConfig())) {
                            if (ThreadLocalRandom.current().nextDouble() < plugin.getPluginConfig().getWaterSpreadChance()) {
                                createFloodWater(loc);
                                blocksProcessed++;
                            }
                        }
                    }
                }
            }
        };
        
        floodingTask.runTaskTimer(plugin, 20L, 40L); // Run every 2 seconds
    }

    /**
     * Start the drainage task that removes flood water over time
     */
    private void startDrainageTask() {
        if (!plugin.getPluginConfig().isNaturalDrainageEnabled()) return;
        
        drainageTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                
                drainFloodWater();
            }
        };
        
        drainageTask.runTaskTimer(plugin, plugin.getPluginConfig().getDrainCheckInterval(), 
                                 plugin.getPluginConfig().getDrainCheckInterval());
    }

    /**
     * Start effects task for visual and sound effects
     */
    private void startEffectsTask() {
        effectsTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                
                // Play thunder sounds and lightning effects
                if (plugin.getPluginConfig().isMonsoonSoundEffectsEnabled()) {
                    playThunderEffects();
                }
                
                // Create visual effects
                if (plugin.getPluginConfig().isMonsoonVisualEffectsEnabled()) {
                    createVisualEffects();
                }
            }
        };
        
        effectsTask.runTaskTimer(plugin, 60L, 100L + ThreadLocalRandom.current().nextLong(40)); // Irregular intervals
    }

    /**
     * Create flood water at the specified location
     */
    private void createFloodWater(Location location) {
        Block block = FloodingUtils.findSuitableFloodBlock(location);
        if (block == null) return;
        
        Location floodLoc = block.getLocation();
        
        // Store original state if not already stored
        if (!originalWaterLevels.containsKey(floodLoc)) {
            if (block.getType() == Material.WATER) {
                Levelled waterData = (Levelled) block.getBlockData();
                originalWaterLevels.put(floodLoc, waterData.getLevel());
            } else {
                originalWaterLevels.put(floodLoc, -1); // Indicates no water originally
            }
        }
        
        // Add or increase water level
        FloodingUtils.addFloodWater(block, plugin.getPluginConfig());
        floodedBlocks.add(floodLoc);
    }

    /**
     * Drain flood water according to the configured rate
     */
    private void drainFloodWater() {
        double drainRate = plugin.getPluginConfig().getDrainRateBlocksPerMinute();
        int blocksToDrain = Math.max(1, (int) Math.ceil(drainRate));
        
        Iterator<Location> iterator = floodedBlocks.iterator();
        int drained = 0;
        
        while (iterator.hasNext() && drained < blocksToDrain) {
            Location floodLoc = iterator.next();
            Block block = floodLoc.getBlock();
            
            if (FloodingUtils.drainFloodWater(block, originalWaterLevels.get(floodLoc))) {
                iterator.remove();
                originalWaterLevels.remove(floodLoc);
                drained++;
            }
        }
    }

    /**
     * Start accelerated drainage when the event ends
     */
    private void startAcceleratedDrainage() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (floodedBlocks.isEmpty()) {
                    cancel();
                    return;
                }
                
                // Drain faster when event ends
                drainFloodWater();
                drainFloodWater(); // Drain twice as fast
            }
        }.runTaskTimer(plugin, 20L, 200L); // Every 10 seconds
    }

    /**
     * Play thunder effects for nearby players
     */
    private void playThunderEffects() {
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(centerLocation) <= 128) {
                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                    world.strikeLightningEffect(player.getLocation().add(
                        ThreadLocalRandom.current().nextInt(-32, 33),
                        0,
                        ThreadLocalRandom.current().nextInt(-32, 33)
                    ));
                }
                
                if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                    player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 
                                   SoundCategory.WEATHER, 0.8f, 
                                   0.8f + ThreadLocalRandom.current().nextFloat() * 0.4f);
                }
            }
        }
    }

    /**
     * Create visual effects like particles
     */
    private void createVisualEffects() {
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(centerLocation) <= 64) {
                // Spawn rain particles
                Location particleLoc = player.getLocation().add(
                    ThreadLocalRandom.current().nextInt(-16, 17),
                    ThreadLocalRandom.current().nextInt(5, 15),
                    ThreadLocalRandom.current().nextInt(-16, 17)
                );
                
                world.spawnParticle(Particle.FALLING_WATER, particleLoc, 
                                  5 + ThreadLocalRandom.current().nextInt(10), 
                                  2.0, 0.5, 2.0, 0.1);
            }
        }
    }

    /**
     * Notify nearby players when the monsoon starts
     */
    private void notifyNearbyPlayers() {
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(centerLocation) <= 128) {
                player.sendMessage(ChatColor.DARK_BLUE + "⛈ A heavy monsoon is approaching...");
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 
                               SoundCategory.WEATHER, 1.0f, 0.7f);
            }
        }
    }

    /**
     * Notify nearby players when the monsoon ends
     */
    private void notifyEventEnd() {
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(centerLocation) <= 128) {
                player.sendMessage(ChatColor.AQUA + "☀ The monsoon is clearing up...");
            }
        }
    }

    /**
     * Get information about the current flooding state
     * @return Map of flood information
     */
    public Map<String, Object> getFloodingInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("floodedBlocks", floodedBlocks.size());
        info.put("drainRate", plugin.getPluginConfig().getDrainRateBlocksPerMinute());
        info.put("maxWaterLevel", plugin.getPluginConfig().getMaxWaterLevel());
        info.put("active", active);
        return info;
    }
}
