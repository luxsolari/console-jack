# Console Jack - AI Coding Agent Instructions

## Project Overview
Console-based blackjack "Card RPG" built with Java 21 and Lanterna terminal UI. Players progress through casino ranks in ASCII text-graphics glory.

## Core Architecture Patterns

### Enum Singleton Pattern (Effective Java, Item 3)
All subsystems use enum singletons for thread-safe, JVM-wide singleton enforcement:
```java
public enum RenderSubsystem implements Subsystem {
    INSTANCE;
    // Access: RenderSubsystem.INSTANCE.methodName()
}
```
**Never create new instances** - always use `.INSTANCE`. See `src/main/java/net/luxsolari/engine/systems/internal/` for examples.

### Subsystem Lifecycle
All subsystems implement `Subsystem` interface with this lifecycle:
1. `init()` - Initialize resources
2. `start()` - Begin operations
3. `update()` - Main loop logic (called repeatedly)
4. `stop()` - Halt operations
5. `cleanUp()` - Release resources

Default `run()` calls: `init() → update() → cleanUp()`

### Threading Model (4 Threads)
- **Master Thread**: Main game loop at 8 UPS (125ms per update)
- **Render Thread**: Terminal UI rendering via Lanterna
- **Input Thread**: Keyboard event processing
- **Audio Thread**: Sound management

**Critical**: Use lock-free communication (queues, immutable records, volatile flags). No shared mutable state without synchronization.

## Package Structure & Responsibilities

### Engine Layer (`net.luxsolari.engine.*`)
Core game engine - **generic, reusable components only**:
- `systems/internal/`: Core subsystems (Master, Render, Input, Audio, StateMachine)
- `manager/`: Static facades exposing subsystem functionality (StateMachineManager, RenderManager, etc.)
- `ecs/`: Entity-Component-System framework (EntityPool, Component interface, EcsSystem)
- `ui/`: Custom terminal UI framework (Menu, Label, UIWidget, UIContainer)
- `states/`: Base state interface (`LoopableState`)

### Game Layer (`net.luxsolari.game.*`)
Blackjack-specific implementation:
- `states/`: Concrete game states (MainMenuState, GameplayState, PauseState)
- `ecs/`: Game components (Card, CardArt, CardSprite)

**Rule**: Never add game-specific logic to engine layer. Keep engine generic.

## State Management

### State Machine (Stack-Based)
Managed via `StateMachineManager` static facade:
```java
StateMachineManager.push(new PauseState());   // Overlay state (e.g., pause menu)
StateMachineManager.pop();                     // Return to previous state
StateMachineManager.replace(new GameplayState()); // Transition to new state
```

### State Implementation Pattern
```java
public class NewState implements LoopableState {
    @Override public void onEnter() { /* Initialize */ }
    @Override public void update() { /* Per-frame logic at 8 UPS */ }
    @Override public void onExit() { /* Cleanup */ }
}
```

## Entity-Component-System (ECS)

### Component Design
**Components are data-only records** - no logic:
```java
public record Position(float relX, float relY, Anchor anchor) implements Component {}
public record Visual(TextCharacter glyph) implements Component {}
```

### Entity Management
```java
EntityPool pool = new EntityPool();
Entity card = pool.create();
card.add(new Position(0.5f, 0.5f, Anchor.CENTER));
card.add(new Visual(glyph));

// Query entities
List<Entity> renderable = pool.with(Position.class, Visual.class);
```

### System Processing
Systems process components - put logic here:
```java
public class DisplayListSystem implements EcsSystem {
    @Override public void update(EntityPool pool) {
        pool.with(Position.class, Visual.class).forEach(entity -> {
            // Generate render commands
        });
    }
}
```

## Development Workflow

### Build & Run
```bash
mvn compile              # Compile only
mvn exec:java            # Run during development
mvn clean package        # Build JAR with dependencies
java -jar target/java-packageable-base-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Platform Packaging
```bash
mvn clean package -Pwindows  # Creates build-win/
mvn clean package -Pmac      # Creates build-mac/
mvn clean package -Plinux    # Creates build-linux/
```

### Code Quality
```bash
mvn checkstyle:check    # Google Java Style enforcement
```

## Code Style Requirements

### Naming Conventions
- Classes: `PascalCase`
- Methods/fields: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase.separated`

### Documentation
**All public APIs require Javadoc**:
```java
/**
 * Brief description.
 *
 * <p>Longer explanation with implementation details.
 *
 * @param name parameter description
 * @return return value description
 */
```

### Style Enforcement
Google Java Style Guide enforced via Checkstyle (`google_checks.xml`). **All PRs must pass `mvn checkstyle:check`**.

## Key Technologies & Versions

