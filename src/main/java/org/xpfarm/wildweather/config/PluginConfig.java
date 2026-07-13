package org.xpfarm.wildweather.config;

import org.bukkit.configuration.ConfigurationSection;
import org.xpfarm.wildweather.WildWeatherPlugin;

import java.util.List;

/**
 * Configuration manager for the Wild Weather Update plugin
 */
public class PluginConfig {
    
    private final WildWeatherPlugin plugin;

    public PluginConfig(WildWeatherPlugin plugin) {
        this.plugin = plugin;
    }

    // Global settings
    public boolean isDebugEnabled() {
        return plugin.getConfig().getBoolean("global.debug", false);
    }

    public boolean areNaturalTriggersEnabled() {
        return plugin.getConfig().getBoolean("global.natural_triggers", true);
    }

    public int getMaxConcurrentEvents() {
        return plugin.getConfig().getInt("global.max_concurrent_events", 1);
    }

    public long getCheckInterval() {
        return plugin.getConfig().getLong("global.check_interval_ticks", 1200);
    }

    // Monsoon settings
    public boolean isMonsoonEnabled() {
        return plugin.getConfig().getBoolean("monsoon.enabled", true);
    }

    public double getMonsoonChance() {
        return plugin.getConfig().getDouble("monsoon.chance", 0.03);
    }

    public long getMonsoonDuration() {
        return plugin.getConfig().getLong("monsoon.duration_ticks", 48000);
    }

    public String getMonsoonIntensity() {
        return plugin.getConfig().getString("monsoon.intensity", "high");
    }

    public int getMonsoonMinBiomeAltitude() {
        return plugin.getConfig().getInt("monsoon.min_biome_altitude", 58);
    }

    public double getDrainRateBlocksPerMinute() {
        return plugin.getConfig().getDouble("monsoon.drain_rate_blocks_per_minute", 1.0);
    }

    public List<String> getMonsoonEnabledBiomes() {
        return plugin.getConfig().getStringList("monsoon.enabled_biomes");
    }

    // Monsoon effects
    public boolean isMonsoonFloodingEnabled() {
        return getMonsoonEffectsSection().getBoolean("flooding", true);
    }

    public boolean isMonsoonCropEnhancementEnabled() {
        return getMonsoonEffectsSection().getBoolean("crop_enhancement", true);
    }

    public boolean isMonsoonCropDamageEnabled() {
        return getMonsoonEffectsSection().getBoolean("crop_damage", true);
    }

    public boolean isMonsoonVisualEffectsEnabled() {
        return getMonsoonEffectsSection().getBoolean("visual_effects", true);
    }

    public boolean isMonsoonSoundEffectsEnabled() {
        return getMonsoonEffectsSection().getBoolean("sound_effects", true);
    }

    private ConfigurationSection getMonsoonEffectsSection() {
        return plugin.getConfig().getConfigurationSection("monsoon.effects");
    }

    // Flooding settings
    public int getMaxWaterLevel() {
        return plugin.getConfig().getInt("flooding.max_water_level", 3);
    }

    public double getWaterSpreadChance() {
        return plugin.getConfig().getDouble("flooding.water_spread_chance", 0.7);
    }

    public long getDrainCheckInterval() {
        return plugin.getConfig().getLong("flooding.drain_check_interval", 21600);
    }

    public boolean isNaturalDrainageEnabled() {
        return plugin.getConfig().getBoolean("flooding.natural_drainage", true);
    }

    // Performance settings
    public int getMaxBlocksPerTick() {
        return plugin.getConfig().getInt("performance.max_blocks_per_tick", 50);
    }

    public boolean isAsyncProcessingEnabled() {
        return plugin.getConfig().getBoolean("performance.async_processing", true);
    }

    public boolean isBiomeCheckCachingEnabled() {
        return plugin.getConfig().getBoolean("performance.cache_biome_checks", true);
    }

