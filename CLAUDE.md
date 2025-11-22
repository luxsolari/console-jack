# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Console Jack is a console-based, text-graphics implementation of the classic casino game, aiming to be a "Card RPG" where players progress through casino ranks. Built using Java 21 with Lanterna for terminal UI rendering.

## Development Commands

### Build and Run

- `mvn compile` - Compile the project
- `mvn exec:java` - Run the application directly
- `mvn clean package` - Build JAR with dependencies
- `java -jar target/java-packageable-base-1.0-SNAPSHOT-jar-with-dependencies.jar` - Run the packaged JAR

### Platform-Specific Packaging

- `mvn clean package -Pwindows` - Package for Windows (creates `build-win/`)
- `mvn clean package -Pmac` - Package for macOS (creates `build-mac/`)
- `mvn clean package -Plinux` - Package for Linux (creates `build-linux/`)

### Code Quality

- Qodana static analysis configured in `qodana.yaml` (uses `jetbrains/qodana-jvm-community:2025.1`)

## Architecture

### Core Design Pattern

The application uses the **enum singleton pattern** (Effective Java, Item 3) for core subsystems. Access subsystems via:

- `RenderSubsystem.INSTANCE`
- `InputSubsystem.INSTANCE`
- `AudioSubsystem.INSTANCE`
- `MasterSubsystem.INSTANCE`
- `StateMachineSubsystem.INSTANCE`

### Threading Model

- **Master Thread**: Runs the main game loop at 8 UPS (125ms per update)
  - Processes UI command queue via `UICommandQueue.INSTANCE.processAll()` before rendering
- **Render Thread**: Handles terminal UI rendering using Lanterna
- **Input Thread**: Processes keyboard/input events
- **Audio Thread**: Manages audio subsystem

See [Threading Model Update](docs/THREADING_MODEL_UPDATE.md) for detailed architecture.

### State Management

- State machine managed by `StateMachineManager` (static facade)
- States implement `LoopableState` interface
- Initial state: `MainMenuState`
- State transitions via push/pop/replace operations

### ECS (Entity Component System)

- `EntityPool`: Manages game entities
- Components: `Position`, `Visual`, `Card`, `CardArt`, `CardSprite`
- Systems: `DisplayListSystem` for rendering
- ECS updates run in the main game loop

### Dynamic UI System

Thread-safe reactive UI modification system supporting:
- **Observable Properties**: `UIProperty<T>` for reactive data binding
- **Command Queue**: `UICommandQueue` for thread-safe imperative updates
- **Bindable Components**: All UI components support `bind()` for automatic updates
- **Dynamic Management**: Runtime add/remove/modify UI elements

Key APIs:
- `UICommandQueue.INSTANCE.enqueue(UICommand)` - Queue UI updates from any thread
- `label.bindText(UIProperty<String>)` - Reactive property binding
- `menu.removeItem(index)` / `menu.addItem(text, action)` - Dynamic menu management

See [Dynamic UI System Guide](docs/DYNAMIC_UI_SYSTEM.md) for comprehensive documentation.

### Package Structure

- `net.luxsolari.engine.*`: Core game engine
  - `ecs/`: Entity-Component-System implementation
  - `manager/`: Static utility managers (Input, Audio, Render, StateMachine)
  - `systems/`: Subsystem interfaces and implementations
  - `states/`: Base state interfaces
  - `ui/`: UI component framework with dynamic modification support
- `net.luxsolari.game.*`: Game-specific implementation
  - `states/`: Concrete game states (MainMenu, Gameplay, Pause)
  - `ecs/`: Game-specific components

### Key Technologies

- **Lanterna 3.1.2**: Terminal/console UI framework
- **Java 21**: Language version with preview features enabled
- **Maven**: Build system with multi-platform packaging profiles

## Entry Point

Main class: `net.luxsolari.game.Main` - Initializes logging and starts `MasterSubsystem.INSTANCE`

- Enter architect mode when commanded with either "Enter Architect Mode" or "/architect-mode". Use docs/ARCHITECT_MODE.md ruleset.
- Enter RIPER mode when commanded with either "Enter RIPER Mode" or "/riper-mode". Use docs/RIPER_MODE.md ruleset.

## Project Memories
- Always refer to main docs inside the @docs/ directory and base your work on them.
- Always check documentation is aligned with changes, refactors or modifications you made to the code. If documentation gaps exist, update relevant docs or create new where appropiate. Make sure all documentation for the projects lives under the @docs/ directory.
- When making architectural changes or major refactors, always review your work to ensure the documentation is aligned with the codebase.
- Always follow git workflow branching model (see https://nvie.com/posts/a-successful-git-branching-model/)
- develop and master are protected branches. When working on features, refactors, fixes, etc. always make sure you create an appropiate branch for the work you'll do.