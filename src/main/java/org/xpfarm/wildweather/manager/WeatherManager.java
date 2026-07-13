package org.xpfarm.wildweather.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.xpfarm.wildweather.WildWeatherPlugin;
import org.xpfarm.wildweather.events.WeatherEvent;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages all weather events and their lifecycle
 */
public class WeatherManager {
    
    private final WildWeatherPlugin plugin;
    private final Map<String, WeatherEvent> registeredEvents;
    private final Set<WeatherEvent> activeEvents;
    private final Map<String, Long> lastTriggerTimes;

    public WeatherManager(WildWeatherPlugin plugin) {
        this.plugin = plugin;
        this.registeredEvents = new HashMap<>();
        this.activeEvents = new HashSet<>();
        this.lastTriggerTimes = new HashMap<>();
        
        startEventTicker();
    }

    /**
     * Register a weather event
     * @param event The weather event to register
     */
    public void registerEvent(WeatherEvent event) {
        registeredEvents.put(event.getName().toLowerCase(), event);
        plugin.getLogger().info("Registered weather event: " + event.getName());
    }

    /**
     * Unregister a weather event
     * @param eventName The name of the event to unregister
     */
    public void unregisterEvent(String eventName) {
        WeatherEvent event = registeredEvents.remove(eventName.toLowerCase());
        if (event != null && event.isActive()) {
            event.end();
            activeEvents.remove(event);
        }
    }

    /**
     * Trigger a weather event manually
     * @param eventName The name of the event to trigger
     * @param location The location where to trigger the event
     * @return true if the event was triggered successfully
     */
    public boolean triggerEvent(String eventName, Location location) {
        WeatherEvent prototype = registeredEvents.get(eventName.toLowerCase());
        if (prototype == null) {
            return false;
        }

        // Check if we've reached max concurrent events
        if (activeEvents.size() >= plugin.getPluginConfig().getMaxConcurrentEvents()) {
            plugin.getLogger().info("Cannot trigger " + eventName + ": max concurrent events reached");
            return false;
        }

        // Check if the event can trigger at this location
        if (!prototype.canTriggerAt(location)) {
            plugin.getLogger().info("Cannot trigger " + eventName + " at location: conditions not met");
            return false;
        }

        // Create a new instance of the event (clone the prototype)
        WeatherEvent newEvent = createEventInstance(prototype, location);
        if (newEvent == null) {
            return false;
        }

        // Start the event
        newEvent.start(location.getWorld(), location);
        activeEvents.add(newEvent);
        
        // Update last trigger time
        lastTriggerTimes.put(eventName.toLowerCase(), System.currentTimeMillis());
        
        plugin.getLogger().info("Triggered weather event: " + eventName + " at " + 
                               location.getBlockX() + ", " + location.getBlockZ());
        
        return true;
    }

    /**
     * Force trigger a weather event (bypasses condition checks)
     * @param eventName The name of the event to trigger
     * @param location The location where to trigger the event
     * @return true if the event was triggered successfully
     */
    public boolean forceEvent(String eventName, Location location) {
        WeatherEvent prototype = registeredEvents.get(eventName.toLowerCase());
        if (prototype == null) {
            return false;
        }

        // Check if we've reached max concurrent events
        if (activeEvents.size() >= plugin.getPluginConfig().getMaxConcurrentEvents()) {
            plugin.getLogger().info("Cannot force " + eventName + ": max concurrent events reached");
            return false;
        }

        // Skip condition checks - this is a forced trigger
        plugin.getLogger().info("Force triggering " + eventName + " (bypassing conditions)");

        // Create a new instance of the event (clone the prototype)
        WeatherEvent newEvent = createEventInstance(prototype, location);
        if (newEvent == null) {
            return false;
        }

        // Start the event
        newEvent.start(location.getWorld(), location);
        activeEvents.add(newEvent);
        
        // Update last trigger time
        lastTriggerTimes.put(eventName.toLowerCase(), System.currentTimeMillis());
        
        plugin.getLogger().info("Force triggered weather event: " + eventName + " at " + 
                               location.getBlockX() + ", " + location.getBlockZ());
        
        return true;
    }

    /**
     * Trigger a weather event at a player's location
     * @param eventName The name of the event to trigger
     * @param playerName The name of the player at whose location to trigger
     * @return true if the event was triggered successfully
     */
    public boolean triggerEventAtPlayer(String eventName, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null || !player.isOnline()) {
            return false;
        }
        
