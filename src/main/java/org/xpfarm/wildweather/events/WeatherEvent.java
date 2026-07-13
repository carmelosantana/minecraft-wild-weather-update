package org.xpfarm.wildweather.events;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.xpfarm.wildweather.WildWeatherPlugin;

import java.util.List;
import java.util.UUID;

/**
 * Abstract base class for all weather events
 */
public abstract class WeatherEvent {
    
    protected final WildWeatherPlugin plugin;
    protected final String name;
    protected final UUID eventId;
    protected boolean active;
    protected long startTime;
    protected long duration;
    protected World world;
    protected Location centerLocation;

    protected WeatherEvent(WildWeatherPlugin plugin, String name, long duration) {
        this.plugin = plugin;
        this.name = name;
        this.duration = duration;
        this.eventId = UUID.randomUUID();
        this.active = false;
    }

    /**
     * Start the weather event
     * @param world The world where the event occurs
     * @param centerLocation The center location of the event
     */
    public final void start(World world, Location centerLocation) {
        this.world = world;
        this.centerLocation = centerLocation.clone();
        this.active = true;
        this.startTime = System.currentTimeMillis();
        
        onStart();
        
        plugin.getLogger().info("Weather event '" + name + "' started in world " + world.getName() + 
                               " at " + centerLocation.getBlockX() + ", " + centerLocation.getBlockZ());
    }

    /**
     * Called when the event starts
     */
    protected abstract void onStart();

    /**
     * Tick the weather event (called periodically)
     */
    public final void tick() {
        if (!active) return;
        
        // Check if event should end
        if (hasExpired()) {
            end();
            return;
        }
        
        onTick();
    }

    /**
     * Called on each tick while the event is active
     */
    protected abstract void onTick();

    /**
     * End the weather event
     */
    public final void end() {
        if (!active) return;
        
        this.active = false;
        onEnd();
        
        plugin.getLogger().info("Weather event '" + name + "' ended in world " + world.getName());
    }

    /**
     * Called when the event ends
     */
    protected abstract void onEnd();

    /**
     * Check if the event can trigger in the given location
     * @param location The location to check
     * @return true if the event can trigger here
     */
    public abstract boolean canTriggerAt(Location location);

    /**
     * Get the allowed biomes for this event
     * @return List of allowed biomes
     */
    public abstract List<Biome> getAllowedBiomes();

    /**
     * Get the trigger chance for this event (0.0 to 1.0)
     * @return The trigger chance
     */
    public abstract double getTriggerChance();

    /**
     * Check if the event has expired based on duration
     * @return true if the event should end
     */
    protected boolean hasExpired() {
        return (System.currentTimeMillis() - startTime) >= (duration * 50); // Convert ticks to milliseconds
    }

    /**
     * Get the remaining duration in ticks
     * @return Remaining duration in ticks
     */
    public long getRemainingDuration() {
        if (!active) return 0;
        
        long elapsed = (System.currentTimeMillis() - startTime) / 50; // Convert to ticks
        return Math.max(0, duration - elapsed);
    }

    // Getters
    public String getName() {
        return name;
    }

    public UUID getEventId() {
        return eventId;
    }

    public boolean isActive() {
        return active;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDuration() {
        return duration;
    }

    public World getWorld() {
        return world;
    }

    public Location getCenterLocation() {
        return centerLocation != null ? centerLocation.clone() : null;
    }

    /**
     * Set the duration of this event
     * @param duration Duration in ticks
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }
}
