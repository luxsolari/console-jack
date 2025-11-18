# Console Jack - UI Components Guide

A practical reference guide for using the UI component framework in Console Jack.

## Table of Contents

- [Quick Start](#quick-start)
- [Core Concepts](#core-concepts)
- [Available Components](#available-components)
  - [Menu](#menu)
  - [MenuItem](#menuitem)
  - [Label](#label)
- [Common Patterns](#common-patterns)
- [Best Practices](#best-practices)
- [Real-World Examples](#real-world-examples)

---

## Quick Start

### Creating a Simple Menu

```java
import net.luxsolari.engine.ui.Menu;
import net.luxsolari.engine.manager.RenderManager;

public class MyState implements LoopableState {
  private Menu myMenu;

  @Override
  public void start() {
    myMenu = new Menu("My Menu Title")
        .addItem("Option 1", () -> doSomething())
        .addItem("Option 2", () -> doSomethingElse())
        .addItem("Quit", () -> quitGame())
        .setBorder(true);  // Optional: adds a border

    myMenu.focus();  // Make the menu active
  }

  @Override
  public void render() {
    RenderManager.clear(RenderManager.UI_LAYER);
    myMenu.render(RenderManager.UI_LAYER);
  }

  @Override
  public void handleInput() {
    if (!renderReady() || myMenu == null) {
      return;
    }

    KeyStroke ks = InputManager.poll();
    if (ks != null) {
      myMenu.handleInput(ks);  // Menu handles arrow keys and Enter
    }
  }

  @Override
  public void end() {
    if (myMenu != null) {
      myMenu.unfocus();
      myMenu = null;
    }
  }
}
```

### Creating Simple Labels

```java
import net.luxsolari.engine.ui.Label;
import com.googlecode.lanterna.TextColor;

// Create a colored label
Label titleLabel = new Label(10, 5, "Game Title",
    TextColor.ANSI.CYAN,
    RenderManager.DEFAULT_BG);

// Render it
titleLabel.render(RenderManager.UI_LAYER);

// Update text dynamically
titleLabel.setText("Updated Title");
```

---

## Core Concepts

### UI Component Hierarchy

```
UIComponent (interface)
├── UIWidget (abstract base for single components)
│   ├── Label (text display)
│   └── MenuItem (menu item with action)
└── UIContainer (abstract base for composite components)
    └── Menu (container for menu items)
```

### Key Interfaces

- **`UIComponent`**: Base interface for all UI elements
- **`Focusable`**: Interface for components that can receive focus
- **`InputHandler`**: Interface for components that process input

### Z-Layer System

UI components render on different layers for proper draw order:

```java
RenderManager.UI_LAYER         // Standard UI layer (default)
RenderManager.UI_LAYER + 1     // Higher layer (overlays)
RenderManager.UI_LAYER + 2     // Even higher layer
// etc.
```

Higher layer numbers render on top of lower layers.

---

## Available Components

### Menu

A navigable menu container that displays a list of menu items with a title.

#### Features
- Automatic keyboard navigation (Arrow Up/Down, Home/End)
- Enter key to execute selected item
- Optional border decoration
- Automatic centering on screen (configurable)
- Rainbow-colored title
- Focus management

#### Constructor

```java
// Centered menu (default)
Menu menu = new Menu("Title");

// Menu at specific position (not centered)
Menu menu = new Menu(x, y, "Title");
```

#### Methods

```java
// Builder pattern for configuration
Menu addItem(String text, MenuAction action)  // Add menu item
Menu setBorder(boolean showBorder)            // Show/hide border
Menu setCenterOnScreen(boolean center)        // Enable/disable auto-centering

// Focus management
void focus()                                  // Make menu active
void unfocus()                                // Deactivate menu
void resetFocus()                             // Reset focus state completely
boolean isFocused()                           // Check if focused
boolean canFocus()                            // Check if focusable

// Input handling
boolean handleInput(KeyStroke keyStroke)      // Process input events

// Rendering
void render(int layerIdx)                     // Render on specified layer

// Query
MenuItem getSelectedItem()                     // Get currently selected item
```

#### Keyboard Controls

| Key | Action |
|-----|--------|
| Arrow Up | Focus previous item |
| Arrow Down | Focus next item |
| Home | Focus first item |
| End | Focus last item |
| Enter | Execute focused item's action |

#### Complete Example

```java
public class PauseState implements LoopableState {
  private Menu pauseMenu;

  @Override
  public void start() {
    pauseMenu = new Menu("Paused")
        .addItem("Resume", () -> StateMachineManager.pop())
        .addItem("Options", this::showOptions)
        .addItem("Quit to Main Menu", () -> {
          StateMachineManager.clear();
          StateMachineManager.push(new MainMenuState());
        })
        .setBorder(true);

    pauseMenu.focus();
  }

  @Override
  public void render() {
    RenderManager.clear(RenderManager.UI_LAYER);
    if (pauseMenu != null) {
      pauseMenu.render(RenderManager.UI_LAYER);
    }
  }

  @Override
  public void handleInput() {
    if (!renderReady() || pauseMenu == null) {
      return;
    }

    KeyStroke ks = InputManager.poll();
    if (ks == null) {
      return;
    }

    // Handle special keys before delegating to menu
    if (ks.getKeyType() == KeyType.Escape) {
      StateMachineManager.pop();  // Quick exit
      return;
    }

    // Let menu handle navigation and selection
    pauseMenu.handleInput(ks);
  }

  @Override
  public void end() {
    if (pauseMenu != null) {
      pauseMenu.unfocus();
      pauseMenu = null;
    }
  }

  private void showOptions() {
    // Implementation for options
  }
}
```

---

### MenuItem

Individual menu item with text and an action to execute.

#### Features
- Highlighted when focused
- Executes action when selected
- Supports keyboard interaction

#### Constructor

```java
MenuItem item = new MenuItem(x, y, "Item Text", () -> doAction());
```

#### Methods

```java
String getText()                              // Get item text
void setText(String text)                     // Update item text
MenuAction getAction()                        // Get associated action
void setAction(MenuAction action)             // Update action
boolean handleInput(KeyStroke keyStroke)      // Process input
void render(int layerIdx)                     // Render the item
```

#### MenuAction Interface

```java
@FunctionalInterface
public interface MenuAction {
  void execute();
}
```

This is a functional interface, so you can use lambdas:

```java
// Lambda expression
.addItem("Start", () -> startGame())

// Method reference
.addItem("Quit", MasterSubsystem.INSTANCE::stop)

// Multi-line lambda
.addItem("Complex Action", () -> {
  doStep1();
  doStep2();
  doStep3();
})
```

---

### Label

Simple text display widget for non-interactive text.

#### Features
- Custom foreground and background colors
- Dynamic text updates
- Positioning control

#### Constructors

```java
// Default colors
Label label = new Label(x, y, "Text");

// Custom colors
Label label = new Label(x, y, "Text",
    TextColor.ANSI.CYAN,           // Foreground
    RenderManager.DEFAULT_BG);      // Background
```

#### Methods

```java
String getText()                              // Get label text
void setText(String text)                     // Update text (auto-resizes)
TextColor getForegroundColor()                // Get text color
void setForegroundColor(TextColor color)      // Set text color
TextColor getBackgroundColor()                // Get background color
void setBackgroundColor(TextColor color)      // Set background color
void setPosition(int x, int y)                // Move label
void render(int layerIdx)                     // Render the label
```

#### Available Colors

```java
// ANSI colors (always available)
TextColor.ANSI.BLACK
TextColor.ANSI.RED
TextColor.ANSI.GREEN
TextColor.ANSI.YELLOW
TextColor.ANSI.BLUE
TextColor.ANSI.MAGENTA
TextColor.ANSI.CYAN
TextColor.ANSI.WHITE

// Default colors from RenderManager
RenderManager.DEFAULT_FG  // White
RenderManager.DEFAULT_BG  // Black
```

#### Complete Example

```java
public class GameplayState implements LoopableState {
  private List<Label> instructionLabels;

  @Override
  public void start() {
    instructionLabels = new ArrayList<>();

    // Title label in cyan
    instructionLabels.add(new Label(0, 0, " Gameplay State ",
        TextColor.ANSI.CYAN, RenderManager.DEFAULT_BG));

    // Instruction labels in white
    instructionLabels.add(new Label(0, 0, "Press P to pause",
        TextColor.ANSI.WHITE, RenderManager.DEFAULT_BG));
    instructionLabels.add(new Label(0, 0, "Press 1 to create card",
        TextColor.ANSI.WHITE, RenderManager.DEFAULT_BG));
  }

  @Override
  public void render() {
    RenderManager.clear(RenderManager.UI_LAYER);

    if (instructionLabels != null) {
      ViewportManager viewport = ViewportManager.INSTANCE;
      int screenWidth = viewport.getWidth();
      int screenHeight = viewport.getHeight();

      // Center the labels vertically
      int totalHeight = instructionLabels.size();
      int startY = (screenHeight - totalHeight) / 2;

      // Position and render each label
      for (int i = 0; i < instructionLabels.size(); i++) {
        Label label = instructionLabels.get(i);
        int labelWidth = label.getText().length();
        int centerX = (screenWidth - labelWidth) / 2;
        label.setPosition(centerX, startY + i);
        label.render(RenderManager.UI_LAYER);
      }
    }
  }

  @Override
  public void end() {
    if (instructionLabels != null) {
      instructionLabels.clear();
      instructionLabels = null;
    }
  }
}
```

---

## Common Patterns

### Pattern 1: Basic Menu State

```java
public class MyMenuState implements LoopableState {
  private Menu menu;

  @Override
  public void start() {
    menu = new Menu("Title")
        .addItem("Item 1", this::action1)
        .addItem("Item 2", this::action2)
        .setBorder(true);
    menu.focus();
  }

  @Override
  public void render() {
    RenderManager.clear(RenderManager.UI_LAYER);
    if (menu != null) {
      menu.render(RenderManager.UI_LAYER);
    }
  }

  @Override
  public void handleInput() {
    if (!renderReady() || menu == null) {
      return;
    }
    KeyStroke ks = InputManager.poll();
    if (ks != null) {
      menu.handleInput(ks);
    }
  }

  @Override
  public void end() {
    if (menu != null) {
      menu.unfocus();
      menu = null;
    }
  }

  @Override
  public void pause() {}

  @Override
  public void resume() {
    if (menu != null) {
      RenderManager.clearAll();
      menu.resetFocus();
      menu.focus();
    }
  }

  @Override
  public void update() {}

  private void action1() { /* implementation */ }
  private void action2() { /* implementation */ }
}
```

### Pattern 2: Nested Menus (Dialog/Overlay)

```java
private void showDialog() {
  Menu dialogMenu = new Menu("Dialog Title")
      .addItem("Option", () -> {})
      .addItem("Close", () -> StateMachineManager.pop())
      .setBorder(true);

  StateMachineManager.push(new LoopableState() {
    @Override
    public void start() {
      RenderManager.clearAll();
      dialogMenu.resetFocus();
      dialogMenu.focus();
    }

    @Override
    public void handleInput() {
      KeyStroke ks = InputManager.poll();
      if (ks != null) {
        if (ks.getKeyType() == KeyType.Escape) {
          StateMachineManager.pop();
        } else {
          dialogMenu.handleInput(ks);
        }
      }
    }

    @Override
    public void render() {
      RenderManager.clear(RenderManager.UI_LAYER + 1);
      dialogMenu.render(RenderManager.UI_LAYER + 1);
    }

    @Override
    public void end() {
      dialogMenu.resetFocus();
      RenderManager.clearAll();
    }

    @Override
    public void pause() {}
    @Override
    public void resume() {}
    @Override
    public void update() {}
  });
}
```

### Pattern 3: Centered Text Block with Labels

```java
private List<Label> createCenteredTextBlock(String[] lines) {
  List<Label> labels = new ArrayList<>();

  for (String line : lines) {
    labels.add(new Label(0, 0, line,
        TextColor.ANSI.WHITE, RenderManager.DEFAULT_BG));
  }

  return labels;
}

private void renderCenteredLabels(List<Label> labels) {
  ViewportManager viewport = ViewportManager.INSTANCE;
  int screenWidth = viewport.getWidth();
  int screenHeight = viewport.getHeight();

  int totalHeight = labels.size();
  int startY = (screenHeight - totalHeight) / 2;

  for (int i = 0; i < labels.size(); i++) {
    Label label = labels.get(i);
    int centerX = (screenWidth - label.getText().length()) / 2;
    label.setPosition(centerX, startY + i);
    label.render(RenderManager.UI_LAYER);
  }
}
```

### Pattern 4: Dynamic Label Updates

```java
private Label scoreLabel;

@Override
public void start() {
  scoreLabel = new Label(10, 2, "Score: 0",
      TextColor.ANSI.YELLOW, RenderManager.DEFAULT_BG);
}

private void updateScore(int newScore) {
  scoreLabel.setText("Score: " + newScore);
}

@Override
public void render() {
  scoreLabel.render(RenderManager.UI_LAYER);
}
```

---

## Best Practices

### 1. Always Check for Null

```java
@Override
public void render() {
  if (menu != null) {  // ✅ Good
    menu.render(RenderManager.UI_LAYER);
  }
}
```

### 2. Clear Layers Before Rendering

```java
@Override
public void render() {
  RenderManager.clear(RenderManager.UI_LAYER);  // ✅ Clear first
  menu.render(RenderManager.UI_LAYER);
}
```

### 3. Clean Up Resources in end()

```java
@Override
public void end() {
  if (menu != null) {
    menu.unfocus();    // ✅ Unfocus before nulling
    menu = null;
  }

  if (labels != null) {
    labels.clear();    // ✅ Clear collections
    labels = null;
  }
}
```

### 4. Use resetFocus() When Resuming States

```java
@Override
public void resume() {
  if (menu != null) {
    RenderManager.clearAll();
    menu.resetFocus();    // ✅ Reset focus state
    menu.focus();         // ✅ Then focus again
  }
}
```

### 5. Handle EOF and Special Keys

```java
@Override
public void handleInput() {
  KeyStroke ks = InputManager.poll();
  if (ks == null) {
    return;
  }

  // Handle EOF (Ctrl+D)
  if (ks.getKeyType() == KeyType.EOF) {
    MasterSubsystem.INSTANCE.stop();
    return;
  }

  // Handle Escape for quick actions
  if (ks.getKeyType() == KeyType.Escape) {
    StateMachineManager.pop();
    return;
  }

  // Then delegate to menu
  menu.handleInput(ks);
}
```

### 6. Use Higher Layers for Overlays

```java
// Main menu on UI_LAYER
mainMenu.render(RenderManager.UI_LAYER);

// Dialog/overlay on higher layer
dialogMenu.render(RenderManager.UI_LAYER + 2);
```

### 7. Check renderReady() Before Rendering

```java
@Override
public void handleInput() {
  if (!renderReady()) {  // ✅ Check render system is ready
    return;
  }
  // ... handle input
}
```

---

## Real-World Examples

### Example 1: Main Menu (from MainMenuState.java)

```java
public class MainMenuState implements LoopableState {
  private Menu mainMenu;

  @Override
  public void start() {
    mainMenu = new Menu("Console Jack")
        .addItem("Start Game", () -> {
          StateMachineManager.replace(new GameplayState());
        })
        .addItem("Options", this::showOptions)
        .addItem("Quit", MasterSubsystem.INSTANCE::stop)
        .setBorder(true);

    mainMenu.focus();
  }

  @Override
  public void handleInput() {
    if (!renderReady() || mainMenu == null) {
      return;
    }

    KeyStroke ks = InputManager.poll();
    if (ks == null) {
      return;
    }

    if (ks.getKeyType() == KeyType.EOF) {
      MasterSubsystem.INSTANCE.stop();
      return;
    }

    mainMenu.handleInput(ks);
  }

  @Override
  public void render() {
    clearUILayers();
    if (!renderReady() || mainMenu == null) {
      return;
    }
    mainMenu.render(RenderManager.UI_LAYER);
  }

  @Override
  public void end() {
    if (mainMenu != null) {
      mainMenu.unfocus();
      mainMenu = null;
    }
  }

  private void clearUILayers() {
    for (int layer = RenderManager.UI_LAYER;
         layer < RenderManager.getLayerCount(); layer++) {
      RenderManager.clear(layer);
    }
  }

  private void showOptions() {
    // Implementation
  }
}
```

### Example 2: Pause Menu (from PauseState.java)

```java
public class PauseState implements LoopableState {
  private Menu pauseMenu;

  @Override
  public void start() {
    pauseMenu = new Menu("Paused")
        .addItem("Resume", () -> StateMachineManager.pop())
        .addItem("Quit to Main Menu", () -> {
          StateMachineManager.clear();
          StateMachineManager.push(new MainMenuState());
        })
        .setBorder(true);

    pauseMenu.focus();
  }

  @Override
  public void handleInput() {
    if (!renderReady() || pauseMenu == null) {
      return;
    }

    KeyStroke ks = InputManager.poll();
    if (ks == null) {
      return;
    }

    if (ks.getKeyType() == KeyType.EOF) {
      MasterSubsystem.INSTANCE.stop();
      return;
    }

    // Escape key for quick resume
    if (ks.getKeyType() == KeyType.Escape) {
      StateMachineManager.pop();
      return;
    }

    pauseMenu.handleInput(ks);
  }

  @Override
  public void render() {
    clearUILayers();
    if (!renderReady() || pauseMenu == null) {
      return;
    }
    pauseMenu.render(RenderManager.UI_LAYER);
  }

  @Override
  public void end() {
    if (pauseMenu != null) {
      pauseMenu.unfocus();
      pauseMenu = null;
    }
  }

  private void clearUILayers() {
    for (int layer = RenderManager.UI_LAYER;
         layer < RenderManager.getLayerCount(); layer++) {
      RenderManager.clear(layer);
    }
  }
}
```

### Example 3: Gameplay Instructions (from GameplayState.java)

```java
public class GameplayState implements LoopableState {
  private List<Label> instructionLabels;

  @Override
  public void start() {
    // Initialize instruction labels
    instructionLabels = new ArrayList<>();
    instructionLabels.add(new Label(0, 0, " Gameplay State ",
        TextColor.ANSI.CYAN, RenderManager.DEFAULT_BG));
    instructionLabels.add(new Label(0, 0, "Press P or Q or Esc to pause",
        TextColor.ANSI.WHITE, RenderManager.DEFAULT_BG));
    instructionLabels.add(new Label(0, 0, "Press 1 to create a card",
        TextColor.ANSI.WHITE, RenderManager.DEFAULT_BG));
    instructionLabels.add(new Label(0, 0, "Press 2 to clear cards",
        TextColor.ANSI.WHITE, RenderManager.DEFAULT_BG));
  }

  private void redrawLayers() {
    if (!renderReady()) {
      return;
    }

    // Clear and render instruction labels
    RenderManager.clear(RenderManager.UI_LAYER);
    if (instructionLabels != null) {
      ViewportManager viewport = ViewportManager.INSTANCE;
      int screenWidth = viewport.getWidth();
      int screenHeight = viewport.getHeight();

      // Calculate vertical starting position (centered)
      int totalHeight = instructionLabels.size();
      int startY = (screenHeight - totalHeight) / 2;

      // Position and render each label
      for (int i = 0; i < instructionLabels.size(); i++) {
        Label label = instructionLabels.get(i);
        int labelWidth = label.getText().length();
        int centerX = (screenWidth - labelWidth) / 2;
        label.setPosition(centerX, startY + i);
        label.render(RenderManager.UI_LAYER);
      }
    }
  }

  @Override
  public void end() {
    // Clean up instruction labels
    if (instructionLabels != null) {
      instructionLabels.clear();
      instructionLabels = null;
    }
  }
}
```

---

## Quick Reference

### Component Creation Cheat Sheet

```java
// Menu
Menu menu = new Menu("Title")
    .addItem("Item", () -> action())
    .setBorder(true)
    .setCenterOnScreen(true);
menu.focus();

// Label (default colors)
Label label = new Label(x, y, "Text");

// Label (custom colors)
Label label = new Label(x, y, "Text",
    TextColor.ANSI.CYAN, RenderManager.DEFAULT_BG);

// MenuItem (rarely created directly)
MenuItem item = new MenuItem(x, y, "Text", () -> action());
```

### State Lifecycle Template

```java
public class MyState implements LoopableState {
  private Menu menu;  // or List<Label>

  @Override
  public void start() {
    // Initialize components
    menu = new Menu("Title").addItem(...).setBorder(true);
    menu.focus();
  }

  @Override
  public void handleInput() {
    if (!renderReady() || menu == null) return;
    KeyStroke ks = InputManager.poll();
    if (ks != null) menu.handleInput(ks);
  }

  @Override
  public void render() {
    RenderManager.clear(RenderManager.UI_LAYER);
    if (menu != null) menu.render(RenderManager.UI_LAYER);
  }

  @Override
  public void end() {
    if (menu != null) {
      menu.unfocus();
      menu = null;
    }
  }

  @Override
  public void pause() {}

  @Override
  public void resume() {
    if (menu != null) {
      RenderManager.clearAll();
      menu.resetFocus();
      menu.focus();
    }
  }

  @Override
  public void update() {}
}
```

---

## Troubleshooting

### Menu not responding to input
- Check that `menu.focus()` was called
- Verify `menu.handleInput(ks)` is being called
- Ensure menu is not null

### Menu rendering in wrong position
- Check `setCenterOnScreen(true)` is set for auto-centering
- Verify screen dimensions are valid
- Check for custom position constructor usage

### Labels not visible
- Ensure `RenderManager.clear()` is called before rendering
- Check Z-layer is correct (UI_LAYER or higher)
- Verify label position is within screen bounds
- Check colors aren't matching background

### Focus issues when resuming states
- Always call `menu.resetFocus()` before `menu.focus()` in `resume()`
- Clear all layers with `RenderManager.clearAll()` in `resume()`

### Overlapping UI elements
- Use higher Z-layers for overlays (UI_LAYER + 1, +2, etc.)
- Clear appropriate layers before rendering
- Ensure proper state cleanup in `end()`

---

## Additional Resources

- **Architecture Documentation**: See `ARCHITECTURE.md` for overall system design
- **Developer Guide**: See `DEVELOPER_GUIDE.md` for development workflow
- **Source Code Examples**:
  - `MainMenuState.java` - Full menu implementation
  - `PauseState.java` - Simple pause menu
  - `GameplayState.java` - Label usage for instructions

---

*Last Updated: 2025*
*For Console Jack - Terminal-based Blackjack Game*
