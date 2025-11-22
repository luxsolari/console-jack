# Dynamic UI System

## Overview

The Dynamic UI System provides thread-safe, reactive mechanisms for modifying UI components at runtime. It consists of two complementary approaches:

1. **Observable Properties (Reactive)**: UI components automatically update when bound data changes
2. **Command Queue (Imperative)**: Explicit UI modification commands executed safely on the Master thread

## Architecture

### Core Components

```
┌─────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│  UIProperty<T>  │──────▶│ UICommandQueue   │──────▶│  Master Thread   │
│  (Observable)   │       │  (Thread-safe)   │       │  (Processes)     │
└─────────────────┘       └──────────────────┘       └──────────────────┘
        │                         ▲                           │
        │ notifies                │ enqueues                  │ executes
        ▼                         │                           ▼
┌─────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│PropertyChange   │       │   UICommand      │       │  UIComponent     │
│  Listener       │──────▶│   (Interface)    │       │  (Updated)       │
└─────────────────┘       └──────────────────┘       └──────────────────┘
```

### Threading Model

```
[Any Thread]
    │
    ├─→ UIProperty.setValue(value)
    │       └─→ UICommandQueue.enqueue(command)
    │
    └─→ UIUpdateCommands.setText(label, "text")
            └─→ UICommandQueue.enqueue(command)

[Master Thread - 8 UPS Loop]
    │
    ├─→ State.handleInput()
    ├─→ State.update()
    ├─→ UICommandQueue.INSTANCE.processAll()  ← NEW
    │       └─→ For each UICommand:
    │               └─→ command.execute()
    │                       └─→ Label.setText("text")
    │
    └─→ State.render()
            └─→ UIComponent.render(layerIdx)

[Render Thread - 10 FPS]
    │
    └─→ RenderSubsystem.renderLayers()
            └─→ Screen.refresh()
```

**Key Integration Point**: `UICommandQueue.processAll()` is called in `MasterSubsystem.java:148` after state update and before rendering.

### Thread Safety

| Component | Thread Access | Synchronization |
|-----------|---------------|-----------------|
| `UIProperty.setValue()` | Any thread | Synchronized method |
| `UICommandQueue.enqueue()` | Any thread | BlockingQueue |
| `UICommandQueue.processAll()` | Master thread only | N/A (single-threaded) |
| `UIComponent` mutations | Master thread only | Via command queue |

## Usage Patterns

### Pattern 1: Observable Properties (Reactive)

Best for: Real-time data that changes frequently (scores, stats, timers)

```java
// Create observable properties
UIProperty<String> playerName = new UIProperty<>("Player 1");
UIProperty<Integer> score = new UIProperty<>(0);

// Create UI components
Label nameLabel = new Label(10, 10, "");
Label scoreLabel = new Label(10, 12, "");

// Bind properties to components
nameLabel.bindText(playerName);
scoreLabel.bind(score, (lbl, value) ->
    ((Label) lbl).setText("Score: " + value)
);

// Update from any thread - UI updates automatically
score.setValue(100);  // Label shows "Score: 100"
playerName.setValue("Player 2");  // Label shows "Player 2"

// Clean up when done
nameLabel.unbindAll();
scoreLabel.unbindAll();
```

### Pattern 2: Direct Commands (Imperative)

Best for: One-time UI changes, complex mutations

```java
// Enqueue individual commands from any thread
UICommandQueue.INSTANCE.enqueue(
    UIUpdateCommands.setText(label, "New Text")
);

UICommandQueue.INSTANCE.enqueue(
    UIUpdateCommands.setForegroundColor(label, TextColor.ANSI.GREEN)
);

// Batch multiple commands atomically
UICommandQueue.INSTANCE.enqueueBatch(List.of(
    UIUpdateCommands.setText(label1, "Line 1"),
    UIUpdateCommands.setText(label2, "Line 2"),
    UIUpdateCommands.setVisible(label3, false)
));

// Complex updates using generic updater
UICommandQueue.INSTANCE.enqueue(
    UIUpdateCommands.updateComponent(menu, m -> {
        m.clearItems();
        m.addItem("New Item 1", action1);
        m.addItem("New Item 2", action2);
    })
);
```