- **Java**: 21 with preview features enabled
- **Lanterna**: 3.1.2 (terminal UI rendering)
- **AudioCue**: 2.1.0 (audio playback)
- **Maven**: Build system
- **Main Class**: `net.luxsolari.game.Main`

## Common Tasks

### Adding a New Subsystem
1. Create `public enum NewSubsystem implements Subsystem { INSTANCE; }`
2. Implement lifecycle methods
3. Register in `MasterSubsystem`
4. Create corresponding `Manager` facade if needed

### Adding UI Components
1. Extend `UIWidget` (single) or `UIContainer` (composite)
2. Implement `Focusable` if interactive
3. Override `render(Screen screen)` and `handleInput(KeyStroke keyStroke)`
4. See `src/main/java/net/luxsolari/engine/ui/Menu.java` for example

### Adding Game States
1. Implement `LoopableState` interface
2. Place in `net.luxsolari.game.states/`
3. Transition via `StateMachineManager.push/pop/replace()`

## Rendering System

### Z-Layer Architecture
Rendering uses a Z-ordered layer system for proper draw order. Lower indices render first (background), higher indices render on top (foreground).

**Core Components**:
```java
// Layer definition - name and render order
public record ZLayer(String name, int index) {}

// Layer content - positions mapped to characters
public record ZLayerData(Map<ZLayerPosition, TextCharacter> contents) {}

// Position within a layer
public record ZLayerPosition(int x, int y) {}

// Immutable render command - thread-safe handoff to render thread
public record RenderCmd(int layer, int x, int y, TextCharacter glyph) {}
```

**Rendering Pipeline**:
1. ECS systems generate `RenderCmd` objects each frame
2. Commands specify layer index, position (x, y), and glyph
3. `RenderSubsystem` sorts by layer index
4. Layers rendered in order: background → foreground
5. Thread-safe via immutable records (no locks needed)

**Usage Pattern**:
```java
// In RenderManager or ECS systems
RenderManager.drawChar(layerIdx, x, y, character, foreground, background);

// Layers are automatically created on-demand
// Use consistent layer indices for proper Z-ordering
```

**Best Practices**:
- Define layer constants for readability (e.g., `BACKGROUND = 0`, `UI = 100`)
- Lower indices = further back, higher indices = closer to viewer
- Each layer uses `ConcurrentHashMap` for thread safety
- Clear layers between frames to avoid ghosting

## Error Handling

### Exception Hierarchy
All engine exceptions extend `EngineException` (which extends `RuntimeException`):

```java
EngineException (base)
├── ResourceInitializationException  // Subsystem init failures
├── ResourceCleanupException          // Cleanup failures
├── InputException                    // Input processing errors
├── StateMachineException             // State transition errors
└── GameLoopException                 // Game loop timing issues
```

**Usage Pattern**:
```java
@Override
public void init() {
    try {
        // Initialize resources
    } catch (IOException e) {
        throw new ResourceInitializationException("Failed to load font", e);
    }
}

@Override
public void cleanUp() {
    try {
        // Release resources
    } catch (Exception e) {
        throw new ResourceCleanupException("Failed to close screen", e);
    }
}
```

**Guidelines**:
- Use specific exception types for better error categorization
- Always include descriptive messages
- Chain underlying causes with the two-argument constructor
- Log exceptions at appropriate levels before throwing
- All engine exceptions are unchecked (RuntimeException) - no forced catch blocks

## Resource Management

### Asset Locations
- Fonts: `src/main/resources/fonts/`
- Audio: `src/main/resources/audio/` (bgm/, sfx/)
- Config: `src/main/resources/logging.properties`

### Cleanup Requirements
- Always implement proper cleanup in `cleanUp()` methods
- Use try-with-resources where applicable
- Release Lanterna `Screen` resources in `RenderSubsystem.cleanUp()`
- Throw `ResourceCleanupException` for cleanup failures

## Performance Targets

- **Game Loop**: 8 UPS (125ms per update) - maintain consistency
- **Render Loop**: 60 FPS when possible
- **Object Creation**: Minimize allocations in game loop (consider object pooling)

## Critical Don'ts

❌ Don't instantiate subsystems - use `.INSTANCE`  
❌ Don't add game logic to engine layer  
❌ Don't add logic to ECS components (data only)  
❌ Don't use shared mutable state across threads without synchronization  
❌ Don't commit code that fails `mvn checkstyle:check`  
❌ Don't skip Javadoc on public APIs  

## Further Reading

- `docs/ARCHITECTURE.md` - Detailed architecture documentation
- `docs/DEVELOPER_GUIDE.md` - Comprehensive development guide
- `docs/IMPROVEMENT_PLAN.md` - Planned enhancements and technical debt
- `docs/CLAUDE.md` - Additional AI coding agent guidance
