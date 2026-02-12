# PowerHUD Development Instructions
## Project Overview
PowerHUD is a Minecraft Fabric mod (v1.10.1) for Minecraft 1.21.4 that provides a customizable HUD overlay system.
## Project Structure
- **Source Code**: src/main/java/net/steve/powerhud/
- **Resources**: src/main/resources/
- **Build Output**: build/libs/
- **Test Environment**: run/
## Version Management
- **Current Version**: 1.10.1
- **Version Files**: 
  - gradle.properties - Set mod_version=X.X.X (primary version source)
  - version.txt - Documentation reference
  - About screen automatically pulls version from gradle.properties via Fabric metadata
- **Versioning Rule**: Bump version on ANY feature change, bug fix, or code modification before commit
## Build & Run Commands
# Build the mod
cd C:\Users\Steve\Documents\PH
.\gradlew.bat build
# Run Minecraft with mod for testing
.\gradlew.bat runClient
# Clean build
.\gradlew.bat clean build
## Key Configuration Files
- PowerHudConfig.java - All mod settings and persistence
- PowerHudConfigScreen.java - Configuration GUI
- HudRenderer.java - Main rendering logic
- HudData.java - Data collection for HUD elements
- HudOrderScreen.java - Drag-and-drop element ordering
- HudConstants.java - UI constants and color definitions
## Current Feature Set (v1.10.1)
### HUD Elements
- **FPS Display** - Three modes: Minimal (FPS:xxx), Normal (FPS/AVG/MIN/MAX), Full (Normal + ms timing)
- **Coordinates** (XYZ)
- **Facing Direction** (Cardinal directions)
- **Biome Information**
- **Time Display** (In-game time)
- **Vitality** (Health/status)
- **Block Information** (Looking at)
- **Best Tool** (Recommendations)
- **Inventory Display** (Visual grid)
- **Oxygen Overlay** - Centered screen display with color-coded bar (NOT in HUD workbench)
- **Block Statistics** (Mined/placed tracking)
- **Gamemode Display**
### Oxygen System (Special Implementation)
- **Display**: Centered overlay 3/4 up screen with colored bar and text
- **Colors**: GREEN (>60%), YELLOW (40-60%), ORANGE (20-40%), RED (<20%) - with light opacity
- **Bar Length**: Fixed width regardless of oxygen level
- **Text**: White text centered over bar showing "Oxygen XX% - [Status]"
- **Config**: Toggle in Elements tab, adjust height with stepper
- **Important**: NOT moveable in HUD Order screen (it is a centered overlay, not a HUD element)
- **Vanilla Air**: Separate toggle to hide default Minecraft air bubbles
### Customization Options
- **9 Fonts**: JetBrains Mono, Fira Code, Roboto Mono, Cascadia Code, Source Code Pro, Comic Mono, Monofur, Ubuntu Mono, Inter Mono
- **20+ Colors**: Extensive palette for text and data (see PowerHudConfig.COLORS and COLOR_NAMES arrays)
- **Bold Titles**: Toggle for HUD element titles
- **HUD Scale**: Adjustable sizing
- **Element Ordering**: Drag-and-drop in HUD Order screen
- **FPS Thresholds**: Configurable color-coded performance indicators
- **Inventory Modes**: Percent, Value, or Boxes display
### Removed Features (Clean Design Philosophy)
- Text effects (shimmer, chroma) - removed for performance
- Background styles - removed for simplicity
- Shadow effects - removed
- Soft/rounded corners - removed
- Custom spacing controls - removed
## Code Style Guidelines
### Rendering
- Use DrawContext for all rendering
- Colors are int ARGB format (0xAARRGGBB)
- Text rendering: dc.drawTextWithShadow() or dc.drawText()
- Use HudConstants for all UI dimensions and spacing
### Configuration
- All settings stored in PowerHudConfig static fields
- Save via PowerHudConfig.save() - uses Gson to config/powerhud.json
- Auto-load on mod initialization
### HUD Element Pattern
1. Check if element enabled in config
2. Collect data via HudData methods
3. Format text strings
4. Calculate position based on alignment and offset
5. Render using DrawContext
## Git Workflow
# Check status
git status
# Stage all changes
git add .
# Commit with descriptive message
git commit -m "Description of changes"
### Commit Message Guidelines
- Be descriptive and specific
- Examples: "Refactor oxygen HUD to centered text overlay", "Bump version to 1.10.1", "Remove text effects and shadow features"
## Common Development Tasks
### Adding a New Color
1. Add int value to PowerHudConfig.COLORS array
2. Add name to PowerHudConfig.COLOR_NAMES array (same index)
3. Ensure arrays stay synchronized
### Adding a New HUD Element
1. Add boolean toggle to PowerHudConfig
2. Add data collection method to HudData (if needed)
3. Add rendering logic to HudRenderer.renderHud()
4. Add toggle button in PowerHudConfigScreen ELEMENTS case
5. Add element to HudOrderScreen if moveable
6. Test thoroughly
### Modifying FPS Display
- Three modes in PowerHudConfig.FpsMode enum: MINIMAL, NORMAL, FULL
- Format strings in HudRenderer.renderHud() FPS section
- Color thresholds: redThresh, orangeThresh, yellowThresh in config
## Testing Checklist
- Build completes without errors: .\gradlew.bat build
- Mod loads in Minecraft: .\gradlew.bat runClient
- Configuration screen opens (default: O key)
- All toggles work correctly
- HUD elements display as expected
- Changes persist after restart
- Version displays correctly in About screen
## Important Notes
- Always bump version before committing changes
- Test builds before committing
- Oxygen overlay is NOT a standard HUD element (special centered overlay)
- Keep color arrays synchronized (COLORS and COLOR_NAMES)
- Use HudConstants for all UI measurements
- Save config after any setting change
## Current Development Focus
- Clean, performant rendering
- User-friendly configuration
- Reliable persistence
- Minimal/modern aesthetic
- Stable 1.21.4 Fabric compatibility