### Pattern 3: Dynamic Menu Management

```java
Menu menu = new Menu("Dynamic Menu")
    .addItem("Item 1", action1)
    .addItem("Item 2", action2);

// Add item dynamically (from any thread)
UICommandQueue.INSTANCE.enqueue(
    UIUpdateCommands.addMenuItem(menu, "Item 3", action3)
);

// Remove item at index
UICommandQueue.INSTANCE.enqueue(
    UIUpdateCommands.removeMenuItem(menu, 1)
);

// Replace item
UICommandQueue.INSTANCE.enqueue(
    UIUpdateCommands.replaceMenuItem(menu, 0, "Updated Item", newAction)
);

// Clear all items
UICommandQueue.INSTANCE.enqueue(
    UIUpdateCommands.clearMenu(menu)
);

// Or call directly from Master thread
menu.removeItem(1);
menu.replaceItem(0, "Updated Item", newAction);
menu.clearItems();
```

### Pattern 4: Container Manipulation

```java
UIContainer container = ...;
UIComponent newChild = ...;

// Add child at end
UICommandQueue.INSTANCE.enqueue(
    UIUpdateCommands.addChild(container, newChild)
);

// Insert at specific index
UICommandQueue.INSTANCE.enqueue(
    UIUpdateCommands.insertChild(container, 2, newChild)
);

// Remove specific child
UICommandQueue.INSTANCE.enqueue(
    UIUpdateCommands.removeChild(container, oldChild)
);

// Clear all children
UICommandQueue.INSTANCE.enqueue(
    UIUpdateCommands.clearChildren(container)
);
```

## API Reference

### UIProperty<T>

Observable property container that notifies listeners of value changes.

```java
public class UIProperty<T> {
    public UIProperty(T initialValue);
    public T getValue();
    public synchronized void setValue(T newValue);
    public Subscription addListener(PropertyChangeListener<T> listener);
    public int getListenerCount();
}
```

**Thread Safety**: All methods thread-safe. `setValue()` synchronized for atomicity.

### UICommandQueue

Thread-safe command processor (enum singleton).

```java
public enum UICommandQueue {
    INSTANCE;

    public void enqueue(UICommand command);
    public void enqueueBatch(List<UICommand> commands);
    public void processAll();  // Master thread only
    public int pendingCount();
    public void clear();
}
```

**System Property**: Enable debug logging with `-Dui.commands.debug=true`

### BindableUIComponent

Interface for UI components supporting property binding.

```java
public interface BindableUIComponent extends UIComponent {
    <T> Subscription bind(UIProperty<T> property,
                          PropertyBinder<? super UIComponent, T> binder);
    void unbindAll();
}
```

**Implementing Classes**: UIWidget (abstract base), Label, Menu, UIContainer

### Label Convenience Methods

```java
public class Label extends UIWidget {
    // Existing methods...
    public void setText(String text);
    public void setForegroundColor(TextColor color);
    public void setBackgroundColor(TextColor color);

    // NEW: Convenience binding methods
    public Subscription bindText(UIProperty<String> property);
    public Subscription bindForegroundColor(UIProperty<TextColor> property);
    public Subscription bindBackgroundColor(UIProperty<TextColor> property);
}
```

### Menu Dynamic Management

```java
public class Menu extends UIContainer implements Focusable {
    // Existing methods...
    public Menu addItem(String text, MenuAction action);

    // NEW: Dynamic item management
    public Menu removeItem(int index);
    public Menu replaceItem(int index, String text, MenuAction action);
    public Menu clearItems();
}
```

### UIContainer Child Manipulation

```java
public abstract class UIContainer extends UIWidget implements InputHandler {
    // Existing methods...
    public UIContainer addChild(UIComponent child);
    public boolean removeChild(UIComponent child);
    public List<UIComponent> getChildren();

    // NEW: Additional manipulation
    public UIContainer insertChild(int index, UIComponent child);
    public UIContainer clearChildren();
}
```

### UIUpdateCommands Factory

Static factory for creating common UI modification commands.

