package org.xpfarm.wildweather.api;

import org.bukkit.Location;
import org.xpfarm.wildweather.events.WeatherEvent;
import org.xpfarm.wildweather.manager.WeatherManager;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Public API for the Wild Weather Update plugin
 * Allows other plugins to interact with weather events
 */
public class WeatherAPI {
    
    private final WeatherManager weatherManager;

    public WeatherAPI(WeatherManager weatherManager) {
        this.weatherManager = weatherManager;
    }

    /**
     * Register a custom weather event
     * @param event The weather event to register
     */
    public void registerEvent(WeatherEvent event) {
        weatherManager.registerEvent(event);
    }

    /**
     * Trigger a weather event at a specific location
     * @param eventName The name of the event to trigger
     * @param location The location where to trigger the event
     * @return true if the event was triggered successfully
     */
    public boolean triggerEvent(String eventName, Location location) {
        return weatherManager.triggerEvent(eventName, location);
    }

    /**
     * Trigger a weather event at a player's location
     * @param eventName The name of the event to trigger
     * @param playerName The name of the player
     * @return true if the event was triggered successfully
     */
    public boolean triggerEventAtPlayer(String eventName, String playerName) {
        return weatherManager.triggerEventAtPlayer(eventName, playerName);
    }

    /**
     * Cancel a specific weather event
     * @param eventId The UUID of the event to cancel
     * @return true if the event was cancelled
     */
    public boolean cancelEvent(UUID eventId) {
        return weatherManager.cancelEvent(eventId);
    }

    /**
     * Cancel all active weather events
     */
    public void cancelAllEvents() {
        weatherManager.cancelAllEvents();
    }

    /**
     * Get all registered event names
     * @return Set of event names
     */
    public Set<String> getRegisteredEvents() {
        return weatherManager.getRegisteredEventNames();
    }

    /**
     * Get all currently active weather events
     * @return Set of active weather events
     */
    public Set<WeatherEvent> getActiveEvents() {
        return weatherManager.getActiveEvents();
    }

    /**
     * Check if a specific event is currently active
     * @param eventName The name of the event
     * @return true if the event is active
     */
    public boolean isEventActive(String eventName) {
        return weatherManager.isEventActive(eventName);
    }

    /**
     * Get a registered weather event by name
     * @param eventName The name of the event
     * @return The weather event or null if not found
     */
    public WeatherEvent getEvent(String eventName) {
        return weatherManager.getRegisteredEvent(eventName);
    }

    /**
     * Get status information about the weather system
     * @return Map containing status information
     */
    public Map<String, Object> getStatus() {
        return weatherManager.getStatus();
    }
}
