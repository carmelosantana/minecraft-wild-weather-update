package org.xpfarm.wildweather.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.xpfarm.wildweather.WildWeatherPlugin;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Obsidian Storm - Catastrophic celestial bombardment event
 * Massive obsidian blocks fall from the sky, creating devastating explosions
 */
public class ObsidianStormEvent extends WeatherEvent implements Listener {
    
    private final Set<UUID> fallingBlocks;
    private final Map<Location, Long> craterLocations;
    private BukkitRunnable bombardmentTask;
    private BukkitRunnable effectsTask;
    private BukkitRunnable coolingTask;
    private double currentIntensity;
    private int blocksSpawned;
    private boolean registered = false;

    public ObsidianStormEvent(WildWeatherPlugin plugin) {
        super(plugin, "Obsidian Storm", plugin.getPluginConfig().getObsidianStormDuration());
        this.fallingBlocks = new HashSet<>();
        this.craterLocations = new HashMap<>();
        this.currentIntensity = plugin.getPluginConfig().getObsidianStormMinIntensity();
        this.blocksSpawned = 0;
    }

    @Override
    protected void onStart() {
        // Register event listener for falling block impacts
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        
        // Set dramatic weather
        world.setStorm(true);
        world.setThundering(true);
        world.setWeatherDuration((int) duration);
        world.setThunderDuration((int) duration);
        
        // Start the bombardment
        startBombardmentTask();
        
        // Start visual and sound effects
        startEffectsTask();
        
        // Start crater cooling effects
        if (plugin.getPluginConfig().isObsidianStormCraterCoolingEnabled()) {
            startCoolingTask();
        }
        
        // Notify nearby players with dramatic title
        notifyNearbyPlayersStart();
    }

