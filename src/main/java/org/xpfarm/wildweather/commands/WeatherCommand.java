package org.xpfarm.wildweather.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.xpfarm.wildweather.WildWeatherPlugin;
import org.xpfarm.wildweather.events.MonsoonEvent;
import org.xpfarm.wildweather.events.ObsidianStormEvent;
import org.xpfarm.wildweather.events.WeatherEvent;
import org.xpfarm.wildweather.manager.WeatherManager;

import java.util.*;

/**
 * Main command handler for weather-related commands
 */
public class WeatherCommand implements CommandExecutor, TabCompleter {
    
    private final WildWeatherPlugin plugin;
    private final WeatherManager weatherManager;

    public WeatherCommand(WildWeatherPlugin plugin) {
        this.plugin = plugin;
        this.weatherManager = plugin.getWeatherManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wildweather.command.weather")) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                sendHelpMessage(sender);
                break;
            case "trigger":
                handleTriggerCommand(sender, args);
                break;
            case "force":
                handleForceCommand(sender, args);
                break;
            case "status":
                handleStatusCommand(sender);
                break;
            case "cancel":
                handleCancelCommand(sender, args);
                break;
            case "debug":
                handleDebugCommand(sender, args);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            default:
                sender.sendMessage(Component.text("Unknown subcommand: " + subCommand, NamedTextColor.RED));
                sendHelpMessage(sender);
                break;
        }
        
