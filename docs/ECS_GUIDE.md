# Console Jack - Entity Component System (ECS) Guide

A comprehensive guide to the Entity Component System architecture used for game logic in Console Jack.

## Table of Contents

- [Overview](#overview)
- [Core Concepts](#core-concepts)
- [EntityPool Management](#entitypool-management)
- [Creating Components](#creating-components)
- [Creating Systems](#creating-systems)
- [Component Queries](#component-queries)
- [Integration with Rendering](#integration-with-rendering)
- [Best Practices](#best-practices)
- [Real-World Examples](#real-world-examples)
- [Common Patterns](#common-patterns)
- [Troubleshooting](#troubleshooting)

---

## Overview

The **Entity Component System (ECS)** is a data-oriented architecture pattern used in Console Jack for all game logic. Instead of traditional object-oriented hierarchies, ECS separates data (Components) from behavior (Systems) using lightweight entity containers.

### Key Benefits

- **Data-Oriented Design**: Components are pure data, systems are pure logic
- **Composition Over Inheritance**: Build complex entities from simple components
- **Performance**: Cache-friendly iteration, no virtual calls
- **Flexibility**: Add/remove components at runtime
- **Simple Mental Model**: Entities are just bags of components
- **Scalability**: Easy to add new components and systems

### ECS Architecture

```
┌───────────────────────────────────────────────────────┐
│                    Master Game Loop                    │
│                     (8 UPS)                           │
└───────────────────┬───────────────────────────────────┘
                    ↓
┌───────────────────────────────────────────────────────┐
│              EcsSystems (update each frame)           │
│  - DisplayListSystem                                  │
│  - (Future: AI, Physics, Collision systems)          │
└───────────────────┬───────────────────────────────────┘
                    ↓
┌───────────────────────────────────────────────────────┐
│                    EntityPool                         │
│  - Stores all active entities                         │
│  - Provides component queries                         │
└───────────────────┬───────────────────────────────────┘
                    ↓
┌───────────────────────────────────────────────────────┐
│                   Entities                            │
│  - Lightweight ID + component map                     │
│  - No behavior, only data                            │
└───────────────────┬───────────────────────────────────┘
                    ↓
┌───────────────────────────────────────────────────────┐
│                  Components                           │
│  - Position, Visual, Card, Layer, etc.               │
│  - Immutable data records                            │
└───────────────────────────────────────────────────────┘
```

---

## Core Concepts

### Entity

An **Entity** is a unique ID with a collection of components attached. Entities have no behavior—they're just containers.

**Analogy**: Think of an entity as a blank form that you fill out with different fields (components).

```java
Entity cardEntity = entityPool.create();  // Creates entity with unique ID
cardEntity.add(new Card(Rank.ACE, Suit.SPADES));  // Add card data
cardEntity.add(new Position(0.5f, 0.5f));          // Add position
cardEntity.add(new Layer(2));                      // Add render layer
```

#### Entity API

```java
int id()                              // Get unique entity ID
<T> void add(T component)            // Attach component (replaces existing)
<T> T get(Class<T> type)             // Retrieve component (null if missing)
boolean has(Class<? extends Component> type)  // Check if component exists
```

### Component

A **Component** is pure data—no behavior, no methods (except getters from record). Components are typically implemented as Java records for immutability.

**Analogy**: Components are like database columns or struct fields.

```java
// Marker interface
public interface Component {}

// Example component (record)
public record Position(float relX, float relY, Anchor anchor) implements Component {}

// Another component
public record Card(Rank rank, Suit suit) implements Component {}
```

**Key Characteristics**:
- **Data Only**: No logic, no methods
- **Immutable**: Preferably use Java records
- **Small**: Keep components focused on single concerns
- **Type-Based**: Components identified by their class type

### System

A **System** is pure logic that operates on entities with specific component combinations. Systems run every frame and transform component data into behavior.

**Analogy**: Systems are like database queries that SELECT entities WHERE they have certain components, then UPDATE them.

```java
@FunctionalInterface
public interface EcsSystem {
  void update(double dt, EntityPool pool);
}
```

**Key Characteristics**:
- **Logic Only**: No state, pure functions
- **Stateless**: All data lives in components
- **Query-Based**: Find entities with specific components
- **Frame-Based**: Called once per game loop update

---

## EntityPool Management

The `EntityPool` is a centralized container for all active entities. It provides creation, querying, and removal operations.

### Creating Entities

```java
// Access the entity pool (typically stored in game state or subsystem)
EntityPool entityPool = new EntityPool();

// Create a new entity
Entity entity = entityPool.create();

// Add components to define the entity
entity.add(new Card(Rank.ACE, Suit.SPADES));
entity.add(new Position(0.5f, 0.3f, Anchor.CENTER));
entity.add(new Visual(TextCharacter.fromCharacter('A')[0]));
entity.add(new Layer(2));
```

### Getting All Entities

```java
// Get immutable snapshot of all entities
List<Entity> allEntities = entityPool.all();

for (Entity entity : allEntities) {
  // Process each entity
  if (entity.has(Card.class)) {
    Card card = entity.get(Card.class);
    System.out.println("Card: " + card.rank() + " of " + card.suit());
  }
}
```

### Querying Entities

```java
// Find entities with specific components
List<Entity> renderables = entityPool.with(Position.class, Visual.class, Layer.class);

// Find entities with multiple component requirements
List<Entity> cards = entityPool.with(Card.class, Position.class);

// Query results are immutable snapshots
```

### Removing Entities

```java
// Remove all entities with specific component combinations
entityPool.removeWith(Card.class);  // Remove all card entities

entityPool.removeWith(Position.class, Visual.class);  // Remove all positioned visuals
```

### EntityPool API Reference

```java
Entity create()                              // Create new entity
List<Entity> all()                           // Get all entities (immutable)
List<Entity> with(Class<? extends Component>... types)  // Query entities
void removeWith(Class<? extends Component>... types)    // Remove matching entities
```

---

## Creating Components

Components are simple data records. Follow these patterns for creating new components.

### Pattern 1: Simple Data Record

```java
package net.luxsolari.engine.ecs;

/**
 * Component holding entity position.
 */
public record Position(float relX, float relY, Anchor anchor) implements Component {

  // Optional: convenience constructor
  public Position(float relX, float relY) {
    this(relX, relY, Anchor.TOP_LEFT);
  }

  // Optional: factory methods
  public static Position centered() {
    return new Position(0.5f, 0.5f, Anchor.CENTER);
  }
}
```

### Pattern 2: Enum-Based Data

```java
package net.luxsolari.game.ecs;

/**
 * Logical identity of a playing card.
 */
public record Card(Rank rank, Suit suit) implements Component {

  public enum Suit {
    SPADES('♠'), HEARTS('♥'), DIAMONDS('♦'), CLUBS('♣');

    private final char symbol;
    Suit(char symbol) { this.symbol = symbol; }
    public char symbol() { return symbol; }
  }

  public enum Rank {
    A("A"), TWO("2"), THREE("3"), /* ... */ K("K");

    private final String label;
    Rank(String label) { this.label = label; }
    public String label() { return label; }
  }
}
```

### Pattern 3: Wrapper Component

```java
package net.luxsolari.engine.ecs;

/**
 * Visual representation for rendering.
 */
public record Visual(TextCharacter glyph) implements Component {}
```

### Pattern 4: Validated Component

```java
package net.luxsolari.engine.ecs;

/**
 * Render layer for Z-ordering.
 */
public record Layer(int index) implements Component {

  // Compact constructor with validation
  public Layer {
    if (index < 0) {
      throw new IllegalArgumentException("Layer index must be non-negative");
    }
  }
}
```

### Component Design Guidelines

1. **Keep Components Small**: Single responsibility principle
2. **Use Records**: Immutable data with free equality/hashCode
3. **No Behavior**: Components should only hold data
4. **Descriptive Names**: `Position`, `Visual`, `Card` (not `Data`, `Info`)
5. **Validate in Constructor**: Use compact constructor for validation
6. **Package by Layer**: Engine components in `engine.ecs`, game components in `game.ecs`

---

## Creating Systems

Systems are the logic processors of ECS. They query the EntityPool for entities and transform their components.

### System Interface

```java
@FunctionalInterface
public interface EcsSystem {
  /**
   * Updates the system logic for the current frame.
   *
   * @param dt time elapsed since previous update (seconds)
   * @param pool shared pool containing all active entities
   */
  void update(double dt, EntityPool pool);
}
```

### Pattern 1: Simple System (Lambda)

```java
// Create system as lambda
EcsSystem positionUpdateSystem = (dt, pool) -> {
  pool.with(Position.class, Velocity.class).forEach(entity -> {
    Position pos = entity.get(Position.class);
    Velocity vel = entity.get(Velocity.class);

    // Update position based on velocity
    float newX = pos.relX() + vel.dx() * (float)dt;
    float newY = pos.relY() + vel.dy() * (float)dt;

    entity.add(new Position(newX, newY, pos.anchor()));
  });
};
```

### Pattern 2: System Class

```java
package net.luxsolari.engine.ecs.systems;

/**
 * Gathers all drawable entities and submits render commands.
 */
public class DisplayListSystem implements EcsSystem {

  @Override
  public void update(double dt, EntityPool pool) {
    if (!RenderSubsystem.INSTANCE.ready()) {
      return;
    }

    List<RenderCmd> displayList = new ArrayList<>();
    ViewportManager viewport = ViewportManager.INSTANCE;

    // Query entities with Position + Visual + Layer
    pool.with(Position.class, Visual.class, Layer.class).forEach(entity -> {
      Position pos = entity.get(Position.class);
      Visual visual = entity.get(Visual.class);
      Layer layer = entity.get(Layer.class);

      // Convert relative to screen coordinates
      int screenX = viewport.toScreenX(pos.relX(), pos.anchor());
      int screenY = viewport.toScreenY(pos.relY(), pos.anchor());

      // Create render command
      displayList.add(new RenderCmd(layer.index(), screenX, screenY, visual.glyph()));
    });

    // Submit to render subsystem
    RenderManager.submitDisplayList(displayList);
  }
}
```

### Pattern 3: Multi-Component System

```java
public class CardRenderSystem implements EcsSystem {

  @Override
  public void update(double dt, EntityPool pool) {
    ViewportManager viewport = ViewportManager.INSTANCE;
    List<RenderCmd> renderCmds = new ArrayList<>();

    // Find all card entities with sprite data
    pool.with(Position.class, Layer.class, CardSprite.class).forEach(entity -> {
      Position pos = entity.get(Position.class);
      Layer layer = entity.get(Layer.class);
      CardSprite sprite = entity.get(CardSprite.class);

      // Get card art
      String[] art = sprite.current();

      // Calculate screen position
      int screenX = viewport.toScreenX(pos.relX(), pos.anchor(), sprite.cols());
      int screenY = viewport.toScreenY(pos.relY(), pos.anchor(), sprite.rows());

      // Render each cell of the card sprite
      for (int row = 0; row < sprite.rows(); row++) {
        String line = art[row];
        for (int col = 0; col < sprite.cols(); col++) {
          char ch = line.charAt(col);
          renderCmds.add(new RenderCmd(
              layer.index(),
              screenX + col,
              screenY + row,
              TextCharacter.fromCharacter(ch)[0]
          ));
        }
      }
    });

    RenderManager.submitDisplayList(renderCmds);
  }
}
```

### Registering Systems

Systems are registered in the `MasterSubsystem` or game state and updated each frame:

```java
public class GameplayState implements LoopableState {
  private EntityPool entityPool;
  private EcsSystem displayListSystem;

  @Override
  public void start() {
    entityPool = new EntityPool();
    displayListSystem = new DisplayListSystem();
  }

  @Override
  public void update() {
    // Update all systems (dt = delta time in seconds)
    double dt = 0.125;  // 8 UPS = 0.125 seconds per frame
    displayListSystem.update(dt, entityPool);
  }
}
```

---

## Component Queries

The EntityPool provides powerful component queries for finding specific entities.

### Single Component Query

```java
// Find all entities with a Card component
List<Entity> cards = entityPool.with(Card.class);

for (Entity entity : cards) {
  Card card = entity.get(Card.class);
  System.out.println(card.rank() + " of " + card.suit());
}
```

### Multi-Component Query (AND)

```java
// Find entities that have ALL specified components
List<Entity> renderables = entityPool.with(Position.class, Visual.class, Layer.class);

// All returned entities are guaranteed to have all three components
for (Entity entity : renderables) {
  Position pos = entity.get(Position.class);    // Never null
  Visual visual = entity.get(Visual.class);     // Never null
  Layer layer = entity.get(Layer.class);        // Never null
}
```

### Query Performance

- **Linear Scan**: Queries iterate all entities (fine for small entity counts)
- **Immutable Results**: Query returns a snapshot (safe for iteration)
- **No Caching**: Each query creates a new list

**For Console Jack's small entity count (< 100), linear scans are perfectly adequate.**

---

## Integration with Rendering

The ECS integrates with the rendering system via the `DisplayListSystem`, which transforms entity components into render commands.

### Render Flow

```
Entity Components → DisplayListSystem → RenderCmd → RenderSubsystem → Screen
```

### Required Components for Rendering

An entity must have these components to be rendered:

1. **Position**: Where to draw (relative coordinates + anchor)
2. **Visual/CardSprite**: What to draw (glyph or multi-cell sprite)
3. **Layer**: Which Z-layer to draw on

### Example: Rendering a Single Glyph

```java
Entity entity = entityPool.create();
entity.add(new Position(0.5f, 0.5f, Anchor.CENTER));
entity.add(new Visual(TextCharacter.fromCharacter('A', TextColor.ANSI.RED, TextColor.ANSI.BLACK)[0]));
entity.add(new Layer(2));

// DisplayListSystem will automatically render this entity
```

### Example: Rendering a Card

```java
Entity cardEntity = entityPool.create();
cardEntity.add(new Card(Rank.ACE, Suit.SPADES));
cardEntity.add(new Position(0.5f, 0.3f, Anchor.CENTER));
cardEntity.add(new Layer(2));
cardEntity.add(new CardSprite(CardArt.fromCard(new Card(Rank.ACE, Suit.SPADES))));

// DisplayListSystem renders multi-cell sprites
```

### Coordinate System

Console Jack uses **relative positioning** (0.0 - 1.0) with anchors:

```java
// Top-left corner
new Position(0.0f, 0.0f, Anchor.TOP_LEFT)

// Center of screen
new Position(0.5f, 0.5f, Anchor.CENTER)

// Bottom-right corner
new Position(1.0f, 1.0f, Anchor.BOTTOM_RIGHT)
```

The `ViewportManager` converts relative coordinates to absolute screen coordinates based on current terminal size.

---

## Best Practices

### 1. Keep Components Pure Data

```java
// ✅ Good: Pure data
public record Health(int current, int maximum) implements Component {}

// ❌ Bad: Contains logic
public record Health(int current, int maximum) implements Component {
  public boolean isDead() { return current <= 0; }  // Logic belongs in system
}
```

### 2. Keep Systems Stateless

```java
// ✅ Good: Stateless, operates on components
public class DamageSystem implements EcsSystem {
  public void update(double dt, EntityPool pool) {
    pool.with(Health.class, DamageTaken.class).forEach(entity -> {
      // Process damage
    });
  }
}

// ❌ Bad: Stores state
public class DamageSystem implements EcsSystem {
  private int totalDamageDealt = 0;  // Don't store state in systems
}
```

### 3. Use Immutable Components

```java
// ✅ Good: Record (immutable)
public record Position(float x, float y) implements Component {}

// ❌ Bad: Mutable class
public class Position implements Component {
  public float x, y;  // Mutable fields
}
```

### 4. Small, Focused Components

```java
// ✅ Good: Focused components
public record Position(float x, float y) implements Component {}
public record Velocity(float dx, float dy) implements Component {}
public record Health(int current, int max) implements Component {}

// ❌ Bad: God component
public record GameObject(
  float x, float y,
  float dx, float dy,
  int health, int maxHealth,
  String name, String description
) implements Component {}
```

### 5. Query Once Per System

```java
// ✅ Good: Query once
public void update(double dt, EntityPool pool) {
  List<Entity> entities = pool.with(Position.class, Velocity.class);
  for (Entity e : entities) {
    // Process
  }
}

// ❌ Bad: Query multiple times
public void update(double dt, EntityPool pool) {
  for (Entity e : pool.all()) {
    if (e.has(Position.class) && e.has(Velocity.class)) {  // Inefficient
      // Process
    }
  }
}
```

### 6. Clean Up Entities When Done

```java
@Override
public void end() {
  // Remove all card entities when leaving gameplay
  entityPool.removeWith(Card.class);

  // Or clear entire pool
  // entityPool.removeWith(Component.class); // Removes everything
}
```

### 7. Validate Component Data

```java
public record Layer(int index) implements Component {
  public Layer {
    if (index < 0) {
      throw new IllegalArgumentException("Layer index must be non-negative");
    }
  }
}
```

### 8. Use Factory Methods for Common Patterns

```java
public record Position(float relX, float relY, Anchor anchor) implements Component {

  public static Position centered() {
    return new Position(0.5f, 0.5f, Anchor.CENTER);
  }

  public static Position topLeft() {
    return new Position(0.0f, 0.0f, Anchor.TOP_LEFT);
  }
}

// Usage
entity.add(Position.centered());  // Clear and concise
```

---

## Real-World Examples

### Example 1: Card Entity Creation

**File**: `src/main/java/net/luxsolari/game/states/GameplayState.java`

```java
private void createRandomCardEntity() {
  Random random = new Random();

  // Create entity
  Entity cardEntity = MasterSubsystem.INSTANCE.getEntityPool().create();

  // Add Card component (game logic data)
  Card.Rank rank = Card.Rank.values()[random.nextInt(Card.Rank.values().length - 1)];
  Card.Suit suit = Card.Suit.values()[random.nextInt(Card.Suit.values().length)];
  cardEntity.add(new Card(rank, suit));

  // Add Position component (relative positioning)
  float randomX = random.nextFloat();
  float randomY = random.nextFloat();
  cardEntity.add(new Position(randomX, randomY, Anchor.TOP_LEFT));

  // Add Layer component (Z-order)
  cardEntity.add(new Layer(CARD_LAYER));

  // Add CardSprite component (visual representation)
  String[] cardFace = CardArt.fromCard(new Card(rank, suit), CardSizeTier.MEDIUM);
  cardEntity.add(new CardSprite(cardFace));

  LOGGER.info("Created card entity: " + rank + " of " + suit);
}
```

### Example 2: DisplayListSystem

**File**: `src/main/java/net/luxsolari/engine/ecs/systems/DisplayListSystem.java`

```java
public class DisplayListSystem implements EcsSystem {

  @Override
  public void update(double dt, EntityPool pool) {
    if (!RenderSubsystem.INSTANCE.ready()) {
      return;
    }

    List<RenderCmd> list = new ArrayList<>();
    ViewportManager viewport = ViewportManager.INSTANCE;

    // Render single-glyph visuals
    pool.with(Position.class, Visual.class, Layer.class).forEach(entity -> {
      Position p = entity.get(Position.class);
      Visual v = entity.get(Visual.class);
      Layer l = entity.get(Layer.class);

      int screenX = viewport.toScreenX(p.relX(), p.anchor());
      int screenY = viewport.toScreenY(p.relY(), p.anchor());

      list.add(new RenderCmd(l.index(), screenX, screenY, v.glyph()));
    });

    // Render multi-cell card sprites
    pool.with(Position.class, Layer.class, CardSprite.class).forEach(entity -> {
      Position p = entity.get(Position.class);
      Layer l = entity.get(Layer.class);
      CardSprite sprite = entity.get(CardSprite.class);
      String[] art = sprite.current();

      int screenX = viewport.toScreenX(p.relX(), p.anchor(), sprite.cols());
      int screenY = viewport.toScreenY(p.relY(), p.anchor(), sprite.rows());

      // Render each cell
      for (int row = 0; row < sprite.rows(); row++) {
        String line = art[row];
        for (int col = 0; col < sprite.cols(); col++) {
          char ch = line.charAt(col);
          list.add(new RenderCmd(
              l.index(),
              screenX + col,
              screenY + row,
              TextCharacter.fromCharacter(ch)[0]
          ));
        }
      }
    });

    RenderManager.submitDisplayList(list);
  }
}
```

### Example 3: Component Definitions

**Engine Components** (`net.luxsolari.engine.ecs`):

```java
// Position with relative coordinates
public record Position(float relX, float relY, Anchor anchor) implements Component {
  public Position(float relX, float relY) {
    this(relX, relY, Anchor.TOP_LEFT);
  }
}

// Single-glyph visual
public record Visual(TextCharacter glyph) implements Component {}

// Render layer for Z-ordering
public record Layer(int index) implements Component {
  public Layer {
    if (index < 0) {
      throw new IllegalArgumentException("Layer index must be non-negative");
    }
  }
}
```

**Game Components** (`net.luxsolari.game.ecs`):

```java
// Playing card data
public record Card(Rank rank, Suit suit) implements Component {
  public enum Suit {
    SPADES('♠'), HEARTS('♥'), DIAMONDS('♦'), CLUBS('♣');
    private final char symbol;
    Suit(char symbol) { this.symbol = symbol; }
    public char symbol() { return symbol; }
  }

  public enum Rank {
    A("A"), TWO("2"), /* ... */ K("K");
    private final String label;
    Rank(String label) { this.label = label; }
    public String label() { return label; }
  }
}
```

---

## Common Patterns

### Pattern 1: Tagging Components

Use empty components as tags/flags:

```java
// Tag component (no data)
public record PlayerOwned() implements Component {}

public record AI() implements Component {}

// Query for player's cards
List<Entity> playerCards = entityPool.with(Card.class, PlayerOwned.class);

// Query for AI's cards
List<Entity> aiCards = entityPool.with(Card.class, AI.class);
```

### Pattern 2: State Components

Use components to represent entity state:

```java
public record FaceUp() implements Component {}
public record FaceDown() implements Component {}

// Flip card face up
if (cardEntity.has(FaceDown.class)) {
  cardEntity.add(new FaceUp());  // Replaces FaceDown
}

// Query face-up cards
List<Entity> faceUpCards = entityPool.with(Card.class, FaceUp.class);
```

### Pattern 3: Animation Components

```java
public record Animation(
    int currentFrame,
    int totalFrames,
    double frameTime,
    double elapsed
) implements Component {}

// Animation system
public class AnimationSystem implements EcsSystem {
  public void update(double dt, EntityPool pool) {
    pool.with(Animation.class, Visual.class).forEach(entity -> {
      Animation anim = entity.get(Animation.class);
      double newElapsed = anim.elapsed() + dt;

      if (newElapsed >= anim.frameTime()) {
        int nextFrame = (anim.currentFrame() + 1) % anim.totalFrames();
        entity.add(new Animation(
            nextFrame,
            anim.totalFrames(),
            anim.frameTime(),
            0.0
        ));

        // Update visual for new frame
        updateVisualForFrame(entity, nextFrame);
      } else {
        entity.add(new Animation(
            anim.currentFrame(),
            anim.totalFrames(),
            anim.frameTime(),
            newElapsed
        ));
      }
    });
  }
}
```

### Pattern 4: Lifetime Components

```java
public record Lifetime(double remaining) implements Component {}

// Lifetime system (remove entities after time expires)
public class LifetimeSystem implements EcsSystem {
  public void update(double dt, EntityPool pool) {
    pool.with(Lifetime.class).forEach(entity -> {
      Lifetime lifetime = entity.get(Lifetime.class);
      double newRemaining = lifetime.remaining() - dt;

      if (newRemaining <= 0) {
        // Entity expired, mark for removal
        entity.add(new Dead());
      } else {
        entity.add(new Lifetime(newRemaining));
      }
    });

    // Remove dead entities
    pool.removeWith(Dead.class);
  }
}

public record Dead() implements Component {}  // Tag for removal
```

### Pattern 5: Parent-Child Relationships

```java
public record Parent(int entityId) implements Component {}
public record Child(int entityId) implements Component {}

// Create parent-child relationship
Entity parent = entityPool.create();
Entity child = entityPool.create();

child.add(new Parent(parent.id()));
parent.add(new Child(child.id()));

// Find children of a parent
int parentId = parent.id();
List<Entity> children = entityPool.all().stream()
    .filter(e -> e.has(Parent.class) && e.get(Parent.class).entityId() == parentId)
    .toList();
```

---

## Troubleshooting

### Entities Not Rendering

**Symptom**: Created entities don't appear on screen

**Solutions**:
1. Ensure entity has Position, Visual/CardSprite, and Layer components:
```java
entity.add(new Position(0.5f, 0.5f, Anchor.CENTER));
entity.add(new Visual(glyph));
entity.add(new Layer(2));
```

2. Check layer index is valid (0 ≤ index < MAX_LAYERS)
3. Verify DisplayListSystem is being updated each frame
4. Check position is within screen bounds (0.0-1.0)

### Component Not Found (null)

**Symptom**: `entity.get(SomeComponent.class)` returns null

**Solutions**:
1. Check component was added: `entity.has(SomeComponent.class)`
2. Verify correct component type in query
3. Component may have been replaced by newer add() call

### Query Returns Empty List

**Symptom**: `entityPool.with(...)` returns empty list

**Solutions**:
1. Verify entities actually have ALL specified components
2. Check entities were added to the pool: `entityPool.create()`
3. Ensure entities weren't removed: `entityPool.removeWith(...)`
4. Use `entityPool.all()` to verify entities exist

### Memory Leaks

**Symptom**: Memory usage grows over time

**Solutions**:
1. Remove entities when no longer needed:
```java
@Override
public void end() {
  entityPool.removeWith(Card.class);  // Clean up cards
}
```

2. Clear entire pool between states:
```java
// In state transition
entityPool = new EntityPool();  // Replace with fresh pool
```

### Entity ID Conflicts

**Symptom**: Entities have duplicate IDs

**Solution**: This shouldn't happen. IDs are auto-incremented. If it does:
1. Don't manually set entity IDs
2. Always use `entityPool.create()` to create entities
3. Check for static ID counters being shared

### System Order Issues

**Symptom**: Systems updating in wrong order causes bugs

**Solution**: Control system execution order:
```java
@Override
public void update() {
  double dt = 0.125;

  // Systems run in order
  inputSystem.update(dt, entityPool);
  physicsSystem.update(dt, entityPool);
  collisionSystem.update(dt, entityPool);
  displayListSystem.update(dt, entityPool);  // Rendering last
}
```

---

## Additional Resources

- **Architecture Documentation**: See `ARCHITECTURE.md` for overall system design
- **State Machine Guide**: See `STATE_MACHINE_GUIDE.md` for state management
- **Rendering Guide**: See `RENDERING_GUIDE.md` for rendering integration
- **Developer Guide**: See `DEVELOPER_GUIDE.md` for development workflow

**Source Code References**:
- Entity: `src/main/java/net/luxsolari/engine/ecs/Entity.java`
- Component: `src/main/java/net/luxsolari/engine/ecs/Component.java`
- EcsSystem: `src/main/java/net/luxsolari/engine/ecs/EcsSystem.java`
- EntityPool: `src/main/java/net/luxsolari/engine/ecs/EntityPool.java`
- DisplayListSystem: `src/main/java/net/luxsolari/engine/ecs/systems/DisplayListSystem.java`
- Example components: `src/main/java/net/luxsolari/game/ecs/`

---

*Last Updated: 2025*
*For Console Jack - Terminal-based Blackjack Game*