    @Override
    protected void onTick() {
        // Update intensity over time
        updateIntensity();
        
        // Clean up finished falling blocks
        fallingBlocks.removeIf(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            return entity == null || !entity.isValid();
        });
    }

    @Override
    protected void onEnd() {
        // Cancel all tasks
        if (bombardmentTask != null && !bombardmentTask.isCancelled()) {
            bombardmentTask.cancel();
        }
        if (effectsTask != null && !effectsTask.isCancelled()) {
            effectsTask.cancel();
        }
        if (coolingTask != null && !coolingTask.isCancelled()) {
            coolingTask.cancel();
        }
        
        // Unregister event listener
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
        
        // Clean up any remaining falling blocks
        for (UUID uuid : fallingBlocks) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        fallingBlocks.clear();
        
        // Gradually restore weather
        world.setStorm(false);
        world.setThundering(false);
        
        // Notify nearby players
        notifyNearbyPlayersEnd();
        
        plugin.getLogger().info("Obsidian Storm ended. Total blocks spawned: " + blocksSpawned);
    }

    @Override
    public boolean canTriggerAt(Location location) {
        Biome biome = location.getBlock().getBiome();
        return getAllowedBiomes().contains(biome) && 
               location.getY() >= plugin.getPluginConfig().getObsidianStormMinAltitude() &&
               location.getY() <= plugin.getPluginConfig().getObsidianStormMaxAltitude() &&
               !world.hasStorm(); // Don't trigger during existing storms
    }

    @Override
    public List<Biome> getAllowedBiomes() {
        List<Biome> biomes = new ArrayList<>();
        List<String> configBiomes = plugin.getPluginConfig().getObsidianStormEnabledBiomes();
        
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
                plugin.getLogger().warning("Invalid biome name in obsidian_storm config: " + biomeName);
            }
        }
        return biomes;
    }

    @Override
    public double getTriggerChance() {
        return plugin.getPluginConfig().getObsidianStormChance();
    }

    /**
     * Start the main bombardment task that spawns falling obsidian blocks
     */
    private void startBombardmentTask() {
        bombardmentTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                
                spawnObsidianBlock();
            }
        };
        
        // Start immediately and repeat based on current intensity
        long intervalTicks = calculateBombardmentInterval();
        bombardmentTask.runTaskTimer(plugin, 0L, intervalTicks);
    }

    /**
     * Start the effects task for environmental effects
     */
    private void startEffectsTask() {
        effectsTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                
                createEnvironmentalEffects();
            }
        };
        
        effectsTask.runTaskTimer(plugin, 20L, 20L); // Run every second
    }

    /**
     * Start the crater cooling effects task
     */
    private void startCoolingTask() {
        coolingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                
                createCoolingEffects();
            }
        };
        
        coolingTask.runTaskTimer(plugin, 60L, 60L); // Run every 3 seconds
    }

    /**
     * Spawn a falling obsidian block at a random location
     */
    private void spawnObsidianBlock() {
        // Find random location within radius
        int radius = (int) (64 + (currentIntensity * 32)); // Radius increases with intensity
        Location spawnLoc = findRandomSpawnLocation(radius);
        
        if (spawnLoc == null) return;
        
        // Calculate spawn height
        int spawnHeight = spawnLoc.getBlockY() + plugin.getPluginConfig().getObsidianBlockSpawnHeightOffset();
        spawnLoc.setY(Math.min(spawnHeight, world.getMaxHeight() - 10));
        
        // Create falling block
        FallingBlock fallingBlock = world.spawnFallingBlock(spawnLoc, Material.OBSIDIAN.createBlockData());
        
        // Set properties
        fallingBlock.setDropItem(false); // Don't drop items when landing
        fallingBlock.setHurtEntities(true); // Damage entities
        // Note: setFallDamage is not available in 1.21, entities take damage based on fall distance
        
        // Set velocity for faster fall
        double fallSpeed = plugin.getPluginConfig().getObsidianBlockFallSpeed();
        fallingBlock.setVelocity(new Vector(0, -fallSpeed, 0));
        
        // Track the falling block
        fallingBlocks.add(fallingBlock.getUniqueId());
        blocksSpawned++;
        
        // Play dramatic sound effect
        if (plugin.getPluginConfig().isObsidianStormThunderSoundsEnabled()) {
            world.playSound(spawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
            world.playSound(spawnLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);
        }
        
        // Visual effects at spawn
        createSpawnEffects(spawnLoc);
    }

    /**
     * Handle falling block impact and create explosion
     */
    @EventHandler
    public void onBlockLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) return;
        if (!fallingBlocks.contains(event.getEntity().getUniqueId())) return;
        
        FallingBlock fallingBlock = (FallingBlock) event.getEntity();
        Location impactLoc = fallingBlock.getLocation();
        
        // Remove from tracking
        fallingBlocks.remove(fallingBlock.getUniqueId());
        
        // Cancel the block placement
        event.setCancelled(true);
        
        // Create massive explosion
        createExplosion(impactLoc);
        
        // Track crater location
        craterLocations.put(impactLoc.clone(), System.currentTimeMillis());
    }

    /**
     * Create a devastating explosion at the impact location
     */
    private void createExplosion(Location location) {
        // Calculate explosion power based on intensity
        double basePower = plugin.getPluginConfig().getObsidianStormBasePower();
        double maxPower = plugin.getPluginConfig().getObsidianStormMaxPower();
        double power = basePower + ((maxPower - basePower) * (currentIntensity / plugin.getPluginConfig().getObsidianStormMaxIntensity()));
        
        // Create the explosion
        if (plugin.getPluginConfig().getObsidianStormDestroyAllBlocks()) {
            // Custom explosion that destroys everything
            createCustomExplosion(location, power);
        } else {
            // Standard explosion
            world.createExplosion(location, (float) power, plugin.getPluginConfig().isObsidianStormFireEffectsEnabled());
        }
        
        // Line crater with obsidian
        createObsidianCrater(location, (int) (power / 2));
        
        // Screen shake effect for nearby players
        if (plugin.getPluginConfig().isObsidianStormScreenShakeEnabled()) {
            createScreenShakeEffect(location, power);
        }
        
        // Dramatic sound effects
        world.playSound(location, Sound.ENTITY_ENDER_DRAGON_DEATH, 3.0f, 0.3f);
        world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 5.0f, 0.1f);
        world.playSound(location, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.1f);
    }

    /**
     * Create a custom explosion that destroys all blocks
     */
    private void createCustomExplosion(Location center, double power) {
        int radius = (int) power;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x*x + y*y + z*z);
                    if (distance <= power) {
                        Location blockLoc = center.clone().add(x, y, z);
                        Block block = blockLoc.getBlock();
                        
                        if (!block.getType().isAir()) {
                            // Add fire chance
                            if (plugin.getPluginConfig().isObsidianStormFireEffectsEnabled() && 
                                ThreadLocalRandom.current().nextDouble() < plugin.getPluginConfig().getObsidianStormFireSpreadChance()) {
                                block.setType(Material.FIRE);
                            } else {
                                block.setType(Material.AIR);
                            }
                        }
                    }
                }
            }
        }
        
        // Create visual explosion effect
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        world.spawnParticle(Particle.LARGE_SMOKE, center, 50, radius/2.0, radius/2.0, radius/2.0, 0.1);
    }

    /**
     * Create an obsidian-lined crater at the impact site
     */
    private void createObsidianCrater(Location center, int radius) {
        double obsidianChance = plugin.getPluginConfig().getObsidianStormCraterObsidianChance();
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x*x + z*z);
                if (distance <= radius) {
                    Location blockLoc = center.clone().add(x, -1, z);
                    Block block = blockLoc.getBlock();
                    
                    // Higher chance for obsidian at crater edges
                    double chance = distance > radius * 0.7 ? obsidianChance : obsidianChance * 0.5;
                    
                    if (ThreadLocalRandom.current().nextDouble() < chance) {
                        block.setType(Material.OBSIDIAN);
                    }
                }
            }
        }
        
        // Always place obsidian at center
        center.clone().add(0, -1, 0).getBlock().setType(Material.OBSIDIAN);
    }

    /**
     * Create screen shake effect for nearby players
     */
    private void createScreenShakeEffect(Location location, double power) {
        double maxDistance = power * 8; // Shake effect radius
        
        for (Player player : world.getPlayers()) {
            double distance = player.getLocation().distance(location);
            if (distance <= maxDistance) {
                // Create shake effect by rapidly changing player's view
                Vector randomOffset = new Vector(
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.5,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.5,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.5
                );
                
                // Apply temporary velocity for shake effect
                player.setVelocity(randomOffset);
                
                // Play screen flash effect
                player.showTitle(Title.title(
                    Component.empty(),
                    Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(100), Duration.ZERO)
                ));
            }
        }
    }

    /**
     * Find a random spawn location within the given radius
     */
    private Location findRandomSpawnLocation(int radius) {
        for (int attempts = 0; attempts < 10; attempts++) {
            int x = centerLocation.getBlockX() + ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int z = centerLocation.getBlockZ() + ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            
            // Find surface level
            Location testLoc = new Location(world, x, world.getHighestBlockYAt(x, z), z);
            
            if (canTriggerAt(testLoc)) {
                return testLoc;
            }
        }
        
        return null; // Couldn't find suitable location
    }

    /**
     * Update the storm intensity over time
     */
    private void updateIntensity() {
        double escalationRate = plugin.getPluginConfig().getObsidianStormEscalationRate();
        double maxIntensity = plugin.getPluginConfig().getObsidianStormMaxIntensity();
        
        // Increase intensity over time
        long elapsedTicks = (System.currentTimeMillis() - startTime) / 50; // Convert to ticks
        double timeProgress = (double) elapsedTicks / duration;
        
        currentIntensity = Math.min(maxIntensity, 
            plugin.getPluginConfig().getObsidianStormMinIntensity() + (timeProgress * escalationRate * maxIntensity));
    }

    /**
     * Calculate bombardment interval based on current intensity
     */
    private long calculateBombardmentInterval() {
        int baseFreq = plugin.getPluginConfig().getObsidianBlockBaseFrequency();
        int maxFreq = plugin.getPluginConfig().getObsidianBlockMaxFrequency();
        
        // Calculate blocks per minute based on intensity
        double blocksPerMinute = baseFreq + ((maxFreq - baseFreq) * (currentIntensity / plugin.getPluginConfig().getObsidianStormMaxIntensity()));
        
        // Convert to ticks between spawns
        return Math.max(1L, (long) (1200 / blocksPerMinute)); // 1200 ticks = 1 minute
    }

    /**
     * Create environmental effects during the storm
     */
    private void createEnvironmentalEffects() {
        // Smoke particles around center
        if (plugin.getPluginConfig().isObsidianStormSmokeParticlesEnabled()) {
            world.spawnParticle(Particle.LARGE_SMOKE, centerLocation, 20, 32, 8, 32, 0.1);
            world.spawnParticle(Particle.ASH, centerLocation, 50, 64, 16, 64, 0.05);
        }
        
        // Ambient thunder sounds
        if (plugin.getPluginConfig().isObsidianStormThunderSoundsEnabled() && 
            ThreadLocalRandom.current().nextDouble() < 0.3) {
            world.playSound(centerLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 
                           0.5f + ThreadLocalRandom.current().nextFloat() * 0.5f);
        }
    }

    /**
     * Create cooling effects at crater locations
     */
    private void createCoolingEffects() {
        Iterator<Map.Entry<Location, Long>> iterator = craterLocations.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<Location, Long> entry = iterator.next();
            Location craterLoc = entry.getKey();
            long creationTime = entry.getValue();
            
            // Remove old craters (after 5 minutes)
            if (System.currentTimeMillis() - creationTime > 300000) {
                iterator.remove();
                continue;
            }
            
            // Create smoke and steam effects
            world.spawnParticle(Particle.SMOKE, craterLoc, 5, 2, 1, 2, 0.02);
            world.spawnParticle(Particle.WHITE_ASH, craterLoc, 3, 1, 1, 1, 0.01);
        }
    }

    /**
     * Create visual effects at spawn location
     */
    private void createSpawnEffects(Location location) {
        world.spawnParticle(Particle.PORTAL, location, 30, 2, 2, 2, 0.1);
        world.spawnParticle(Particle.FLAME, location, 10, 1, 1, 1, 0.05);
    }

    /**
     * Notify nearby players when the storm starts
     */
    private void notifyNearbyPlayersStart() {
        Component title = Component.text("🛢️ OBSIDIAN STORM", NamedTextColor.DARK_RED);
        Component subtitle = Component.text("Seek immediate shelter!", NamedTextColor.RED);
        
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(centerLocation) <= 200) {
                player.showTitle(Title.title(title, subtitle, 
                    Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(4), Duration.ofSeconds(1))));
                
                player.sendMessage(Component.text("⚠️ ", NamedTextColor.RED)
                    .append(Component.text("A catastrophic Obsidian Storm has begun! Find shelter immediately!", NamedTextColor.YELLOW)));
            }
        }
    }

    /**
     * Notify nearby players when the storm ends
     */
    private void notifyNearbyPlayersEnd() {
        Component message = Component.text("🌤️ ", NamedTextColor.GOLD)
            .append(Component.text("The Obsidian Storm has passed. Beware of the scorched craters.", NamedTextColor.GRAY));
        
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(centerLocation) <= 200) {
                player.sendMessage(message);
            }
        }
    }
}