```java
public final class UIUpdateCommands {
    // Label operations
    public static UICommand setText(Label label, String text);
    public static UICommand setForegroundColor(Label label, TextColor color);
    public static UICommand setBackgroundColor(Label label, TextColor color);

    // Component operations
    public static UICommand setPosition(UIComponent component, int x, int y);
    public static UICommand setVisible(UIComponent component, boolean visible);

    // Container operations
    public static UICommand addChild(UIContainer container, UIComponent child);
    public static UICommand removeChild(UIContainer container, UIComponent child);
    public static UICommand removeChildAt(UIContainer container, int index);
    public static UICommand insertChild(UIContainer container, int index, UIComponent child);
    public static UICommand clearChildren(UIContainer container);

    // Menu operations
    public static UICommand addMenuItem(Menu menu, String text, MenuAction action);
    public static UICommand removeMenuItem(Menu menu, int index);
    public static UICommand replaceMenuItem(Menu menu, int index, String text, MenuAction action);
    public static UICommand clearMenu(Menu menu);

    // Generic updater
    public static <T extends UIComponent> UICommand updateComponent(T component, Consumer<T> updater);
}
```

## Memory Management

### Subscription Lifecycle

**IMPORTANT**: Always unsubscribe when disposing UI components to prevent memory leaks.

```java
// Manual subscription management
Subscription sub = label.bindText(property);
// ... use the label ...
sub.unsubscribe();

// Or unsubscribe all bindings
label.unbindAll();
```

### Best Practices

1. **State Disposal**: Call `unbindAll()` in state cleanup methods
   ```java
   @Override
   public void dispose() {
       if (label instanceof UIWidget widget) {
           widget.unbindAll();
       }
   }
   ```

2. **Scoped Properties**: Create properties in state constructors, dispose in cleanup
   ```java
   public class GameplayState implements LoopableState {
       private UIProperty<Integer> score;
       private Label scoreLabel;

       public GameplayState() {
           score = new UIProperty<>(0);
           scoreLabel = new Label(10, 10, "");
           scoreLabel.bind(score, (lbl, val) ->
               ((Label) lbl).setText("Score: " + val)
           );
       }

       @Override
       public void dispose() {
           scoreLabel.unbindAll();
       }
   }
   ```

3. **Monitor Queue Depth**: Log warnings when queue exceeds 1000 commands
   ```java
   int pending = UICommandQueue.INSTANCE.pendingCount();
   if (pending > 100) {
       logger.warning("High UI command queue depth: " + pending);
   }
   ```

## Performance Considerations

### Command Queue

- **Unbounded**: LinkedBlockingQueue grows without limit
- **Processing**: O(n) where n = pending commands
- **Typical Load**: 8 UPS game loop processes ~10-100 commands/second easily
- **Warning Threshold**: Logs warning at 1000+ pending commands

### Observable Properties

- **Listener Storage**: CopyOnWriteArrayList (optimized for reads)
- **setValue() Cost**: O(listeners) + queue enqueue cost
- **Memory**: ~200 bytes per property + (listeners × 100 bytes)

### Optimization Tips

1. **Batch Updates**: Use `enqueueBatch()` for multiple related changes
2. **Conditional Bindings**: Only bind properties that actually change
3. **Unbind Unused**: Remove bindings when UI components are hidden/inactive
4. **Throttle Updates**: For high-frequency data, throttle setValue() calls

## Debugging

### Enable Debug Logging

```bash
java -Dui.commands.debug=true -jar console-jack.jar
```

Output example:
```
FINE: Enqueued UI command (queue depth: 5)
FINE: Processing 5 UI commands
WARNING: UI command execution failed
    at UICommand.lambda$setText$0
```

### Monitor Metrics

```java
UICommandQueue queue = UICommandQueue.INSTANCE;
System.out.println("Pending: " + queue.pendingCount());
System.out.println("Processed: " + queue.getTotalProcessed());
System.out.println("Errors: " + queue.getTotalErrors());
```

### Common Issues

**Issue**: UI not updating
- **Cause**: Commands not being processed
- **Solution**: Verify `UICommandQueue.processAll()` is called in game loop

**Issue**: Memory leak
- **Cause**: Subscriptions not unsubscribed
- **Solution**: Call `unbindAll()` in state disposal

**Issue**: High queue depth
- **Cause**: Commands enqueued faster than processed
- **Solution**: Reduce update frequency or batch commands