    // Obsidian Storm settings
    public boolean isObsidianStormEnabled() {
        return plugin.getConfig().getBoolean("obsidian_storm.enabled", true);
    }

    public double getObsidianStormChance() {
        return plugin.getConfig().getDouble("obsidian_storm.chance", 0.001);
    }

    public long getObsidianStormDuration() {
        return plugin.getConfig().getLong("obsidian_storm.duration_ticks", 3600);
    }

    public long getObsidianStormCooldownHours() {
        return plugin.getConfig().getLong("obsidian_storm.cooldown_hours", 168);
    }

    public int getObsidianStormMinAltitude() {
        return plugin.getConfig().getInt("obsidian_storm.min_altitude", 40);
    }

    public int getObsidianStormMaxAltitude() {
        return plugin.getConfig().getInt("obsidian_storm.max_altitude", 120);
    }

    public List<String> getObsidianStormEnabledBiomes() {
        return plugin.getConfig().getStringList("obsidian_storm.enabled_biomes");
    }

    // Obsidian Storm - Obsidian Blocks
    public double getObsidianBlockFallSpeed() {
        return plugin.getConfig().getDouble("obsidian_storm.obsidian_blocks.fall_speed", 2.0);
    }

    public int getObsidianBlockSpawnHeightOffset() {
        return plugin.getConfig().getInt("obsidian_storm.obsidian_blocks.spawn_height_offset", 40);
    }

    public int getObsidianBlockBaseFrequency() {
        return plugin.getConfig().getInt("obsidian_storm.obsidian_blocks.base_frequency", 3);
    }

    public int getObsidianBlockMaxFrequency() {
        return plugin.getConfig().getInt("obsidian_storm.obsidian_blocks.max_frequency", 15);
    }

    // Obsidian Storm - Explosion
    public double getObsidianStormBasePower() {
        return plugin.getConfig().getDouble("obsidian_storm.explosion.base_power", 8.0);
    }

    public double getObsidianStormMaxPower() {
        return plugin.getConfig().getDouble("obsidian_storm.explosion.max_power", 16.0);
    }

    public double getObsidianStormCraterObsidianChance() {
        return plugin.getConfig().getDouble("obsidian_storm.explosion.crater_obsidian_chance", 0.8);
    }

    public double getObsidianStormFireSpreadChance() {
        return plugin.getConfig().getDouble("obsidian_storm.explosion.fire_spread_chance", 0.6);
    }

    public boolean getObsidianStormDestroyAllBlocks() {
        return plugin.getConfig().getBoolean("obsidian_storm.explosion.destroy_all_blocks", true);
    }

    // Obsidian Storm - Intensity
    public int getObsidianStormMinIntensity() {
        return plugin.getConfig().getInt("obsidian_storm.intensity.min", 1);
    }

    public int getObsidianStormMaxIntensity() {
        return plugin.getConfig().getInt("obsidian_storm.intensity.max", 5);
    }

    public double getObsidianStormEscalationRate() {
        return plugin.getConfig().getDouble("obsidian_storm.intensity.escalation_rate", 0.2);
    }

    // Obsidian Storm - Effects
    public boolean isObsidianStormScreenShakeEnabled() {
        return plugin.getConfig().getBoolean("obsidian_storm.effects.screen_shake", true);
    }

    public boolean isObsidianStormThunderSoundsEnabled() {
        return plugin.getConfig().getBoolean("obsidian_storm.effects.thunder_sounds", true);
    }

    public boolean isObsidianStormSmokeParticlesEnabled() {
        return plugin.getConfig().getBoolean("obsidian_storm.effects.smoke_particles", true);
    }

    public boolean isObsidianStormFireEffectsEnabled() {
        return plugin.getConfig().getBoolean("obsidian_storm.effects.fire_effects", true);
    }

    public boolean isObsidianStormCraterCoolingEnabled() {
        return plugin.getConfig().getBoolean("obsidian_storm.effects.crater_cooling", true);
    }
}