        return true;
    }

    /**
     * Send help message showing available commands
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(Component.text("=== Wild Weather Update Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/weather help", NamedTextColor.YELLOW).append(Component.text(" - Show this help message", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/weather status", NamedTextColor.YELLOW).append(Component.text(" - Show current weather status", NamedTextColor.WHITE)));
        if (sender.hasPermission("wildweather.admin.trigger")) {
            sender.sendMessage(Component.text("/weather trigger <event> [player]", NamedTextColor.YELLOW).append(Component.text(" - Attempt to trigger an event", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("/weather force <event> [player]", NamedTextColor.YELLOW).append(Component.text(" - Force an event (bypasses conditions)", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("/weather cancel [event]", NamedTextColor.YELLOW).append(Component.text(" - Cancel active weather events", NamedTextColor.WHITE)));
        }
        if (sender.hasPermission("wildweather.admin.debug")) {
            sender.sendMessage(Component.text("/weather debug <event> [player]", NamedTextColor.YELLOW).append(Component.text(" - Debug event conditions", NamedTextColor.WHITE)));
        }
        if (sender.hasPermission("wildweather.admin")) {
            sender.sendMessage(Component.text("/weather reload", NamedTextColor.YELLOW).append(Component.text(" - Reload configuration", NamedTextColor.WHITE)));
        }
        sender.sendMessage(Component.text("Available events: monsoon, obsidian", NamedTextColor.GRAY));
    }

    /**
     * Handle the trigger subcommand
     */
    private void handleTriggerCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wildweather.admin.trigger")) {
            sender.sendMessage(Component.text("You don't have permission to trigger weather events!", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /weather trigger <event> [player]", NamedTextColor.RED));
            return;
        }

        String eventName = args[1];
        String targetPlayer = args.length > 2 ? args[2] : (sender instanceof Player ? sender.getName() : null);

        if (targetPlayer == null) {
            sender.sendMessage(Component.text("You must specify a player when running from console!", NamedTextColor.RED));
            return;
        }

        Player player = Bukkit.getPlayer(targetPlayer);
        if (player == null || !player.isOnline()) {
            sender.sendMessage(Component.text("Player '" + targetPlayer + "' not found or not online!", NamedTextColor.RED));
            return;
        }

        boolean success = plugin.getWeatherManager().triggerEvent(eventName, player.getLocation());
        if (success) {
            sender.sendMessage(Component.text("Successfully triggered ", NamedTextColor.GREEN)
                .append(Component.text(eventName, NamedTextColor.YELLOW))
                .append(Component.text(" at " + targetPlayer + "'s location", NamedTextColor.GREEN)));
        } else {
            sender.sendMessage(Component.text("Failed to trigger " + eventName + ". Check conditions and try again.", NamedTextColor.RED));
        }
    }

    /**
     * Handle the status subcommand
     */
    private void handleStatusCommand(CommandSender sender) {
        Map<String, Object> status = plugin.getWeatherManager().getStatus();
        
        sender.sendMessage(Component.text("=== Weather Status ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Registered Events: ", NamedTextColor.AQUA)
            .append(Component.text(status.get("registeredEvents").toString(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Active Events: ", NamedTextColor.AQUA)
            .append(Component.text(status.get("activeEvents").toString(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Max Concurrent: ", NamedTextColor.AQUA)
            .append(Component.text(status.get("maxConcurrentEvents").toString(), NamedTextColor.WHITE)));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> activeEvents = (List<Map<String, Object>>) status.get("activeEventDetails");
        
        if (!activeEvents.isEmpty()) {
            sender.sendMessage(Component.text("Active Event Details:", NamedTextColor.YELLOW));
            for (Map<String, Object> eventInfo : activeEvents) {
                sender.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
                    .append(Component.text(eventInfo.get("name").toString(), NamedTextColor.WHITE))
                    .append(Component.text(" in ", NamedTextColor.GRAY))
                    .append(Component.text(eventInfo.get("world").toString(), NamedTextColor.AQUA))
                    .append(Component.text(" (", NamedTextColor.GRAY))
                    .append(Component.text(eventInfo.get("duration").toString() + " ticks remaining", NamedTextColor.YELLOW))
                    .append(Component.text(")", NamedTextColor.GRAY)));
            }
        }
    }

    /**
     * Handle the cancel subcommand
     */
    private void handleForceCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wildweather.admin.force")) {
            sender.sendMessage(Component.text("You don't have permission to force weather events.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /weather force <event>", NamedTextColor.RED));
            sender.sendMessage(Component.text("Available events: monsoon, obsidian", NamedTextColor.YELLOW));
            return;
        }

        String eventName = args[1].toLowerCase();
        Player target = null;

        // Check if a player is specified
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("Player '" + args[2] + "' not found.", NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(Component.text("You must specify a player when running from console.", NamedTextColor.RED));
            return;
        }

        WeatherEvent event = null;
        WeatherManager weatherManager = plugin.getWeatherManager();
        if (eventName.equals("monsoon")) {
            event = new MonsoonEvent(plugin);
        } else if (eventName.equals("obsidian") || eventName.equals("obsidian_storm") || eventName.equals("obsidianstorm")) {
            event = new ObsidianStormEvent(plugin);
        } else {
            sender.sendMessage(Component.text("Unknown event: " + eventName, NamedTextColor.RED));
            sender.sendMessage(Component.text("Available events: monsoon, obsidian", NamedTextColor.YELLOW));
            return;
        }

        boolean success = weatherManager.forceEventAtPlayer(event, target);
        if (success) {
            sender.sendMessage(Component.text("Forced " + event.getName() + " event on " + target.getName() + " (bypassing all conditions).", NamedTextColor.GREEN));
            if (!sender.equals(target)) {
                target.sendMessage(Component.text("A " + event.getName() + " event has been forced upon you!", NamedTextColor.YELLOW));
            }
        } else {
            sender.sendMessage(Component.text("Failed to force event. This usually means another event is already active.", NamedTextColor.RED));
        }
    }

    private void handleCancelCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wildweather.admin.trigger")) {
            sender.sendMessage(Component.text("You don't have permission to cancel weather events!", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            plugin.getWeatherManager().cancelAllEvents();
            sender.sendMessage(Component.text("Cancelled all active weather events.", NamedTextColor.GREEN));
            return;
        }

        String target = args[1].toLowerCase();
        
        if (target.equals("all")) {
            plugin.getWeatherManager().cancelAllEvents();
            sender.sendMessage(Component.text("Cancelled all active weather events.", NamedTextColor.GREEN));
        } else {
            // Try to cancel specific event type
            Set<WeatherEvent> activeEvents = plugin.getWeatherManager().getActiveEvents();
            boolean found = false;
            
            for (WeatherEvent event : activeEvents) {
                if (event.getName().equalsIgnoreCase(target)) {
                    plugin.getWeatherManager().cancelEvent(event.getEventId());
                    sender.sendMessage(Component.text("Cancelled " + event.getName() + " event.", NamedTextColor.GREEN));
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                sender.sendMessage(Component.text("No active event named '" + target + "' found.", NamedTextColor.RED));
            }
        }
    }

    /**
     * Handle the debug subcommand
     */
    private void handleDebugCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wildweather.admin.debug")) {
            sender.sendMessage(Component.text("You don't have permission to view debug information!", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== Debug Information ===", NamedTextColor.GOLD));
        
        if (args.length > 1) {
            String eventName = args[1].toLowerCase();
            if (eventName.equals("monsoon")) {
                debugMonsoonEvent(sender);
            } else if (eventName.equals("obsidian") || eventName.equals("obsidian_storm") || eventName.equals("obsidianstorm")) {
                debugObsidianStormEvent(sender);
            } else {
                sender.sendMessage(Component.text("Unknown event for debug: " + eventName, NamedTextColor.RED));
                sender.sendMessage(Component.text("Available: monsoon, obsidian_storm", NamedTextColor.GRAY));
            }
        } else {
            // General debug info
            sender.sendMessage(Component.text("Plugin Version: ", NamedTextColor.AQUA)
                .append(Component.text(plugin.getDescription().getVersion(), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Natural Triggers: ", NamedTextColor.AQUA)
                .append(Component.text(plugin.getPluginConfig().areNaturalTriggersEnabled() ? "Enabled" : "Disabled", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Check Interval: ", NamedTextColor.AQUA)
                .append(Component.text(plugin.getPluginConfig().getCheckInterval() + " ticks", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Debug Mode: ", NamedTextColor.AQUA)
                .append(Component.text(plugin.getPluginConfig().isDebugEnabled() ? "Enabled" : "Disabled", NamedTextColor.WHITE)));
        }
    }

    /**
     * Show debug information for monsoon events
     */
    private void debugMonsoonEvent(CommandSender sender) {
        sender.sendMessage(Component.text("=== Monsoon Debug ===", NamedTextColor.GOLD));
        
        WeatherEvent monsoonPrototype = plugin.getWeatherManager().getRegisteredEvent("monsoon");
        if (monsoonPrototype instanceof MonsoonEvent) {
            MonsoonEvent monsoon = (MonsoonEvent) monsoonPrototype;
            
            sender.sendMessage(Component.text("Enabled: ", NamedTextColor.AQUA)
                .append(Component.text(plugin.getPluginConfig().isMonsoonEnabled() ? "Yes" : "No", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Trigger Chance: ", NamedTextColor.AQUA)
                .append(Component.text(String.format("%.2f%%", plugin.getPluginConfig().getMonsoonChance() * 100), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Duration: ", NamedTextColor.AQUA)
                .append(Component.text(plugin.getPluginConfig().getMonsoonDuration() + " ticks", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Drain Rate: ", NamedTextColor.AQUA)
                .append(Component.text(plugin.getPluginConfig().getDrainRateBlocksPerMinute() + " blocks/min", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Min Altitude: ", NamedTextColor.AQUA)
                .append(Component.text(plugin.getPluginConfig().getMonsoonMinBiomeAltitude() + "", NamedTextColor.WHITE)));
            
            if (monsoon.isActive()) {
                Map<String, Object> floodInfo = monsoon.getFloodingInfo();
                sender.sendMessage(Component.text("Flooded Blocks: ", NamedTextColor.AQUA)
                    .append(Component.text(floodInfo.get("floodedBlocks").toString(), NamedTextColor.WHITE)));
            }
        }
    }

    /**
     * Show debug information for obsidian storm events
     */
    private void debugObsidianStormEvent(CommandSender sender) {
        sender.sendMessage(Component.text("=== Obsidian Storm Debug ===", NamedTextColor.GOLD));
        
        WeatherEvent stormPrototype = plugin.getWeatherManager().getRegisteredEvent("obsidian storm");
        if (stormPrototype instanceof ObsidianStormEvent) {
            sender.sendMessage(Component.text("Enabled: ", NamedTextColor.AQUA)
                .append(Component.text(plugin.getPluginConfig().isObsidianStormEnabled() ? "Yes" : "No", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Trigger Chance: ", NamedTextColor.AQUA)
                .append(Component.text(String.format("%.3f%%", plugin.getPluginConfig().getObsidianStormChance() * 100), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Duration: ", NamedTextColor.AQUA)
                .append(Component.text(plugin.getPluginConfig().getObsidianStormDuration() + " ticks", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Cooldown: ", NamedTextColor.AQUA)
                .append(Component.text(plugin.getPluginConfig().getObsidianStormCooldownHours() + " hours", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Explosion Power: ", NamedTextColor.AQUA)
                .append(Component.text(plugin.getPluginConfig().getObsidianStormBasePower() + "-" + 
                                     plugin.getPluginConfig().getObsidianStormMaxPower(), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Block Frequency: ", NamedTextColor.AQUA)
                .append(Component.text(plugin.getPluginConfig().getObsidianBlockBaseFrequency() + "-" + 
                                     plugin.getPluginConfig().getObsidianBlockMaxFrequency() + " blocks/min", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Fall Speed: ", NamedTextColor.AQUA)
                .append(Component.text(plugin.getPluginConfig().getObsidianBlockFallSpeed() + " blocks/sec", NamedTextColor.WHITE)));
            
            // Show enabled biomes
            List<String> biomes = plugin.getPluginConfig().getObsidianStormEnabledBiomes();
            sender.sendMessage(Component.text("Enabled Biomes: ", NamedTextColor.AQUA)
                .append(Component.text(String.join(", ", biomes), NamedTextColor.WHITE)));
        } else {
            sender.sendMessage(Component.text("Obsidian Storm event not found or not enabled!", NamedTextColor.RED));
        }
    }

    /**
     * Handle the reload subcommand
     */
    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("wildweather.admin.trigger")) {
            sender.sendMessage(Component.text("You don't have permission to reload the configuration!", NamedTextColor.RED));
            return;
        }

        plugin.reloadPluginConfig();
        sender.sendMessage(Component.text("Wild Weather Update configuration reloaded!", NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            List<String> subCommands = Arrays.asList("help", "trigger", "force", "status", "cancel", "debug", "reload");
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("trigger") || subCommand.equals("force") || subCommand.equals("cancel") || subCommand.equals("debug")) {
                // Second argument - event names
                for (String eventName : plugin.getWeatherManager().getRegisteredEventNames()) {
                    if (eventName.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(eventName);
                    }
                }
                
                if (subCommand.equals("cancel") && "all".startsWith(args[1].toLowerCase())) {
                    completions.add("all");
                }
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("trigger") || args[0].equalsIgnoreCase("force"))) {
            // Third argument for trigger/force - player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        }
        
        return completions;
    }
}
