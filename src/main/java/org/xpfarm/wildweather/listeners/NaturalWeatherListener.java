package org.xpfarm.wildweather.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.xpfarm.wildweather.WildWeatherPlugin;

/**
 * Listener for natural weather events that might trigger custom weather events
 */
public class NaturalWeatherListener implements Listener {
    
    private final WildWeatherPlugin plugin;

    public NaturalWeatherListener(WildWeatherPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Listen for weather changes that might trigger custom events
     */
    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!plugin.getPluginConfig().areNaturalTriggersEnabled()) {
            return;
        }

        // If weather is changing to rain/storm, it might be a good time to check for triggers
        if (event.toWeatherState()) {
            // Weather is changing to rain/storm
            if (plugin.getPluginConfig().isDebugEnabled()) {
                plugin.getLogger().info("Weather changed to rain in world " + event.getWorld().getName() + 
                                      " - checking for weather event triggers");
            }
            
            // We don't trigger immediately on weather change to avoid conflicts
            // The natural trigger check will handle this in the next cycle
        }
    }

    /**
     * Initialize weather checking when a world loads
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (plugin.getPluginConfig().isDebugEnabled()) {
            plugin.getLogger().info("World " + event.getWorld().getName() + " loaded - weather system active");
        }
    }
}
