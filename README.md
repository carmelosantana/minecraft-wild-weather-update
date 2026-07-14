# 🌩️ Wild Weather Update

A dynamic weather system plugin for Minecraft that adds realistic weather patterns and devastating natural events to enhance gameplay.

## Features

### Weather Events

#### 🌧️ Monsoon Event
- Heavy rainfall with realistic flooding mechanics
- Water accumulation in low-lying areas
- Natural drainage over time
- Crop enhancement and damage effects
- Visual and sound effects

#### 🛢️ Obsidian Storm Event ⚠️ **EXTREMELY RARE & DESTRUCTIVE**
- Catastrophic celestial bombardment
- Massive obsidian blocks fall from the sky
- Devastating explosions on impact (8-16x TNT power)
- Creates obsidian-lined craters
- Screen shake and dramatic effects
- Only occurs in specific biomes
- 1-week default cooldown between events

### Core Features
- **Natural Triggers**: Events occur based on realistic weather conditions
- **Configurable Settings**: Customize frequency, intensity, and effects
- **API Integration**: Other plugins can interact with the weather system
- **Cross-Platform**: Works with Bedrock via Geyser/Floodgate
- **Performance Optimized**: Async processing and efficient block handling

## Installation

1. Download the latest JAR from releases
2. Place in your server's `plugins/` directory
3. Start/restart your server
4. Configure in `plugins/WildWeatherUpdate/config.yml`

## Commands

### Basic Commands
- `/weather help` - Show available commands
- `/weather status` - View active weather events
- `/weather trigger <event> [player]` - Manually trigger events (Admin)
- `/weather debug <event>` - Show detailed event information (Admin)
- `/weather cancel [event|all]` - Cancel active events (Admin)
- `/weather reload` - Reload configuration (Admin)

### Event-Specific Commands
- `/weather trigger monsoon` - Start a monsoon event
- `/weather trigger obsidian_storm` - ⚠️ Start obsidian storm (DESTRUCTIVE!)
- `/weather debug monsoon` - Show monsoon configuration
- `/weather debug obsidian_storm` - Show obsidian storm settings

## Configuration

The plugin uses `config.yml` for all settings. Key sections:

### Monsoon Configuration
```yaml
monsoon:
  enabled: true
  chance: 0.03  # 3% chance per check
  duration_ticks: 48000  # 40 minutes
  enabled_biomes:
    - plains
    - jungle
    - swamp
  effects:
    flooding: true
    crop_enhancement: true
```

### Obsidian Storm Configuration
```yaml
obsidian_storm:
  enabled: true
  chance: 0.001  # 0.1% chance - EXTREMELY RARE
  duration_ticks: 3600  # 3 minutes
  cooldown_hours: 168  # 1 week cooldown
  explosion:
    base_power: 8.0
    max_power: 16.0
    destroy_all_blocks: true
```

## Permissions

- `wildweather.*` - All permissions
- `wildweather.command.weather` - Use weather commands (default: op)
- `wildweather.admin.trigger` - Trigger events manually (default: op)
- `wildweather.admin.debug` - Access debug information (default: op)

## Development

### Building
```bash
git clone <repository>
cd wild-weather-update
make setup    # Initial setup
make build    # Build plugin
make dev      # Build + install + restart server
```

### Testing
```bash
make test-monsoon    # Get monsoon test commands
make test-obsidian   # Get obsidian storm test commands (⚠️ DESTRUCTIVE)
make give-debug-items # Get useful debug items
```

### Docker Development
```bash
make docker-build   # Build in container
make docker-test    # Test in isolated environment
```

## API Usage

Other plugins can interact with the weather system:

```java
// Get the weather API
WeatherAPI weatherAPI = ((WildWeatherPlugin) getServer().getPluginManager().getPlugin("WildWeatherUpdate")).getWeatherAPI();

// Trigger an event
weatherAPI.triggerWeatherEvent("monsoon", location);

// Check if event is active
boolean isActive = weatherAPI.isEventActive("obsidian_storm");

// Get active events
Set<WeatherEvent> activeEvents = weatherAPI.getActiveEvents();
```

## Safety Warnings

### ⚠️ Obsidian Storm Safety
The Obsidian Storm is **EXTREMELY DESTRUCTIVE** and should be used with caution:

- **Backup your world** before testing
- Use **creative mode** or test in disposable worlds
- Explosions **destroy ALL blocks** in blast radius
- Creates permanent landscape changes
- Can damage player-built structures severely

### Testing Recommendations
1. Always test in creative mode first
2. Use `/tp @p ~ ~50 ~` for safe viewing
3. Have backups ready
4. Consider the 1-week cooldown for natural triggering

## Requirements

- **Minecraft**: Java Edition 1.21+
- **Server**: Paper 26.1.2+ (recommended)
- **Java**: 25+
- **Optional**: ViaVersion for cross-version compatibility

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines.

## Support

- Check the [Issues](../../issues) for known problems
- Review configuration examples in `config.yml`
- Use `/weather debug <event>` for troubleshooting

## License

This project is licensed under the [GNU Affero General Public License v3.0 or later](LICENSE).

---

**⚠️ Important**: Always backup your world before using this plugin, especially before triggering Obsidian Storm events!
