# PowerHUD Roadmap & Future Ideas

## 1. Customizable HUD Profiles
- Save/load different HUD configurations for different scenarios (mining, building, exploration, combat)
- Quick-switch between profiles with hotkeys
- This is pure QOL, no gameplay advantage

## 2. Color Gradient Themes
- Animated gradient backgrounds for boxes
- Smoother color transitions instead of the current static colors
- "Ambient" mode that shifts colors based on biome you're in

## 3. HUD Animations
- Smooth slide-in/slide-out when toggling
- Fade transitions when values change
- Element shake/pulse on critical events (low health) - purely visual feedback

## 4. Historical Data Visualization
- FPS graph over last 60 seconds (mini chart)
- Coordinate trail/path visualization showing where you've been
- Time-spent-in-biome tracker

## 5. Sound Cues (Contextual)
- Optional audio feedback for inventory full, low oxygen, etc.
- Customizable alert tones
- Purely informational, not providing unfair advantage

## Information Display Additions (Safe - All Client-Side Data)

### 6. Light Level Display
- Show block light level at player position or crosshair
- **Why it's safe**: This prevents mob spawning guesswork but doesn't reveal ores or hidden structures

### 7. Durability Warnings
- Visual indicator when held item is <20% durability
- Cycle through hotbar to show all low-durability items
- **Why it's safe**: Just displays existing item data more conveniently

### 8. Session Statistics
- Playtime this session
- Blocks mined/placed counter
- Distance traveled (walking/swimming/flying separately)
- Deaths this session
- **Why it's safe**: All personal stats, no competitive advantage

### 9. Equipment Status Display
- Show armor durability as percentage
- Active potion effects with timers (already in vanilla F3, just cleaner)
- **Why it's safe**: Reorganizes existing information

### 10. World Info Enhancements
- Moon phase indicator
- Local difficulty value
- Slime chunk indicator (if in slime chunk)
- **Note**: Slime chunks might be borderline - could make it require F3 screen to be visible at least once

## Comfort Features (Safe)

### 11. Configurable Anchoring
- More anchor points (center, custom X/Y positions)
- Safe zones to prevent overlap with other mods
- Margin/padding controls

### 12. HUD Element Resizing
- Individual element scaling (not just global)
- Auto-scale based on screen resolution

### 13. Color Blindness Modes
- Preset color schemes for different types of color blindness
- High contrast mode

### 14. Mini-Compass Rose
- Small directional indicator showing cardinal directions
- Optional degree readout (0-360°)
- **Why it's safe**: Just a prettier version of F3 direction data

## Advanced Data (Borderline - Be Careful)

### 15. Chunk Boundary Visualization
- Overlay showing chunk borders
- **Status**: Probably safe - similar to existing mods like MiniHUD
- Make it a separate toggle that's OFF by default

### 16. Redstone Signal Strength (At Crosshair)
- Shows redstone power level when looking at redstone components
- **Status**: Probably safe - helps with technical builds, not PvP

## 17. F3 Debug Screen Overhaul (Major Feature)
**Concept:** Replace the cluttered vanilla F3 text wall with a modern, tabbed interface using PowerHUD's styling.
**Status:** Safe / Pure Client-Side (Reorganizing existing vanilla data).

### Design: Tabbed Interface
Instead of a wall of text, split data into clean tabs toggleable via hotkeys (e.g., F3 + 1/2/3).

#### Tab 1: Performance
- FPS (Current, Min, Max) & Frame Time
- Memory Usage (Allocated vs Used)
- GPU & CPU Names
- Entity Count (Total vs Rendered)
- *Optional: Mini FPS Graph (Last 60s)*

#### Tab 2: Position & World
- Coordinates (XYZ) & Block Position
- Chunk Coordinates (Rel/Abs)
- Facing Direction (Yaw/Pitch)
- Biome & Local Difficulty
- Light Levels (Block/Sky) & Days Played

#### Tab 3: Target Info (Raycast)
- Target Block Name & Position
- Block Properties (Rotation, Waterlogged, etc.)
- Block Tags (#minecraft:logs)
- Mining Harvestability (Optional/Safe?)

#### Tab 4: System
- Java Version & JVM Arguments
- OS & Display Resolution
- Mod Loader & Minecraft Version

### Implementation Strategy
- **Mixin:** `@Mixin(DebugHud.class)` targeting `render`.
- **Logic:** Check `PowerHudConfig.replaceF3Screen`. If true, `ci.cancel()` the vanilla render and call `PowerHudF3Renderer.render()`.
- **Customization:**
    - Toggle individual sections.
    - Click-to-copy coordinates/seed.
    - "Minimal Mode" option.
