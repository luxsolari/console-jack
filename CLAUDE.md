# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Console Jack is a console-based, text-graphics implementation of the classic casino game, aiming to be a "Card RPG" where players progress through casino ranks. Built using Java 21 with Lanterna for terminal UI rendering.

## Development Commands

### Build and Run

- `mvn compile` - Compile the project
- `mvn exec:java` - Run the application directly
- `mvn clean package` - Build JAR with dependencies
- `java -jar target/console-jack-1.0-SNAPSHOT-jar-with-dependencies.jar` - Run the packaged JAR

### Platform-Specific Packaging

- `mvn clean package -Pwindows` - Package for Windows (creates `build-win/`)
- `mvn clean package -Pmac` - Package for macOS (creates `build-mac/`)
- `mvn clean package -Plinux` - Package for Linux (creates `build-linux/`)

### Code Quality

- Qodana static analysis configured in `qodana.yaml` (uses `jetbrains/qodana-jvm-community:2025.1`)

## Architecture

### Core Design Pattern

The application uses a two-layer access model:

**Public manager facades** (game code uses these — static utility classes):
- `EntityManager` — create/destroy entities, register ECS systems
- `MasterManager` — engine lifecycle control (e.g. graceful shutdown)
- `StateMachineManager` — state push/pop/replace operations
- `RenderManager` — rendering commands and utilities
- `InputManager` — input event distribution
- `AudioManager` — sound playback control

**Internal coordinators / subsystems** (engine internals — do not access from game code):
- `MasterSubsystem.INSTANCE` — main game loop (enum singleton)
- `RenderSubsystem.INSTANCE` — Lanterna rendering (enum singleton)
- `InputSubsystem.INSTANCE` — keyboard input (enum singleton)
- `AudioSubsystem.INSTANCE` — audio management (enum singleton)
- `EntityCoordinator.INSTANCE` — ECS pool and system registry (enum singleton)
- `StateMachineCoordinator.INSTANCE` — state machine implementation (enum singleton)

### Threading Model

- **Master Thread**: Runs the main game loop at 8 UPS (125ms per update)
- **Render Thread**: Handles terminal UI rendering using Lanterna
- **Input Thread**: Processes keyboard/input events
- **Audio Thread**: Manages audio subsystem

### State Management

- State machine managed by `StateMachineManager` (static facade)
- States implement `LoopableState` interface
- Initial state: `MainMenuState`
- State transitions via push/pop/replace operations

### ECS (Entity Component System)

- `EntityManager`: Public API for creating/destroying entities and registering systems
- `EntityCoordinator`: Internal singleton managing `EntityPool` and ordered system list
- Components: `Position`, `Visual`, `ScalableVisual`, `Layer`, `Card`, `CardArt`, `CardSprite`
- Systems: `DisplayListSystem` for rendering
- ECS updates run in the main game loop via `EntityCoordinator`

### Package Structure

- `net.luxsolari.engine.*`: Core game engine
  - `ecs/`: Entity-Component-System implementation
  - `manager/`: Static utility managers (Input, Audio, Render, StateMachine)
  - `systems/`: Subsystem interfaces and implementations
  - `states/`: Base state interfaces
- `net.luxsolari.game.*`: Game-specific implementation
  - `states/`: Concrete game states (MainMenu, Gameplay, Pause)
  - `ecs/`: Game-specific components

### Key Technologies

- **Lanterna 3.1.2**: Terminal/console UI framework
- **Java 21**: Language version with preview features enabled
- **Maven**: Build system with multi-platform packaging profiles

## Entry Point

Main class: `net.luxsolari.game.Main` - Delegates to `MainEngine.bootstrap()`, which loads logging config and starts `MasterSubsystem.INSTANCE.run()`

- Enter architect mode when commanded with either "Enter Architect Mode" or "/architect-mode". Use docs/ARCHITECT_MODE.md ruleset.
- Enter RIPER mode when commanded with either "Enter RIPER Mode" or "/riper-mode". Use docs/RIPER_MODE.md ruleset.

## Project Memories
- Always refer to main docs inside the @docs/ directory and base your work on them.
- Always check documentation is aligned with changes, refactors or modifications you made to the code. If documentation gaps exist, update relevant docs or create new where appropiate. Make sure all documentation for the projects lives under the @docs/ directory.
- When making architectural changes or major refactors, always review your work to ensure the documentation is aligned with the codebase.