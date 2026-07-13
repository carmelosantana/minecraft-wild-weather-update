package org.xpfarm.wildweather;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.xpfarm.wildweather.api.WeatherAPI;
import org.xpfarm.wildweather.commands.WeatherCommand;
import org.xpfarm.wildweather.config.PluginConfig;
import org.xpfarm.wildweather.events.MonsoonEvent;
import org.xpfarm.wildweather.events.ObsidianStormEvent;
import org.xpfarm.wildweather.listeners.NaturalWeatherListener;
import org.xpfarm.wildweather.manager.WeatherManager;

/**
 * Wild Weather Update Plugin - Main class
 * Adds dynamic weather effects to Minecraft with realistic patterns and events
 */
public final class WildWeatherPlugin extends JavaPlugin {

    private WeatherManager weatherManager;
    private PluginConfig pluginConfig;
    private WeatherAPI weatherAPI;

    @Override
    public void onEnable() {
        // Initialize configuration
        saveDefaultConfig();
        this.pluginConfig = new PluginConfig(this);
        
        // Initialize managers
        this.weatherManager = new WeatherManager(this);
        this.weatherAPI = new WeatherAPI(this.weatherManager);
        
        // Register weather events
        registerWeatherEvents();
        
        // Register commands
        registerCommands();
        
        // Register event listeners
        registerEventListeners();
        
        // Start weather checking task
        startWeatherTask();
        
        getLogger().info("Wild Weather Update has been enabled!");
        
        // Send startup message to console
        Bukkit.getConsoleSender().sendMessage(
            Component.text("=== Wild Weather Update ===", NamedTextColor.GOLD)
        );
        Bukkit.getConsoleSender().sendMessage(
            Component.text("Dynamic weather system loaded", NamedTextColor.GREEN)
        );
        Bukkit.getConsoleSender().sendMessage(
            Component.text("Version: ", NamedTextColor.GRAY)
                .append(Component.text(getDescription().getVersion(), NamedTextColor.WHITE))
        );
    }

    @Override
    public void onDisable() {
        // Cancel all active weather events
        if (weatherManager != null) {
            weatherManager.cancelAllEvents();
        }
        
        // Cancel scheduled tasks
        Bukkit.getScheduler().cancelTasks(this);
        
        getLogger().info("Wild Weather Update has been disabled!");
        
        // Send shutdown message to console
        Bukkit.getConsoleSender().sendMessage(
            Component.text("Wild Weather Update disabled", NamedTextColor.RED)
        );
    }

    /**
     * Register all weather events
     */
    private void registerWeatherEvents() {
        weatherManager.registerEvent(new MonsoonEvent(this));
        
        // Register Obsidian Storm if enabled
        if (pluginConfig.isObsidianStormEnabled()) {
            weatherManager.registerEvent(new ObsidianStormEvent(this));
        }
        
        getLogger().info("Registered weather events: Monsoon" + 
                        (pluginConfig.isObsidianStormEnabled() ? ", Obsidian Storm" : ""));
    }

    /**
     * Register plugin commands
     */
    private void registerCommands() {
        WeatherCommand weatherCommand = new WeatherCommand(this);
        getCommand("weather").setExecutor(weatherCommand);
        getCommand("weather").setTabCompleter(weatherCommand);
    }

    /**
     * Register event listeners
     */
    private void registerEventListeners() {
        getServer().getPluginManager().registerEvents(new NaturalWeatherListener(this), this);
    }

    /**
     * Start the weather checking task
     */
    private void startWeatherTask() {
        long checkInterval = pluginConfig.getCheckInterval();
        
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (pluginConfig.areNaturalTriggersEnabled()) {
                weatherManager.checkForNaturalTriggers();
            }
        }, checkInterval, checkInterval);
    }

    /**
     * Get the weather manager
     * @return WeatherManager instance
     */
    public WeatherManager getWeatherManager() {
        return weatherManager;
    }

    /**
     * Get the plugin configuration
     * @return PluginConfig instance
     */
    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    /**
     * Get the weather API
     * @return WeatherAPI instance
     */
    public WeatherAPI getWeatherAPI() {
        return weatherAPI;
    }

    /**
     * Reload the plugin configuration
     */
    public void reloadPluginConfig() {
        reloadConfig();
        this.pluginConfig = new PluginConfig(this);
        getLogger().info("Configuration reloaded!");
    }
}
