# Console Jack - State Machine Guide

A comprehensive guide to the state management system powering Console Jack's game flow.

## Table of Contents

- [Overview](#overview)
- [Core Concepts](#core-concepts)
- [LoopableState Interface](#loopablestate-interface)
- [StateMachineManager API](#statemachinemanager-api)
- [State Lifecycle](#state-lifecycle)
- [Creating States](#creating-states)
- [State Transitions](#state-transitions)
- [Best Practices](#best-practices)
- [Real-World Examples](#real-world-examples)
- [Common Patterns](#common-patterns)
- [Troubleshooting](#troubleshooting)

---

## Overview

The **State Machine** is the foundational architecture pattern for Console Jack. Every screen, menu, and game mode is implemented as a **state** that manages its own logic, input handling, rendering, and lifecycle.

### Key Benefits

- **Organized Game Flow**: Each screen is encapsulated in its own state
- **Stack-Based Navigation**: Push/pop states for menus and overlays
- **Clean Transitions**: Automatic pause/resume when overlaying states
- **Resource Management**: Clear lifecycle hooks for setup and cleanup
- **Thread-Safe**: Lock-protected state transitions
- **Simple API**: Facade pattern via `StateMachineManager`

### Architecture

```
MasterSubsystem (Game Loop)
    ↓
StateMachineCoordinator (LIFO Stack)
    ↓
StateMachineManager (Public Facade)
    ↓
Game States (MainMenuState, GameplayState, etc.)
```

---

## Core Concepts

### LIFO Stack Architecture

The state machine uses a **Last-In-First-Out (LIFO) stack** to manage states:

```
┌─────────────────┐
│  PauseState     │ ← Top (Active)
├─────────────────┤
│  GameplayState  │ ← Paused
├─────────────────┤
│  MainMenuState  │ ← Paused
└─────────────────┘
```

- **Active State**: Only the top state receives `update()`, `render()`, and `handleInput()` calls
- **Paused States**: Lower states remain on the stack but are inactive
- **Push**: Adds a new state on top (pauses the previous active state)
- **Pop**: Removes the top state (resumes the next state)
- **Replace**: Replaces the active state with a new one

### State Lifecycle Phases

Every state goes through these phases:

1. **Creation**: State object instantiated
2. **Start**: `start()` called → Initialize resources
3. **Active**: Receives `update()`, `render()`, `handleInput()` calls
4. **Pause**: `pause()` called → Another state pushed on top
5. **Resume**: `resume()` called → Overlaying state popped
6. **End**: `end()` called → Cleanup resources
7. **Destruction**: Object eligible for garbage collection

---

## LoopableState Interface

All game states implement the `LoopableState` interface.

### Interface Definition

```java
package net.luxsolari.engine.states;

public interface LoopableState {
  void start();         // Initialize state (called once)
  void pause();         // State paused by overlay
  void resume();        // State resumed after overlay
  void handleInput();   // Process user input
  void update();        // Update game logic
  void render();        // Render visuals
  void end();           // Cleanup resources (called once)

  // Helper method available to all states
  default boolean renderReady() {
    return RenderSubsystem.INSTANCE.ready();
  }
}
```

### Method Responsibilities

#### `start()`
Called once when the state becomes active for the first time.

**Responsibilities**:
- Initialize UI components (menus, labels)
- Set input context via `InputManager.setContext()`
- Start background music via `AudioManager.playBGM()`
- Create entities in `EntityPool`
- Load resources
- Set up initial state variables

**Example**:
```java
@Override
public void start() {
  LOGGER.info("Gameplay started");

  // Set input context
  InputManager.setContext(new GameplayInputContext());

  // Start music
  AudioManager.playBGM("gameplay_theme", true);

  // Initialize UI
  instructionLabels = createInstructionLabels();

  // Create game entities
  createDeck();
}
```

#### `pause()`
Called when another state is pushed on top of this one.

**Responsibilities**:
- Stop background music (optional)
- Pause animations
- Save transient state if needed
- **DO NOT** destroy resources (state will resume)

**Example**:
```java
@Override
public void pause() {
  LOGGER.info("Gameplay paused");
  // Optional: stop music to avoid overlap
  // AudioManager.stopBGM();
}
```

#### `resume()`
Called when an overlaying state is popped and this state becomes active again.

**Responsibilities**:
- Re-set input context (important!)
- Resume background music
- Clear and redraw render layers
- Reset focus on UI components
- Resume animations

**Example**:
```java
@Override
public void resume() {
  LOGGER.info("Gameplay resumed");

  // Re-set input context (critical!)
  InputManager.setContext(new GameplayInputContext());

  // Resume music
  AudioManager.playBGM("gameplay_theme", true);

  // Clear and redraw
  RenderManager.clearAll();
  if (menu != null) {
    menu.resetFocus();
    menu.focus();
  }
}
```

#### `handleInput()`
Called every frame by the master game loop.

**Responsibilities**:
- Check if render system is ready
- Poll for input via `InputManager.pollCommand()`
- Handle input commands
- Delegate to UI components if needed

**Example**:
```java
@Override
public void handleInput() {
  if (!renderReady()) {
    return;
  }

  InputResult input = InputManager.pollCommand();
  if (input == null || input.command() == null) {
    return;
  }

  switch (input.command()) {
    case QUIT -> MasterSubsystem.INSTANCE.stop();
    case PAUSE -> StateMachineManager.push(new PauseState());
    case HIT -> handleHit();
    case STAND -> handleStand();
  }
}
```

#### `update()`
Called every frame (8 UPS) for game logic updates.

**Responsibilities**:
- Update game state (scores, timers, etc.)
- Process AI logic
- Update animations
- Check win/loss conditions

**Example**:
```java
@Override
public void update() {
  // Update timers
  if (turnTimer > 0) {
    turnTimer--;
  }

  // Check game over
  if (isGameOver()) {
    showGameOverScreen();
  }
}
```

#### `render()`
Called every frame for visual updates.

**Responsibilities**:
- Clear render layers
- Render UI components
- Render game entities
- **DO NOT** modify game state here

**Example**:
```java
@Override
public void render() {
  clearUILayers();

  if (!renderReady()) {
    return;
  }

  // Render UI
  if (menu != null) {
    menu.render(RenderManager.UI_LAYER);
  }

  // Render labels
  renderInstructionLabels();
}
```

#### `end()`
Called once when the state is being removed from the stack.

**Responsibilities**:
- Stop background music
- Unfocus UI components
- Clear entity pool entries
- Null out references for garbage collection
- Release any held resources

**Example**:
```java
@Override
public void end() {
  LOGGER.info("Gameplay ended");

  // Stop music
  AudioManager.stopBGM();

  // Cleanup UI
  if (menu != null) {
    menu.unfocus();
    menu = null;
  }

  // Cleanup labels
  if (instructionLabels != null) {
    instructionLabels.clear();
    instructionLabels = null;
  }

  // Clear entities
  entityPool.removeWith(Card.class);
}
```

---

## StateMachineManager API

The `StateMachineManager` is a stateless facade providing access to the state machine.

### Methods

#### `push(LoopableState state)`
Pushes a new state onto the stack, making it active.

**Behavior**:
1. Current active state (if any) receives `pause()`
2. New state is added to the top of the stack
3. All render layers are cleared
4. New state receives `start()`

**Use Cases**:
- Opening overlay menus (pause menu, dialogs)
- Transitioning to a new screen while keeping the previous one in memory

**Example**:
```java
// Open pause menu (overlay)
StateMachineManager.push(new PauseState());

// Open dialog (overlay)
StateMachineManager.push(new DialogState("Are you sure?"));
```

#### `pop()`
Removes the active state from the stack.

**Behavior**:
1. Active state receives `end()`
2. State is removed from the stack
3. All render layers are cleared
4. Next state (if any) receives `resume()`

**Use Cases**:
- Closing overlay menus
- Returning to the previous screen

**Example**:
```java
// Close pause menu
StateMachineManager.pop();

// Return to previous screen
StateMachineManager.pop();
```

#### `replace(LoopableState state)`
Replaces the active state with a new one. Equivalent to `pop()` + `push()`.

**Behavior**:
1. Current active state receives `end()`
2. State is removed from the stack
3. New state is added to the stack
4. All render layers are cleared
5. New state receives `start()`

**Use Cases**:
- Transitioning between main screens (main menu → gameplay)
- State transitions where you don't want to keep the previous state

**Example**:
```java
// Start game from main menu
StateMachineManager.replace(new GameplayState());

// Return to main menu from game over
StateMachineManager.replace(new MainMenuState());
```

#### `clear()`
Removes all states from the stack.

**Behavior**:
1. All states receive `end()` in LIFO order
2. Stack is emptied
3. All render layers are cleared

**Use Cases**:
- Resetting to a clean state
- Quitting to main menu from deep in the game

**Example**:
```java
// Quit to main menu from anywhere
StateMachineManager.clear();
StateMachineManager.push(new MainMenuState());
```

#### `active()`
Returns the currently active state.

**Returns**: `LoopableState` or `null` if stack is empty

**Example**:
```java
LoopableState current = StateMachineManager.active();
if (current instanceof GameplayState) {
  // Do something gameplay-specific
}
```

#### `hasStates()`
Checks if any states exist in the stack.

**Returns**: `true` if at least one state exists

**Example**:
```java
if (!StateMachineManager.hasStates()) {
  // Stack is empty, push initial state
  StateMachineManager.push(new MainMenuState());
}
```

---

## State Lifecycle

### Complete Lifecycle Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. State Created (new GameplayState())                      │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. start() called                                            │
│    - Initialize resources                                    │
│    - Set input context                                       │
│    - Start music                                             │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. Active Loop (every frame)                                │
│    - handleInput()                                           │
│    - update()                                                │
│    - render()                                                │
└───────────────────┬───────────────┬─────────────────────────┘
                    ↓               ↓
        ┌───────────────────┐   ┌──────────────────┐
        │ Another state     │   │ State popped     │
        │ pushed on top     │   │                  │
        └─────────┬─────────┘   └────────┬─────────┘
                  ↓                       ↓
        ┌───────────────────┐   ┌──────────────────┐
        │ 4a. pause()       │   │ 6. end()         │
        │     called        │   │    - Cleanup     │
        └─────────┬─────────┘   │    - Stop music  │
                  ↓              │    - Null refs   │
        ┌───────────────────┐   └────────┬─────────┘
        │ State Paused      │            ↓
        │ (on stack but     │   ┌──────────────────┐
        │  not active)      │   │ 7. State         │
        └─────────┬─────────┘   │    Destroyed     │
                  ↓              └──────────────────┘
        ┌───────────────────┐
        │ Overlay state     │
        │ popped            │
        └─────────┬─────────┘
                  ↓
        ┌───────────────────┐
        │ 5. resume()       │
        │    called         │
        │    - Reset        │
        │      context      │
        │    - Redraw       │
        └─────────┬─────────┘
                  ↓
        Back to Active Loop (step 3)
```

### State Transition Diagram

```
     push(StateB)           pop()
StateA ──────────→ StateB ──────────→ StateA
       pause()               resume()

     replace(StateC)
StateA ─────────────→ StateC
       end()          start()
```

---

## Creating States

### Minimal State Template

```java
package net.luxsolari.game.states;

import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.manager.*;
import net.luxsolari.engine.input.*;
import java.util.logging.Logger;

public class MyGameState implements LoopableState {

  private static final String TAG = MyGameState.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  @Override
  public void start() {
    LOGGER.info("MyGameState started");

    // Set input context
    InputManager.setContext(new MyGameInputContext());

    // Initialize resources
  }

  @Override
  public void pause() {
    LOGGER.info("MyGameState paused");
  }

  @Override
  public void resume() {
    LOGGER.info("MyGameState resumed");
    InputManager.setContext(new MyGameInputContext());
    RenderManager.clearAll();
  }

  @Override
  public void handleInput() {
    if (!renderReady()) {
      return;
    }

    InputResult input = InputManager.pollCommand();
    if (input == null || input.command() == null) {
      return;
    }

    // Handle input
  }

  @Override
  public void update() {
    // Update logic
  }

  @Override
  public void render() {
    if (!renderReady()) {
      return;
    }

    // Render visuals
  }

  @Override
  public void end() {
    LOGGER.info("MyGameState ended");
    // Cleanup
  }
}
```

### Menu-Based State Template

```java
public class MenuState implements LoopableState {

  private static final String TAG = MenuState.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private Menu menu;

  @Override
  public void start() {
    LOGGER.info("MenuState started");

    InputManager.setContext(new MenuInputContext());
    AudioManager.playBGM("menu_theme", true);

    menu = new Menu("My Menu")
        .addItem("Option 1", this::option1)
        .addItem("Option 2", this::option2)
        .addItem("Back", () -> StateMachineManager.pop())
        .setBorder(true);

    menu.focus();
  }

  @Override
  public void pause() {
    LOGGER.info("MenuState paused");
    AudioManager.stopBGM();
  }

  @Override
  public void resume() {
    LOGGER.info("MenuState resumed");
    InputManager.setContext(new MenuInputContext());
    AudioManager.playBGM("menu_theme", true);

    if (menu != null) {
      RenderManager.clearAll();
      menu.resetFocus();
      menu.focus();
    }
  }

  @Override
  public void handleInput() {
    if (!renderReady() || menu == null) {
      return;
    }

    InputResult input = InputManager.pollCommand();
    if (input == null || input.command() == null) {
      return;
    }

    // Handle state-level commands
    if (input.command() == InputCommand.QUIT) {
      MasterSubsystem.INSTANCE.stop();
      return;
    }

    // Delegate to menu
    menu.handleCommand(input.command());
  }

  @Override
  public void update() {}

  @Override
  public void render() {
    clearUILayers();

    if (!renderReady() || menu == null) {
      return;
    }

    menu.render(RenderManager.UI_LAYER);
  }

  @Override
  public void end() {
    LOGGER.info("MenuState ended");
    AudioManager.stopBGM();

    if (menu != null) {
      menu.unfocus();
      menu = null;
    }
  }

  private void clearUILayers() {
    for (int layer = RenderManager.UI_LAYER;
         layer < RenderManager.getLayerCount(); layer++) {
      RenderManager.clear(layer);
    }
  }

  private void option1() { /* implementation */ }
  private void option2() { /* implementation */ }
}
```

---

## State Transitions

### Pattern 1: Main Menu → Gameplay (Replace)

```java
// In MainMenuState
menu.addItem("Start Game", () -> {
  StateMachineManager.replace(new GameplayState());
});
```

**Why `replace()`**: We don't need to keep the main menu in memory during gameplay.

### Pattern 2: Gameplay → Pause Menu (Push)

```java
// In GameplayState handleInput()
case PAUSE -> StateMachineManager.push(new PauseState());
```

**Why `push()`**: We want to overlay the pause menu while keeping gameplay state intact.

### Pattern 3: Pause Menu → Resume Gameplay (Pop)

```java
// In PauseState
menu.addItem("Resume", () -> {
  StateMachineManager.pop();
});
```

**Why `pop()`**: Simply remove the pause menu to return to gameplay.

### Pattern 4: Pause Menu → Main Menu (Clear + Push)

```java
// In PauseState
menu.addItem("Quit to Main Menu", () -> {
  StateMachineManager.clear();
  StateMachineManager.push(new MainMenuState());
});
```

**Why `clear()` + `push()`**: Remove all states (pause + gameplay) and start fresh with main menu.

### Pattern 5: Temporary Dialog (Anonymous State)

```java
private void showDialog() {
  StateMachineManager.push(new LoopableState() {
    private Menu dialogMenu;

    @Override
    public void start() {
      dialogMenu = new Menu("Dialog")
          .addItem("OK", () -> StateMachineManager.pop())
          .setBorder(true);
      dialogMenu.focus();
    }

    @Override
    public void handleInput() {
      if (dialogMenu != null) {
        KeyStroke ks = InputManager.poll();
        if (ks != null) {
          dialogMenu.handleInput(ks);
        }
      }
    }

    @Override
    public void render() {
      if (dialogMenu != null) {
        RenderManager.clear(RenderManager.UI_LAYER + 1);
        dialogMenu.render(RenderManager.UI_LAYER + 1);
      }
    }

    @Override
    public void end() {
      if (dialogMenu != null) {
        dialogMenu.unfocus();
        dialogMenu = null;
      }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void update() {}
  });
}
```

---

## Best Practices

### 1. Always Re-Set Input Context in resume()

```java
@Override
public void resume() {
  InputManager.setContext(new GameplayInputContext());  // ✅ Critical!
}
```

**Why**: When states are pushed/popped, the input context can change. Always restore your context.

### 2. Clear Render Layers Appropriately

```java
@Override
public void render() {
  clearUILayers();  // ✅ Clear before rendering
  menu.render(RenderManager.UI_LAYER);
}
```

**Why**: Prevents visual artifacts from previous frames.

### 3. Clean Up Resources in end()

```java
@Override
public void end() {
  AudioManager.stopBGM();              // ✅ Stop audio

  if (menu != null) {
    menu.unfocus();                    // ✅ Unfocus UI
    menu = null;                       // ✅ Null reference
  }

  if (labels != null) {
    labels.clear();                    // ✅ Clear collections
    labels = null;
  }
}
```

**Why**: Proper cleanup prevents memory leaks and resource contention.

### 4. Check renderReady() Before Rendering

```java
@Override
public void handleInput() {
  if (!renderReady()) {               // ✅ Check render system
    return;
  }
  // ... poll input
}
```

**Why**: Multi-threaded startup requires checking if render subsystem is initialized.

### 5. Use Logging for State Transitions

```java
@Override
public void start() {
  LOGGER.info("GameplayState started");   // ✅ Log state changes
  // ...
}
```

**Why**: Makes debugging state transitions much easier.

### 6. Don't Modify Game State in render()

```java
@Override
public void render() {
  // ✅ Good: Just rendering
  menu.render(RenderManager.UI_LAYER);

  // ❌ Bad: Modifying state
  // score++;
  // entities.remove(0);
}
```

**Why**: Violates separation of concerns and can cause timing issues.

### 7. Handle Null States Gracefully

```java
@Override
public void render() {
  if (menu == null) {                 // ✅ Null check
    return;
  }
  menu.render(RenderManager.UI_LAYER);
}
```

**Why**: Prevents NPEs during state transitions.

### 8. Use Higher Layers for Overlays

```java
// Main state
menu.render(RenderManager.UI_LAYER);

// Dialog/overlay state
dialogMenu.render(RenderManager.UI_LAYER + 2);
```

**Why**: Ensures overlays render on top of the base state.

---

## Real-World Examples

### Example 1: MainMenuState

**File**: `src/main/java/net/luxsolari/game/states/MainMenuState.java`

```java
public class MainMenuState implements LoopableState {

  private static final String TAG = MainMenuState.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private Menu mainMenu;

  @Override
  public void start() {
    LOGGER.info("Main menu started");
    AudioManager.playBGM("menu_theme", true);
    InputManager.setContext(new MainMenuInputContext());

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
  public void pause() {
    LOGGER.info("Main menu paused");
    AudioManager.stopBGM();
  }

  @Override
  public void resume() {
    LOGGER.info("Main menu resumed");
    AudioManager.playBGM("menu_theme", true);
    InputManager.setContext(new MainMenuInputContext());

    if (mainMenu != null) {
      RenderManager.clearAll();
      mainMenu.resetFocus();
      mainMenu.focus();
    }
  }

  @Override
  public void handleInput() {
    if (!renderReady() || mainMenu == null) {
      return;
    }

    InputResult input = InputManager.pollCommand();
    if (input == null || input.command() == null) {
      return;
    }

    if (input.command() == InputCommand.QUIT) {
      MasterSubsystem.INSTANCE.stop();
      return;
    }

    mainMenu.handleCommand(input.command());
  }

  @Override
  public void update() {}

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
    LOGGER.info("Main menu ended");
    AudioManager.stopBGM();

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
    // Implementation omitted for brevity
  }
}
```

### Example 2: PauseState

**File**: `src/main/java/net/luxsolari/game/states/PauseState.java`

```java
public class PauseState implements LoopableState {

  private static final String TAG = PauseState.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private Menu pauseMenu;

  @Override
  public void start() {
    LOGGER.info("Pause menu opened");
    InputManager.setContext(new PauseInputContext());

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
  public void pause() {
    LOGGER.info("Pause menu paused");
  }

  @Override
  public void resume() {
    LOGGER.info("Pause menu resumed");
    InputManager.setContext(new PauseInputContext());

    if (pauseMenu != null) {
      RenderManager.clearAll();
      pauseMenu.resetFocus();
      pauseMenu.focus();
    }
  }

  @Override
  public void handleInput() {
    if (!renderReady() || pauseMenu == null) {
      return;
    }

    InputResult input = InputManager.pollCommand();
    if (input == null || input.command() == null) {
      return;
    }

    switch (input.command()) {
      case QUIT -> MasterSubsystem.INSTANCE.stop();
      case RESUME -> StateMachineManager.pop();
      case BACK -> {
        StateMachineManager.clear();
        StateMachineManager.push(new MainMenuState());
      }
    }

    pauseMenu.handleCommand(input.command());
  }

  @Override
  public void update() {}

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
    LOGGER.info("Pause menu closed");

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

---

## Common Patterns

### Pattern 1: Simple Screen Transition

```java
// Main menu → Gameplay
StateMachineManager.replace(new GameplayState());
```

### Pattern 2: Overlay Menu

```java
// Open pause menu (keeps gameplay in memory)
StateMachineManager.push(new PauseState());

// Close pause menu
StateMachineManager.pop();
```

### Pattern 3: Reset to Main Menu

```java
// From anywhere in the game
StateMachineManager.clear();
StateMachineManager.push(new MainMenuState());
```

### Pattern 4: Confirmation Dialog

```java
private void confirmQuit() {
  Menu confirmMenu = new Menu("Confirm")
      .addItem("Yes", () -> {
        StateMachineManager.clear();
        StateMachineManager.push(new MainMenuState());
      })
      .addItem("No", () -> StateMachineManager.pop())
      .setBorder(true);

  // Push temporary state
  StateMachineManager.push(new LoopableState() {
    @Override public void start() { confirmMenu.focus(); }
    @Override public void handleInput() { /* handle */ }
    @Override public void render() { confirmMenu.render(RenderManager.UI_LAYER + 1); }
    @Override public void end() { confirmMenu.unfocus(); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void update() {}
  });
}
```

### Pattern 5: Splash Screen with Auto-Transition

```java
public class SplashState implements LoopableState {
  private int frameCount = 0;
  private static final int SPLASH_DURATION = 120; // frames

  @Override
  public void start() {
    // Show splash screen
  }

  @Override
  public void update() {
    frameCount++;
    if (frameCount >= SPLASH_DURATION) {
      StateMachineManager.replace(new MainMenuState());
    }
  }

  @Override
  public void render() {
    // Render splash
  }

  // ... other methods
}
```

---

## Troubleshooting

### State Not Receiving Input

**Symptom**: Input not working in state

**Solutions**:
1. Check input context is set in both `start()` and `resume()`
2. Verify state is actually active: `StateMachineManager.active()`
3. Check `renderReady()` returns true
4. Ensure no exception in `handleInput()` early-returns

### Visual Artifacts Between States

**Symptom**: Previous state's visuals remain visible

**Solutions**:
1. Call `RenderManager.clearAll()` in `resume()`
2. Implement `clearUILayers()` helper and call in `render()`
3. Clear specific layers before rendering

### Memory Leaks

**Symptom**: Memory usage grows over time

**Solutions**:
1. Null all object references in `end()`
2. Unfocus UI components before nulling
3. Clear collections (`labels.clear()`)
4. Remove entities from EntityPool

### Input Context Lost After Resume

**Symptom**: Keys don't work after returning from overlay

**Solution**: Always re-set context in `resume()`:
```java
@Override
public void resume() {
  InputManager.setContext(new MyInputContext());  // ✅
}
```

### State Stack Confusion

**Symptom**: Unexpected state transitions

**Solution**: Add logging to track stack changes:
```java
@Override
public void start() {
  LOGGER.info(this.getClass().getSimpleName() + " started");
}

@Override
public void end() {
  LOGGER.info(this.getClass().getSimpleName() + " ended");
}
```

### Race Conditions During Startup

**Symptom**: NPEs or missing visuals on startup

**Solution**: Always check `renderReady()`:
```java
@Override
public void handleInput() {
  if (!renderReady()) {
    return;
  }
  // ... safe to proceed
}
```

---

## Additional Resources

- **Architecture Documentation**: See `ARCHITECTURE.md` for overall system design
- **Input System Guide**: See `INPUT_SYSTEM_GUIDE.md` for input handling
- **UI Components Guide**: See `UI_COMPONENTS_GUIDE.md` for UI components
- **Developer Guide**: See `DEVELOPER_GUIDE.md` for development workflow

**Source Code References**:
- State machine implementation: `src/main/java/net/luxsolari/engine/systems/internal/StateMachineCoordinator.java`
- State manager facade: `src/main/java/net/luxsolari/engine/manager/StateMachineManager.java`
- State interface: `src/main/java/net/luxsolari/engine/states/LoopableState.java`
- Example states: `src/main/java/net/luxsolari/game/states/`

---

*Last Updated: 2025*
*For Console Jack - Terminal-based Blackjack Game*
