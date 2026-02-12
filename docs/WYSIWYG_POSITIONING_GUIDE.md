# WYSIWYG HUD Positioning System - v1.11.0

## Overview
PowerHUD v1.11.0 introduces a revolutionary **free-form WYSIWYG (What You See Is What You Get)** positioning system for HUD elements. This replaces the old column-based layout with pixel-perfect drag-and-drop positioning.

## What Changed

### Before (v1.10.1 and earlier)
- Elements were constrained to 3 columns: LEFT, CENTER, RIGHT
- Vertical ordering only within each column
- Arrow buttons to move elements up/down or left/right
- Limited flexibility in positioning

### After (v1.11.0)
- **True free-form positioning** - place elements ANYWHERE on screen
- **Live preview** - see exactly how your HUD looks as you edit
- **Intuitive drag-and-drop** - click and drag elements to position them
- **Palette system** - available elements shown at bottom, drag to add
- **Smart right-alignment** - elements on right side use negative X positioning

## How to Use

### Opening the Editor
1. Press **O** to open PowerHUD config
2. Click **HUD Element Order** button
3. The WYSIWYG editor opens with live preview

### Adding Elements
1. Look at the **palette at the bottom** of the screen
2. Available elements are shown in gray boxes
3. **Click and drag** an element from the palette to anywhere on screen
4. Release to place it

### Moving Elements
1. **Click and hold** on any existing HUD element
2. **Drag** it to the new position
3. Release to drop it there
4. Position is saved automatically when you click "Done"

### Removing Elements
1. **Click and drag** an element you want to remove
2. **Drag it down** to the palette area at the bottom
3. Release to remove it from your HUD

### Understanding Positioning
- **Positive X** (e.g., x=10): Distance from LEFT edge of screen
- **Negative X** (e.g., x=-150): Distance from RIGHT edge of screen
- **Y**: Distance from TOP of screen
- Elements automatically snap to negative X when placed on right half

### Saving Your Layout
1. Click **Done** to save and exit
2. Click **Cancel** to discard changes
3. Click **Clear All** to remove all elements
4. Click **Reset Default** to restore original layout

## Technical Details

### Data Structure
```java
public static class LayoutEntry {
    String id;           // Element identifier (e.g., "FPS", "XYZ")
    int x;               // X position (negative = from right)
    int y;               // Y position (from top)
    boolean useFreeForm; // Always true in v1.11.0
}
```

### Position Calculation
- At render time, negative X values are converted: `x = screenWidth + x`
- This ensures right-aligned elements stay properly positioned on different screen sizes

### Backwards Compatibility
- Old config files with alignment-based positioning are automatically converted
- Conversion happens on first load:
  - LEFT (align=0) → x=10
  - CENTER (align=1) → x=-200
  - RIGHT (align=2) → x=-150

## Element Sizing

The editor automatically calculates approximate sizes for:
- **INV** (Inventory): 200x60 pixels
- **XYZ** (Coordinates): 150x12 pixels
- **FPS**: 120-180x12 pixels (depending on mode)
- **BLOCK_STATS**: 160x12 pixels
- **Others**: 120x12 pixels (default)

## Mouse Controls

| Action | Control |
|--------|---------|
| Add element | Drag from palette to screen |
| Move element | Click and drag element |
| Remove element | Drag element to palette |
| Hover highlight | Mouse over element shows white outline |

## Config File Format

Example `powerhud.json` with free-form positions:
```json
{
  "hudOrder": [
    {
      "id": "FPS",
      "x": -150,
      "y": 10,
      "useFreeForm": true
    },
    {
      "id": "XYZ",
      "x": 10,
      "y": 10,
      "useFreeForm": true
    }
  ]
}
```

## Tips & Tricks

1. **Right-aligned elements**: Place on right side of screen, they'll auto-adjust for different resolutions
2. **Grouping**: Place related elements near each other (e.g., FPS stats on top-right)
3. **Overlay safe zones**: Keep elements away from center and top-center where oxygen overlay appears
4. **Preview mode**: The live preview shows exactly what you'll see in-game
5. **Undo**: Click Cancel to discard all changes since opening the editor

## Known Limitations

- **Oxygen element** is NOT in the workbench (it's a special centered overlay)
- No snap-to-grid (coming in future version?)
- No undo/redo within editing session (must cancel and reopen)
- Element collision detection only for highlighting, not prevention

## Future Enhancements (Ideas)

- Snap-to-grid option
- Copy/paste element positions
- Save/load multiple layout presets
- Export/import layout configurations
- Alignment guides and ruler overlay
- Group elements together
- Lock element positions

## Rollback Instructions

If you need to revert to v1.10.1:
1. Backup files are in `backup/` directory with timestamps
2. Copy backup files back to `src/main/java/net/steve/powerhud/`
3. Update `gradle.properties` to `mod_version=1.10.1`
4. Run `.\gradlew.bat clean build`

## Credits

Implemented by: Steve
Date: February 12, 2026
Version: 1.11.0

