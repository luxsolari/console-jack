# Console Jack - Architecture Documentation

## Overview

Console Jack is built using a multi-threaded, subsystem-based architecture with an Entity Component System (ECS) for game logic and a custom UI framework for terminal rendering.

## Core Design Principles

### 1. Enum Singleton Pattern
All core subsystems use the enum singleton pattern (Effective Java, Item 3) for JVM-wide singleton enforcement:

```java
public enum MasterSubsystem implements Subsystem {
    INSTANCE;
    // implementation
}
```

**Benefits:**
- Thread-safe by default
- Serialization-safe
- Reflection-proof
- Memory efficient

### 2. Thread Separation
Each major concern runs in its own thread to prevent blocking:

- **Master Thread**: Game loop and ECS updates
- **Render Thread**: Terminal UI rendering
- **Input Thread**: Keyboard event processing
- **Audio Thread**: Sound management

## Architecture Layers

### Layer 1: Engine Core (`net.luxsolari.engine`)

#### Subsystems (`systems/internal/`)
Core subsystems that form the foundation:

- **`MasterSubsystem`**: Orchestrates the main game loop at 8 UPS
- **`RenderSubsystem`**: Manages Lanterna terminal rendering
- **`InputSubsystem`**: Handles keyboard input events
- **`AudioSubsystem`**: Manages sound effects and background music
- **`StateMachineSubsystem`**: Coordinates game state transitions

#### Managers (`manager/`)
Static facades providing centralized access:

- **`StateMachineManager`**: State push/pop/replace operations
- **`RenderManager`**: Rendering commands and utilities
- **`InputManager`**: Input event distribution
- **`AudioManager`**: Sound playback control

#### ECS Framework (`ecs/`)
Entity Component System implementation:

```
EntityPool
├── Entity (ID-based)
├── Component (data containers)
│   ├── Position
│   ├── Visual
│   └── Layer
└── EcsSystem (logic processors)
    └── DisplayListSystem
```

#### UI Framework (`ui/`)
Custom terminal UI components:

```
UIComponent (base)
├── UIWidget (single components)
│   ├── Label
│   └── MenuItem
└── UIContainer (composite components)
    └── Menu
```

**Key Interfaces:**
- **`Focusable`**: Components that can receive input focus
- **`InputHandler`**: Components that process input events

### Layer 2: Game Implementation (`net.luxsolari.game`)

#### Game States (`states/`)
Concrete implementations of game screens:

- **`MainMenuState`**: Main menu with navigation
- **`GameplayState`**: Blackjack game logic
- **`PauseState`**: Pause menu overlay

#### Game Components (`ecs/`)
Blackjack-specific ECS components:

- **`Card`**: Playing card data
- **`CardArt`**: Visual representation
- **`CardSprite`**: Rendering information

## Threading Model

### Master Thread (8 UPS)
```java
while (running) {
    long startTime = System.nanoTime();

    // Update ECS systems
    for (EcsSystem system : ecsSystems) {
        system.update(entityPool);
    }

    // Update current game state
    StateMachineManager.getCurrentState().update();

    // Sleep to maintain target UPS
    sleepForTargetUps(startTime);
}
```

### Thread Communication
- **Lock-free queues** for inter-thread communication
- **Immutable records** for data transfer (`RenderCmd`, `ZLayerData`)
- **Volatile flags** for state coordination

## State Management

### State Machine Pattern
```
StateMachineManager
├── State Stack (LIFO)
├── Push State (overlay)
├── Pop State (return)
└── Replace State (transition)
```

### State Lifecycle
```java
interface LoopableState {
    void onEnter();    // Initialize state
    void update();     // Per-frame logic
    void onExit();     // Cleanup
}
```

## ECS Architecture

### Entity Management
- **Entities**: Unique integer IDs
- **Component Storage**: Type-indexed maps
- **System Processing**: Component iteration

### Component Design
```java
// Data-only components
public record Position(int x, int y) implements Component {}
public record Visual(String text, TextColor color) implements Component {}
```

### System Processing
```java
public class DisplayListSystem implements EcsSystem {
    @Override
    public void update(EntityPool entityPool) {
        // Query entities with Position + Visual
        // Generate render commands
        // Submit to render subsystem
    }
}
```

## Rendering Pipeline

### Z-Layer System
```java
public enum ZLayer {
    BACKGROUND(0),
    GAME_OBJECTS(100),
    UI_BACKGROUND(200),
    UI_FOREGROUND(300),
    DEBUG_OVERLAY(400);
}
```

### Render Command Flow
```
ECS Systems → RenderCmd → RenderSubsystem → Lanterna → Terminal
```

## Resource Management

### Subsystem Lifecycle
```java
interface Subsystem {
    void init() throws ResourceInitializationException;
    void start();
    void stop();
    void cleanUp() throws ResourceCleanupException;
}
```

### Asset Loading
- **Fonts**: TTF files in `resources/fonts/`
- **Audio**: WAV files in `resources/audio/`
- **Configurations**: Properties files in `resources/`

## Error Handling Strategy

### Exception Hierarchy
```
RuntimeException
├── ResourceInitializationException
└── ResourceCleanupException
```

### Error Recovery
- **Graceful degradation** for non-critical failures
- **Logging** at appropriate levels
- **Resource cleanup** in finally blocks

## Performance Considerations

### Hot Paths
- **Game loop**: 8 UPS target (125ms budget)
- **Render loop**: 60 FPS target when possible
- **ECS queries**: Optimized for component iteration

### Memory Management
- **Object pooling** for frequently created objects
- **Immutable records** to reduce garbage collection
- **Primitive collections** where appropriate

## Extension Points

### Adding New Components
1. Create record implementing `Component`
2. Add to relevant entities in `EntityPool`
3. Create system to process the component

### Adding New States
1. Implement `LoopableState`
2. Add to game package
3. Register transitions in existing states

### Adding New UI Components
1. Extend `UIWidget` or `UIContainer`
2. Implement `Focusable` if interactive
3. Handle input in `handleInput()` method

## Testing Strategy

### Unit Testing
- **Component logic**: Isolated component behavior
- **System logic**: ECS system processing
- **State logic**: State transition behavior

### Integration Testing
- **Subsystem coordination**: Thread interaction
- **State machine**: State transition flows
- **UI framework**: Component interaction

### Performance Testing
- **Game loop timing**: UPS consistency
- **Memory usage**: Garbage collection impact
- **Thread contention**: Lock-free communication