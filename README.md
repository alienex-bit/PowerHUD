# PowerHUD

A modern, highly customizable HUD overlay mod for Minecraft Fabric 1.21.4

## ✨ Features

### 📊 Performance & Diagnostics
- **Real-time FPS Display** with color-coded performance indicators
- **Animated FPS Dot** that pulses based on performance
- **Min/Max/Average FPS Tracking** with reset capability
- **Custom F3 Debug Screen** - Cleaner, tabbed debug interface

### 🗺️ Navigation & World Info
- **Coordinates Display** (XYZ) with block position
- **Facing Direction** with cardinal directions
- **Biome Information** - See current biome name
- **Time Display** - In-game time with day/night indicator
- **Gamemode Indicator** - Current game mode display

### 🎒 Inventory & Equipment
- **Visual Inventory Display** - Grid-based inventory visualization
- **Best Tool Suggestions** - Shows optimal tool for target block
- **Block Information** - Displays block you're looking at
- **Block Statistics** - Track blocks mined/placed

### 🌊 Survival Stats
- **Oxygen Level Indicator** - Visual oxygen bar for underwater exploration
- **Vitality Display** - Health and status information

### 🎨 Customization
- **9 Custom Fonts** - JetBrains Mono, Fira Code, Roboto Mono, Cascadia Code, Source Code Pro, Comic Mono, Monofur, Ubuntu Mono, Inter Mono
- **Text Effects** - Shimmer, Chroma, or classic styles
- **Multiple Color Themes** - Choose from preset color schemes
- **Flexible Layout** - Position elements left, center, or right-aligned
- **Background Styles** - Mist, Haze, Dusk, Obsidian, Solid, or Off
- **Adjustable Scaling** - Resize HUD elements to your preference
- **Custom Spacing** - Control line spacing and element gaps
- **HUD Reordering** - Drag-and-drop element arrangement

## 🎮 Default Keybindings

| Key | Action |
|-----|--------|
| \O\ | Open configuration screen |
| \H\ | Toggle HUD visibility |
| \R\ | Reset FPS statistics |
| \Z\ | Cycle debug screen modes |

*All keybindings are customizable in-game*

## 📦 Installation

### Requirements
- Minecraft **1.21.4**
- [Fabric Loader](https://fabricmc.net/use/) (latest version)
- [Fabric API](https://modrinth.com/mod/fabric-api) (required dependency)

### Steps
1. Install Fabric Loader for Minecraft 1.21.4
2. Download Fabric API and place in your \.minecraft/mods\ folder
3. Download the latest PowerHUD release from [Releases](https://github.com/alienex-bit/PowerHUD/releases)
4. Place \powerhud-vX.X.X.jar\ in your \.minecraft/mods\ folder
5. Launch Minecraft with the Fabric profile

## ⚙️ Configuration

Press \O\ in-game to open the configuration screen.

### Available Options
- **Toggle Elements** - Show/hide individual HUD components
- **Font Selection** - Choose from 9 custom monospace fonts
- **Color Themes** - Select preset color schemes or customize
- **Text Effects** - Apply shimmer or chroma effects
- **Layout & Alignment** - Position elements anywhere on screen
- **Background Style** - Adjust transparency and style
- **Scaling** - Adjust HUD size (50% - 200%)
- **Line Spacing** - Control vertical spacing between elements
- **HUD Order** - Reorder elements via drag-and-drop interface

## 🖼️ Screenshots

*Coming soon - Add screenshots of your HUD in action!*

## 🛠️ Building from Source

### Prerequisites
- Java 21 or higher
- Git

### Build Steps
\\\ash
# Clone the repository
git clone https://github.com/alienex-bit/PowerHUD.git
cd PowerHUD

# Build the mod (Windows)
gradlew.bat build

# Build the mod (Linux/Mac)
./gradlew build
\\\

The compiled JAR will be located in \uild/libs/\

### Development Environment
\\\ash
# Run Minecraft client with mod loaded
gradlew.bat runClient
\\\

## 📋 Roadmap

See [FUTURE_IDEAS.md](FUTURE_IDEAS.md) for planned features including:
- HUD Profiles (save/load configurations)
- Animated transitions and effects
- Historical data visualization (FPS graphs)
- Light level display
- Durability warnings
- Session statistics
- And much more!

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Report Bugs** - [Open an issue](https://github.com/alienex-bit/PowerHUD/issues/new) with details
2. **Suggest Features** - Share your ideas in the issues section
3. **Submit Pull Requests** - Fork, code, and PR!

### Development Guidelines
- Follow existing code style
- Test thoroughly before submitting
- Update documentation for new features

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🐛 Bug Reports & Support

Found a bug or need help?
- [Open an issue](https://github.com/alienex-bit/PowerHUD/issues/new)
- Provide Minecraft version, Fabric Loader version, and steps to reproduce

## 📊 Version History

- **v1.9.99** - Current development version
  - Dynamic versioning system
  - Enhanced configuration options
  - Multiple font support
  - Custom F3 debug screen

## 🙏 Acknowledgments

- Built with [Fabric](https://fabricmc.net/)
- Font resources from various open-source projects
- Inspired by the Minecraft modding community

---

**Made with ☕ by [alienex-bit](https://github.com/alienex-bit)**
