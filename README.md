# PowerHUD

PowerHUD is a Minecraft Fabric mod for Minecraft 1.21.4 that provides a customizable HUD overlay system.

## Current Version
**1.11.4**

## Features
- Customizable HUD elements: FPS, Coordinates, Facing Direction, Biome, Time, Vitality, Block Info, Best Tool, Inventory, Oxygen Overlay, Block Statistics, Gamemode
- Oxygen overlay: Centered, fixed-width bar with color-coded status and text
- 9 fonts, 20+ colors, bold titles, HUD scale, element ordering (drag-and-drop)
- FPS display: Minimal, Normal, Full modes with color thresholds
- Inventory modes: Percent, Value, Boxes
- **Profiles:** Save/load/delete HUD layouts for quick switching between custom setups. Profiles allow you to instantly switch between different HUD configurations and layouts, making customization easier and more flexible.
- Clean, performant rendering (DrawContext, ARGB colors)
- All settings persist via config/powerhud.json

## Usage
- Open config screen (default: O key)
- Drag and drop HUD elements in the HUD Order screen
- Save/load HUD profiles in the Profiles tab
- All changes persist after restart

## Version Management
- Version is set in gradle.properties and displayed in About screen
- Always bump version before committing changes

## Build & Run
- Build: `./gradlew.bat build`
- Run: `./gradlew.bat runClient`

## Support
Contact: watkins.steve@gmail.com

## Changelog
- 1.11.4: **Profiles feature improved and documented. Version bump.**
- 1.11.3: Added HUD profile management (save/load/delete), improved oxygen overlay, UI fixes
- 1.10.1: Minimalist redesign, performance improvements, removed text effects/shadows

## License
MIT License