        return triggerEvent(eventName, player.getLocation());
    }

    /**
     * Force trigger a weather event at a player's location (bypasses condition checks)
     * @param eventName The name of the event to trigger
     * @param playerName The name of the player at whose location to trigger
     * @return true if the event was triggered successfully
     */
    public boolean forceEventAtPlayer(String eventName, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null || !player.isOnline()) {
            return false;
        }
        
        return forceEvent(eventName, player.getLocation());
    }

    /**
     * Cancel a specific active weather event
     * @param eventId The UUID of the event to cancel
     * @return true if the event was found and cancelled
     */
    public boolean cancelEvent(UUID eventId) {
        for (WeatherEvent event : activeEvents) {
            if (event.getEventId().equals(eventId)) {
                event.end();
                activeEvents.remove(event);
                plugin.getLogger().info("Cancelled weather event: " + event.getName());
                return true;
            }
        }
        return false;
    }

    /**
     * Cancel all active weather events
     */
    public void cancelAllEvents() {
        for (WeatherEvent event : new HashSet<>(activeEvents)) {
            event.end();
        }
        activeEvents.clear();
        plugin.getLogger().info("Cancelled all active weather events");
    }

    /**
     * Check for natural triggers across all worlds
     */
    public void checkForNaturalTriggers() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) {
                continue; // Only check normal worlds
            }
            
            // Skip if no players are in this world
            if (world.getPlayers().isEmpty()) {
                continue;
            }
            
            checkWorldForTriggers(world);
        }
    }

    /**
     * Check a specific world for weather event triggers
     * @param world The world to check
     */
    private void checkWorldForTriggers(World world) {
        // Check each registered event
        for (WeatherEvent prototype : registeredEvents.values()) {
            String eventName = prototype.getName().toLowerCase();
            
            // Check cooldown
            if (isEventOnCooldown(eventName)) {
                continue;
            }
            
            // Check if event can naturally trigger
            if (shouldTriggerEvent(prototype, world)) {
                // Find a suitable location for the event
                Location triggerLocation = findTriggerLocation(prototype, world);
                if (triggerLocation != null) {
                    triggerEvent(prototype.getName(), triggerLocation);
                    break; // Only trigger one event per check cycle
                }
            }
        }
    }

    /**
     * Check if an event is on cooldown
     * @param eventName The name of the event
     * @return true if the event is on cooldown
     */
    private boolean isEventOnCooldown(String eventName) {
        Long lastTrigger = lastTriggerTimes.get(eventName);
        if (lastTrigger == null) {
            return false;
        }
        
        // Different cooldowns for different events
        long cooldownMs;
        if (eventName.equals("obsidian storm")) {
            // Obsidian Storm has a much longer cooldown (hours)
            cooldownMs = plugin.getPluginConfig().getObsidianStormCooldownHours() * 60 * 60 * 1000;
        } else {
            // Default 30 minute cooldown for other events
            cooldownMs = 30 * 60 * 1000;
        }
        
        return (System.currentTimeMillis() - lastTrigger) < cooldownMs;
    }

    /**
     * Determine if an event should trigger naturally
     * @param event The weather event prototype
     * @param world The world to check
     * @return true if the event should trigger
     */
    private boolean shouldTriggerEvent(WeatherEvent event, World world) {
        // Basic weather condition check
        if (!world.hasStorm()) {
            return false; // Most events require existing rain
        }
        
        // Random chance check
        double triggerChance = event.getTriggerChance();
        return ThreadLocalRandom.current().nextDouble() < triggerChance;
    }

    /**
     * Find a suitable location to trigger an event
     * @param event The weather event prototype
     * @param world The world to search in
     * @return A suitable location or null if none found
     */
    private Location findTriggerLocation(WeatherEvent event, World world) {
        // Try to find a location near active players
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) {
            return null;
        }
        
        // Pick a random player and search around them
        Player randomPlayer = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        Location playerLoc = randomPlayer.getLocation();
        
        // Search in a radius around the player
        int searchRadius = 64;
        for (int attempts = 0; attempts < 10; attempts++) {
            int offsetX = ThreadLocalRandom.current().nextInt(-searchRadius, searchRadius + 1);
            int offsetZ = ThreadLocalRandom.current().nextInt(-searchRadius, searchRadius + 1);
            
            Location testLoc = playerLoc.clone().add(offsetX, 0, offsetZ);
            testLoc.setY(world.getHighestBlockYAt(testLoc));
            
            if (event.canTriggerAt(testLoc)) {
                return testLoc;
            }
        }
        
        return null;
    }

    /**
     * Create a new instance of a weather event
     * @param prototype The prototype event
     * @param location The location where it will be triggered
     * @return A new event instance or null if creation failed
     */
    private WeatherEvent createEventInstance(WeatherEvent prototype, Location location) {
        try {
            // For now, we'll use the prototype directly
            // In a more complex system, you might want to clone the event
            return prototype;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create event instance: " + e.getMessage());
            return null;
        }
    }

    /**
     * Start the event ticker that manages active events
     */
    private void startEventTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Tick all active events
                Set<WeatherEvent> eventsToRemove = new HashSet<>();
                
                for (WeatherEvent event : activeEvents) {
                    if (event.isActive()) {
                        event.tick();
                    } else {
                        eventsToRemove.add(event);
                    }
                }
                
                // Remove inactive events
                activeEvents.removeAll(eventsToRemove);
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }

    // Getters for information
    
    /**
     * Get all registered event names
     * @return Set of registered event names
     */
    public Set<String> getRegisteredEventNames() {
        return new HashSet<>(registeredEvents.keySet());
    }

    /**
     * Get all active events
     * @return Set of active weather events
     */
    public Set<WeatherEvent> getActiveEvents() {
        return new HashSet<>(activeEvents);
    }

    /**
     * Get a registered event by name
     * @param eventName The name of the event
     * @return The weather event or null if not found
     */
    public WeatherEvent getRegisteredEvent(String eventName) {
        return registeredEvents.get(eventName.toLowerCase());
    }

    /**
     * Check if an event is currently active
     * @param eventName The name of the event to check
     * @return true if the event is active
     */
    public boolean isEventActive(String eventName) {
        return activeEvents.stream()
                .anyMatch(event -> event.getName().equalsIgnoreCase(eventName) && event.isActive());
    }

    /**
     * Get status information about the weather manager
     * @return Map containing status information
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("registeredEvents", registeredEvents.size());
        status.put("activeEvents", activeEvents.size());
        status.put("maxConcurrentEvents", plugin.getPluginConfig().getMaxConcurrentEvents());
        
        List<Map<String, Object>> activeEventInfo = new ArrayList<>();
        for (WeatherEvent event : activeEvents) {
            Map<String, Object> eventInfo = new HashMap<>();
            eventInfo.put("name", event.getName());
            eventInfo.put("duration", event.getRemainingDuration());
            eventInfo.put("world", event.getWorld().getName());
            eventInfo.put("location", event.getCenterLocation());
            activeEventInfo.add(eventInfo);
        }
        status.put("activeEventDetails", activeEventInfo);
        
        return status;
    }

    /**
     * Force a weather event to start at a player's location, bypassing all condition checks
     * @param event The weather event to force
     * @param player The player to center the event on
     * @return true if the event was started successfully
     */
    public boolean forceEventAtPlayer(WeatherEvent event, Player player) {
        if (activeEvents.size() >= plugin.getPluginConfig().getMaxConcurrentEvents()) {
            return false;
        }

        activeEvents.add(event);
        event.start(player.getWorld(), player.getLocation());
        lastTriggerTimes.put(event.getName(), System.currentTimeMillis());
        
        plugin.getLogger().info("Forced weather event: " + event.getName() + " at player " + player.getName());
        return true;
    }

    /**
     * Force a weather event to start at a specific location, bypassing all condition checks
     * @param event The weather event to force
     * @param location The location to center the event on
     * @return true if the event was started successfully
     */
    public boolean forceEvent(WeatherEvent event, Location location) {
        if (activeEvents.size() >= plugin.getPluginConfig().getMaxConcurrentEvents()) {
            return false;
        }

        activeEvents.add(event);
        event.start(location.getWorld(), location);
        lastTriggerTimes.put(event.getName(), System.currentTimeMillis());
        
        plugin.getLogger().info("Forced weather event: " + event.getName() + " at location " + 
                               location.getBlockX() + ", " + location.getBlockZ());
        return true;
    }
}