## Migration Guide

### From Direct Mutation

**Before**:
```java
// Only safe from Master thread
label.setText("New Text");
```

**After (from any thread)**:
```java
UICommandQueue.INSTANCE.enqueue(
    UIUpdateCommands.setText(label, "New Text")
);
```

**After (reactive)**:
```java
UIProperty<String> text = new UIProperty<>("Initial");
label.bindText(text);
// Later, from any thread:
text.setValue("New Text");
```

### Adding Dynamic Menus

**Before**: Static menu created in constructor
```java
Menu menu = new Menu("Options")
    .addItem("Item 1", action1)
    .addItem("Item 2", action2);
```

**After**: Dynamic items based on game state
```java
Menu menu = new Menu("Options");
UIProperty<GameState> gameState = new UIProperty<>(currentState);

// Rebuild menu when game state changes
gameState.addListener((oldState, newState) -> {
    menu.clearItems();
    if (newState.isPaused()) {
        menu.addItem("Resume", this::resume);
        menu.addItem("Quit", this::quit);
    } else {
        menu.addItem("Pause", this::pause);
    }
});
```

## Architecture Decisions

### Why Command Queue + Observable Properties?

| Pattern | Best For | Drawbacks |
|---------|----------|-----------|
| **Observable Properties** | Reactive data binding, real-time updates | Listener overhead, potential leaks |
| **Command Queue** | One-time changes, complex mutations | Manual enqueueing, not automatic |
| **Hybrid (Both)** | Maximum flexibility, covers all use cases | Slightly more complex API |

We chose the hybrid approach to support both reactive bindings (automatic) and explicit commands (control).

### Why Enum Singleton for UICommandQueue?

- **Thread-safe initialization**: JVM guarantees enum singleton safety
- **Consistency**: Matches existing Console Jack pattern (RenderSubsystem, InputSubsystem, etc.)
- **Simplicity**: No lazy initialization or double-checked locking needed

### Why BlockingQueue Instead of Locks?

- **Lock-free enqueueing**: Better concurrency for multi-threaded enqueuing
- **FIFO ordering**: Guaranteed per-producer-thread
- **Simplicity**: No manual lock management

## Future Enhancements

### Potential Additions

1. **Animation System**: Tween properties over time
   ```java
   UIAnimator.tween(label.textProperty(), "Old", "New", Duration.seconds(1));
   ```

2. **Computed Properties**: Derive values from other properties
   ```java
   UIProperty<String> full = UIProperty.computed(
       () -> firstName.getValue() + " " + lastName.getValue(),
       firstName, lastName
   );
   ```

3. **Weak References**: Auto-cleanup for abandoned subscriptions
   ```java
   property.addWeakListener(listener);  // Auto-removed when listener GC'd
   ```

4. **Command Prioritization**: High-priority commands processed first
   ```java
   UICommandQueue.INSTANCE.enqueue(command, Priority.HIGH);
   ```

5. **Undo/Redo**: Command history for reversible operations
   ```java
   UndoableCommand cmd = UIUpdateCommands.setTextUndoable(label, "New");
   cmd.undo();
   ```

## Related Documentation

- [State Machine Guide](STATE_MACHINE_GUIDE.md) - How states integrate with UI
- [Threading Model Diagram](../src/main/resources/console-jack-thread-model.excalidraw) - Visual threading architecture
- [UI Component Hierarchy](ARCHITECTURE.md) - Existing UI system overview

## Change Log

### v1.0 - Dynamic UI System (2025-11-22)

**Added**:
- `UIProperty<T>` - Observable property container
- `UICommandQueue` - Thread-safe command processor
- `BindableUIComponent` - Interface for reactive bindings
- `UIUpdateCommands` - Static factory for common commands
- `Label.bindText/bindForegroundColor/bindBackgroundColor()` - Convenience methods
- `Menu.removeItem/replaceItem/clearItems()` - Dynamic item management
- `UIContainer.insertChild/clearChildren()` - Additional manipulation

**Modified**:
- `UIWidget` - Now implements `BindableUIComponent`
- `MasterSubsystem.loop()` - Calls `UICommandQueue.processAll()` before rendering

**Impact**: All UI components now support thread-safe dynamic modification
