# Console Jack

Console-based, text-graphics implementation of the classic casino blackjack game.

Aims to be a "Card RPG" of sorts where you start a career as a frequent player at a local casino, and "move up the ranks" to the top-tier casinos of the world. All of that, in ASCII text-graphics, terminal-emulator-driven glory.

All right, let's cut to the real README now.
Originally conceived to be an excercise in Java architeture design and advanced Java features (think multithreading, concurrency, file I/O, dequeues, etc), this project is right now pivoting to become a mini-game TUI game engine built over the Lanterna library, with a (planned) card game on top.
I also use this as a tester for learning AI engineering, planning to add some form of AI functionality in the form of game features / mechanics while I learn.
Currently in active development, the features I'm including as of now are more engine-centered, so game features are still inexistent. If you find the "engine" package interesting or useful for whatever project it is you are cooking, feel free to copy/fork this repo and use it.

## Quick Start

### Prerequisites
- Java 21 or higher
- Maven 3.x

### Running the Game
```bash
# Clone and build
git clone <repository-url>
cd console-jack
mvn compile

# Run the game
mvn exec:java
```

### Building for Distribution
```bash
# Build JAR with dependencies
mvn clean package

# Run the packaged version
java -jar target/java-packageable-base-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Tech Stack

- **Language**: Java 21 (with preview features)
- **Build System**: Maven
- **Terminal UI**: Lanterna 3.1.2
- **Audio**: AudioCue 2.1.0
- **Code Quality**: Checkstyle (Google style), Qodana

## Architecture Overview

### Core Design Pattern
Core subsystems use the **enum singleton pattern** (Effective Java, Item 3):
- `RenderSubsystem.INSTANCE` - Terminal rendering
- `InputSubsystem.INSTANCE` - Keyboard input handling
- `AudioSubsystem.INSTANCE` - Sound effects and music
- `MasterSubsystem.INSTANCE` - Main game loop coordination
- `StateMachineSubsystem.INSTANCE` - Game state management

### Threading Model
- **Master Thread**: Game loop at 8 UPS (125ms per update)
- **Render Thread**: Terminal UI rendering with Lanterna
- **Input Thread**: Keyboard/input event processing
- **Audio Thread**: Audio subsystem management

### Entity Component System (ECS)
- **EntityPool**: Manages game entities
- **Components**: Position, Visual, Card, CardArt, CardSprite
- **Systems**: DisplayListSystem for rendering coordination

## Development

### Build Commands
```bash
mvn compile              # Compile source code
mvn clean package        # Build JAR with dependencies
mvn exec:java            # Run during development
mvn checkstyle:check     # Code style validation
```

### Platform Packaging
```bash
mvn clean package -Pwindows    # Windows package (build-win/)
mvn clean package -Pmac        # macOS package (build-mac/)
mvn clean package -Plinux      # Linux package (build-linux/)
```

### Project Structure
```
src/main/java/net/luxsolari/
├── engine/          # Core game engine
│   ├── ecs/         # Entity-Component-System
│   ├── manager/     # Static utility managers
│   ├── systems/     # Subsystem implementations
│   ├── states/      # Base state interfaces
│   └── ui/          # Custom UI framework
└── game/            # Game-specific implementation
    ├── ecs/         # Game components (Card, etc.)
    └── states/      # Game states (Menu, Gameplay, Pause)
```

## Contributing

1. Follow Google Java Style Guide (enforced by Checkstyle)
2. Add JavaDoc to all public APIs
3. Test changes with `mvn exec:java`
4. Verify build with `mvn clean package`

See `docs/IMPROVEMENT_PLAN.md` for planned enhancements and `docs/CLAUDE.md` for detailed development guidance.
