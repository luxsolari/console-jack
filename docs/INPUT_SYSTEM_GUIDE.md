# Console Jack - Input System Guide

A comprehensive guide to the command-based input system with context-sensitive key bindings.

## Table of Contents

- [Overview](#overview)
- [Core Concepts](#core-concepts)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Input Components](#input-components)
  - [InputCommand](#inputcommand)
  - [KeyBinding](#keybinding)
  - [InputContext](#inputcontext)
  - [InputResult](#inputresult)
  - [InputManager](#inputmanager)
- [Creating Input Contexts](#creating-input-contexts)
- [Using Input in Game States](#using-input-in-game-states)
- [Best Practices](#best-practices)
- [Real-World Examples](#real-world-examples)
- [Troubleshooting](#troubleshooting)

---

## Overview

The Console Jack input system provides a **command-based architecture** that decouples raw keyboard input from game logic. Instead of directly checking for specific keys, game states work with high-level commands (like `PAUSE`, `CONFIRM`, `HIT`) that can be bound to different keys in different contexts.

### Key Benefits

- **Context-Sensitive**: Different game states can map the same key to different commands
- **Flexible Remapping**: Easy to change key bindings without modifying game logic
- **Clean Separation**: Game logic works with commands, not raw KeyStrokes
- **Maintainable**: All bindings for a state are defined in one place
- **Type-Safe**: Enum-based commands prevent typos and enable IDE autocomplete

---

## Core Concepts

### Commands vs KeyStrokes

**KeyStroke** (Low-level): Raw keyboard input from Lanterna
```java
KeyStroke ks = InputManager.poll();
if (ks.getKeyType() == KeyType.Enter) { ... }  // ❌ Tightly coupled to specific keys
```

**InputCommand** (High-level): Semantic game actions
```java
InputResult input = InputManager.pollCommand();
if (input.command() == InputCommand.CONFIRM) { ... }  // ✅ Decoupled from specific keys
```

### Input Contexts

Each game state has its own `InputContext` that defines how keys map to commands. The same key can mean different things in different states:

- Main Menu: `Q` → `QUIT` (exit application)
- Gameplay: `Q` → `PAUSE` (pause game)
- Pause Menu: `Q` → `BACK` (return to main menu)

### Input Flow

```
User presses key
    ↓
InputSubsystem (polls Lanterna)
    ↓
InputManager.pollCommand()
    ↓
Current InputContext (resolves KeyBinding → InputCommand)
    ↓
InputResult (contains both KeyStroke and Command)
    ↓
Game State (handles command)
```

---

## Architecture

### Package Structure

```
net.luxsolari.engine.input/
├── InputCommand.java        # Enum of all game commands
├── KeyBinding.java          # Immutable key + modifiers record
├── InputContext.java        # Interface for key-to-command mapping
└── InputResult.java         # Wrapper for keystroke + command

net.luxsolari.engine.manager/
└── InputManager.java        # Public facade for input system

net.luxsolari.game.input/
├── MainMenuInputContext.java      # Main menu bindings
├── GameplayInputContext.java      # Gameplay bindings
└── PauseInputContext.java         # Pause menu bindings
```

### Threading Model

The input system runs on a dedicated thread managed by `InputSubsystem`:
- **Input Thread**: Polls Lanterna for keystrokes, queues them in thread-safe deque
- **Master Thread**: Game states poll for input via `InputManager` during their `handleInput()` method

---

## Quick Start

### 1. Set Context in State

```java
public class MyGameState implements LoopableState {
  @Override
  public void start() {
    // Set the input context for this state
    InputManager.setContext(new MyGameInputContext());
  }

  @Override
  public void resume() {
    // Re-set context when resuming from overlay state
    InputManager.setContext(new MyGameInputContext());
  }
}
```

### 2. Poll for Commands

```java
@Override
public void handleInput() {
  if (!renderReady()) {
    return;
  }

  InputResult input = InputManager.pollCommand();
  if (input == null || !input.hasCommand()) {
    return;
  }

  // Handle commands
  switch (input.command()) {
    case PAUSE -> StateMachineManager.push(new PauseState());
    case QUIT -> MasterSubsystem.INSTANCE.stop();
    case CONFIRM -> selectMenuItem();
    default -> {}
  }
}
```

### 3. Create Your InputContext

```java
public class MyGameInputContext implements InputContext {
  private static final Map<KeyBinding, InputCommand> BINDINGS = Map.ofEntries(
      Map.entry(KeyBinding.of('P'), InputCommand.PAUSE),
      Map.entry(KeyBinding.of(KeyType.Escape), InputCommand.PAUSE),
      Map.entry(KeyBinding.of(KeyType.Enter), InputCommand.CONFIRM)
  );

  @Override
  public Map<KeyBinding, InputCommand> getBindings() {
    return BINDINGS;
  }

  @Override
  public String getName() {
    return "MyGame";
  }
}
```

---

## Input Components

### InputCommand

**Location**: `net.luxsolari.engine.input.InputCommand`

Enum defining all input commands recognized by the game. Commands are context-independent and semantic.

#### Available Commands

```java
// Global Commands
QUIT           // Quit application
BACK           // Go back/cancel
CONFIRM        // Confirm selection
CANCEL         // Cancel action

// Navigation Commands
NAVIGATE_UP
NAVIGATE_DOWN
NAVIGATE_LEFT
NAVIGATE_RIGHT
NAVIGATE_FIRST
NAVIGATE_LAST

// Game State Commands
PAUSE
RESUME

// Blackjack Commands
HIT
STAND
DOUBLE_DOWN
SPLIT
SURRENDER

// Debug Commands
DEBUG_CREATE_CARD
DEBUG_CLEAR_CARDS
DEBUG_TOGGLE

// Audio Commands
TOGGLE_SOUND
VOLUME_UP
VOLUME_DOWN

// UI Commands
TOGGLE_FULLSCREEN
```

#### Adding New Commands

1. Add to `InputCommand` enum:
```java
public enum InputCommand {
  // ... existing commands
  NEW_FEATURE,  // Your new command
}
```

2. Bind in relevant `InputContext` implementations
3. Handle in game state's `handleInput()` method

---

### KeyBinding

**Location**: `net.luxsolari.engine.input.KeyBinding`

Immutable record representing a key combination with modifiers. Used as map keys for resolving keystrokes to commands.

#### Structure

```java
public record KeyBinding(
    KeyType keyType,
    Character character,
    boolean ctrlPressed,
    boolean altPressed,
    boolean shiftPressed
)
```

#### Factory Methods

```java
// Character key without modifiers
KeyBinding.of('A')                        // 'A' key
KeyBinding.of('q')                        // 'Q' key (normalized to uppercase)

// Character key with modifiers
KeyBinding.of('Q', true, false, false)    // Ctrl+Q
KeyBinding.of('F', false, true, false)    // Alt+F
KeyBinding.of('S', true, true, false)     // Ctrl+Alt+S

// Special key without modifiers
KeyBinding.of(KeyType.Enter)
KeyBinding.of(KeyType.Escape)
KeyBinding.of(KeyType.ArrowUp)
KeyBinding.of(KeyType.Home)

// Special key with modifiers
KeyBinding.of(KeyType.F1, true, false, false)  // Ctrl+F1

// From Lanterna KeyStroke
KeyStroke ks = new KeyStroke(KeyType.Enter);
KeyBinding.fromKeyStroke(ks)
```

#### Character Normalization

Character keys are automatically normalized to uppercase for consistency:
```java
KeyBinding.of('a').equals(KeyBinding.of('A'))  // true
```

#### String Representation

```java
KeyBinding.of('Q').toString()                        // "Q"
KeyBinding.of('Q', true, false, false).toString()    // "Ctrl+Q"
KeyBinding.of(KeyType.Enter).toString()              // "Enter"
KeyBinding.of('F', false, true, false).toString()    // "Alt+F"
```

---

### InputContext

**Location**: `net.luxsolari.engine.input.InputContext`

Interface defining context-specific key bindings for a game state. Each state provides its own implementation.

#### Interface

```java
public interface InputContext {
  /**
   * Returns the key-to-command mapping for this context.
   */
  Map<KeyBinding, InputCommand> getBindings();

  /**
   * Resolves a key binding to its command in this context.
   */
  default InputCommand resolve(KeyBinding binding) {
    return getBindings().get(binding);
  }

  /**
   * Returns the name of this context for debugging.
   */
  default String getName() {
    return this.getClass().getSimpleName();
  }
}
```

#### Implementation Pattern

```java
public class ExampleInputContext implements InputContext {

  // Immutable map of bindings (created once)
  private static final Map<KeyBinding, InputCommand> BINDINGS = Map.ofEntries(
      Map.entry(KeyBinding.of('P'), InputCommand.PAUSE),
      Map.entry(KeyBinding.of(KeyType.Escape), InputCommand.BACK),
      Map.entry(KeyBinding.of(KeyType.Enter), InputCommand.CONFIRM)
  );

  @Override
  public Map<KeyBinding, InputCommand> getBindings() {
    return BINDINGS;
  }

  @Override
  public String getName() {
    return "Example";  // Optional: custom name for debugging
  }
}
```

---

### InputResult

**Location**: `net.luxsolari.engine.input.InputResult`

Wrapper record containing both the raw keystroke and resolved command. Returned by `InputManager.pollCommand()`.

#### Structure

```java
public record InputResult(KeyStroke keyStroke, InputCommand command) {
  public boolean hasCommand();    // true if command is not null
  public boolean hasKeyStroke();  // true if keyStroke is not null
}
```

#### Usage

```java
InputResult input = InputManager.pollCommand();
if (input == null) {
  return;  // No input available
}

// Check if a command was resolved
if (input.hasCommand()) {
  switch (input.command()) {
    case PAUSE -> pauseGame();
    case QUIT -> quitGame();
  }
}

// Access raw keystroke if needed
if (input.hasKeyStroke()) {
  KeyType keyType = input.keyStroke().getKeyType();
  // ... low-level handling
}
```

---

### InputManager

**Location**: `net.luxsolari.engine.manager.InputManager`

Public facade providing command-based input handling. Game states interact exclusively with this manager.

#### API

```java
// Context Management
InputManager.setContext(InputContext context)   // Set current input context
InputManager.getContext()                       // Get current context

// Input Polling
InputManager.pollCommand()                      // High-level: returns InputResult
InputManager.poll()                             // Low-level: returns raw KeyStroke

// Status
InputManager.ready()                            // Check if input system is ready
```

#### Primary Method: pollCommand()

```java
InputResult input = InputManager.pollCommand();
```

**Returns**: `InputResult` containing:
- `keyStroke`: Raw Lanterna KeyStroke
- `command`: Resolved InputCommand (may be null if key has no binding)

**Returns null**: If no input is available

---

## Creating Input Contexts

### Step 1: Determine Required Commands

List all commands your state needs to handle:
```
- Navigation (up/down/first/last)
- Confirm selection
- Pause game
- Quit application
```

### Step 2: Choose Key Bindings

Map each command to one or more keys:
```
NAVIGATE_UP    → Arrow Up
NAVIGATE_DOWN  → Arrow Down
NAVIGATE_FIRST → Home
NAVIGATE_LAST  → End
CONFIRM        → Enter
PAUSE          → P, Escape
QUIT           → Q, Ctrl+Q, EOF (Ctrl+D)
```

### Step 3: Implement InputContext

```java
package net.luxsolari.game.input;

import com.googlecode.lanterna.input.KeyType;
import java.util.Map;
import net.luxsolari.engine.input.InputCommand;
import net.luxsolari.engine.input.InputContext;
import net.luxsolari.engine.input.KeyBinding;

public class MyStateInputContext implements InputContext {

  private static final Map<KeyBinding, InputCommand> BINDINGS = Map.ofEntries(
      // Navigation
      Map.entry(KeyBinding.of(KeyType.ArrowUp), InputCommand.NAVIGATE_UP),
      Map.entry(KeyBinding.of(KeyType.ArrowDown), InputCommand.NAVIGATE_DOWN),
      Map.entry(KeyBinding.of(KeyType.Home), InputCommand.NAVIGATE_FIRST),
      Map.entry(KeyBinding.of(KeyType.End), InputCommand.NAVIGATE_LAST),

      // Actions
      Map.entry(KeyBinding.of(KeyType.Enter), InputCommand.CONFIRM),

      // Control
      Map.entry(KeyBinding.of('P'), InputCommand.PAUSE),
      Map.entry(KeyBinding.of(KeyType.Escape), InputCommand.PAUSE),

      // Quit (multiple bindings for same command)
      Map.entry(KeyBinding.of('Q'), InputCommand.QUIT),
      Map.entry(KeyBinding.of('Q', true, false, false), InputCommand.QUIT),  // Ctrl+Q
      Map.entry(KeyBinding.of(KeyType.EOF), InputCommand.QUIT)               // Ctrl+D
  );

  @Override
  public Map<KeyBinding, InputCommand> getBindings() {
    return BINDINGS;
  }

  @Override
  public String getName() {
    return "MyState";
  }
}
```

### Step 4: Use in Game State

```java
public class MyState implements LoopableState {
  @Override
  public void start() {
    InputManager.setContext(new MyStateInputContext());
  }

  @Override
  public void resume() {
    InputManager.setContext(new MyStateInputContext());
  }

  @Override
  public void handleInput() {
    InputResult input = InputManager.pollCommand();
    if (input == null || !input.hasCommand()) {
      return;
    }

    switch (input.command()) {
      case NAVIGATE_UP -> navigateUp();
      case NAVIGATE_DOWN -> navigateDown();
      case CONFIRM -> confirm();
      case PAUSE -> StateMachineManager.push(new PauseState());
      case QUIT -> MasterSubsystem.INSTANCE.stop();
      default -> {}
    }
  }
}
```

---

## Using Input in Game States

### State Lifecycle Integration

```java
public class GameplayState implements LoopableState {

  @Override
  public void start() {
    // Set context when state starts
    InputManager.setContext(new GameplayInputContext());
  }

  @Override
  public void pause() {
    // Context remains set, but state won't receive input
  }

  @Override
  public void resume() {
    // Re-set context when resuming
    InputManager.setContext(new GameplayInputContext());
  }

  @Override
  public void handleInput() {
    // Check render system is ready
    if (!renderReady()) {
      return;
    }

    // Poll for input
    InputResult input = InputManager.pollCommand();
    if (input == null) {
      return;  // No input available
    }

    // Handle unbound keys if needed
    if (!input.hasCommand()) {
      // Raw keystroke available but no command binding
      // Usually just ignore
      return;
    }

    // Handle commands
    switch (input.command()) {
      case QUIT -> MasterSubsystem.INSTANCE.stop();
      case PAUSE -> StateMachineManager.push(new PauseState());
      case HIT -> handleHit();
      case STAND -> handleStand();
      default -> {}
    }
  }

  @Override
  public void end() {
    // Optional: clear context
    InputManager.setContext(null);
  }
}
```

### Handling Input Results

#### Pattern 1: Command-Only Handling

```java
InputResult input = InputManager.pollCommand();
if (input == null || !input.hasCommand()) {
  return;
}

switch (input.command()) {
  case PAUSE -> pauseGame();
  case QUIT -> quitGame();
}
```

#### Pattern 2: Hybrid Handling (Command + Raw)

```java
InputResult input = InputManager.pollCommand();
if (input == null) {
  return;
}

// Handle bound commands
if (input.hasCommand()) {
  switch (input.command()) {
    case PAUSE -> pauseGame();
    case QUIT -> quitGame();
  }
  return;
}

// Handle unbound keys (raw keystroke)
if (input.hasKeyStroke()) {
  KeyType keyType = input.keyStroke().getKeyType();
  if (keyType == KeyType.Character) {
    // Handle typed characters for text input, etc.
  }
}
```

#### Pattern 3: Fallback to UI Components

```java
InputResult input = InputManager.pollCommand();
if (input == null) {
  return;
}

// Try command handling first
if (input.hasCommand()) {
  boolean handled = handleCommand(input.command());
  if (handled) {
    return;
  }
}

// Fall back to raw keystroke for UI components
if (input.hasKeyStroke() && menu != null) {
  menu.handleInput(input.keyStroke());
}
```

---

## Best Practices

### 1. Always Set Context in start() and resume()

```java
@Override
public void start() {
  InputManager.setContext(new MyInputContext());  // ✅ Set on start
}

@Override
public void resume() {
  InputManager.setContext(new MyInputContext());  // ✅ Re-set on resume
}
```

**Why**: When states are pushed/popped, the previous state needs to restore its context.

### 2. Check for Null Before Using Input

```java
InputResult input = InputManager.pollCommand();
if (input == null) {            // ✅ Check for null
  return;
}
if (!input.hasCommand()) {      // ✅ Check for command
  return;
}
// Now safe to use input.command()
```

### 3. Use Static Final Maps for Bindings

```java
private static final Map<KeyBinding, InputCommand> BINDINGS = Map.ofEntries(...);  // ✅
// NOT: private Map<KeyBinding, InputCommand> bindings = new HashMap<>();  // ❌
```

**Why**: Immutable, created once, thread-safe, memory efficient.

### 4. Provide Multiple Bindings for Important Actions

```java
// Quit command bound to multiple keys
Map.entry(KeyBinding.of('Q'), InputCommand.QUIT),
Map.entry(KeyBinding.of('Q', true, false, false), InputCommand.QUIT),  // Ctrl+Q
Map.entry(KeyBinding.of(KeyType.EOF), InputCommand.QUIT)               // Ctrl+D
```

**Why**: Users have different preferences, increases accessibility.

### 5. Use Semantic Command Names

```java
// Good: Semantic, describes intent
InputCommand.HIT
InputCommand.STAND
InputCommand.CONFIRM

// Bad: Implementation-focused
InputCommand.PRESS_H
InputCommand.ENTER_KEY
```

### 6. Handle QUIT and EOF Consistently

```java
// Always handle EOF (Ctrl+D on Unix, Ctrl+Z on Windows)
Map.entry(KeyBinding.of(KeyType.EOF), InputCommand.QUIT),

// In handleInput():
switch (input.command()) {
  case QUIT -> MasterSubsystem.INSTANCE.stop();  // Proper shutdown
}
```

### 7. Document Key Bindings for Users

```java
// In UI or help screen
instructionLabels.add(new Label(0, 0, "Press P or Esc to pause"));
instructionLabels.add(new Label(0, 0, "Press H to Hit, S to Stand"));
instructionLabels.add(new Label(0, 0, "Press Q or Ctrl+Q to quit"));
```

### 8. One Context Per State Class

```java
// Good: Clear ownership
public class MainMenuState implements LoopableState {
  InputManager.setContext(new MainMenuInputContext());
}

public class GameplayState implements LoopableState {
  InputManager.setContext(new GameplayInputContext());
}
```

---

## Real-World Examples

### Example 1: Main Menu Input Context

**File**: `src/main/java/net/luxsolari/game/input/MainMenuInputContext.java`

```java
public class MainMenuInputContext implements InputContext {

  private static final Map<KeyBinding, InputCommand> BINDINGS = Map.ofEntries(
      // Navigation
      Map.entry(KeyBinding.of(KeyType.ArrowUp), InputCommand.NAVIGATE_UP),
      Map.entry(KeyBinding.of(KeyType.ArrowDown), InputCommand.NAVIGATE_DOWN),
      Map.entry(KeyBinding.of(KeyType.Home), InputCommand.NAVIGATE_FIRST),
      Map.entry(KeyBinding.of(KeyType.End), InputCommand.NAVIGATE_LAST),

      // Actions
      Map.entry(KeyBinding.fromKeyStroke(new KeyStroke(KeyType.Enter)), InputCommand.CONFIRM),
      Map.entry(KeyBinding.of(KeyType.EOF), InputCommand.QUIT),

      // Shortcuts
      Map.entry(KeyBinding.of('Q', true, false, false), InputCommand.QUIT),  // Ctrl+Q
      Map.entry(KeyBinding.of('Q'), InputCommand.QUIT)  // Q to quit
  );

  @Override
  public Map<KeyBinding, InputCommand> getBindings() {
    return BINDINGS;
  }

  @Override
  public String getName() {
    return "MainMenu";
  }
}
```

### Example 2: Gameplay Input Context

**File**: `src/main/java/net/luxsolari/game/input/GameplayInputContext.java`

```java
public class GameplayInputContext implements InputContext {

  private static final Map<KeyBinding, InputCommand> BINDINGS = Map.ofEntries(
      // Game control
      Map.entry(KeyBinding.of('P'), InputCommand.PAUSE),
      Map.entry(KeyBinding.of('Q'), InputCommand.PAUSE),
      Map.entry(KeyBinding.of(KeyType.Escape), InputCommand.PAUSE),
      Map.entry(KeyBinding.of(KeyType.EOF), InputCommand.QUIT),
      Map.entry(KeyBinding.fromKeyStroke(new KeyStroke(KeyType.Enter)), InputCommand.CONFIRM),

      // Blackjack actions
      Map.entry(KeyBinding.of('H'), InputCommand.HIT),
      Map.entry(KeyBinding.of('S'), InputCommand.STAND),
      Map.entry(KeyBinding.of('D'), InputCommand.DOUBLE_DOWN),
      Map.entry(KeyBinding.of('X'), InputCommand.SPLIT),
      Map.entry(KeyBinding.of('R'), InputCommand.SURRENDER),
      Map.entry(KeyBinding.of(' '), InputCommand.HIT),  // Space = Hit

      // Debug commands
      Map.entry(KeyBinding.of('1'), InputCommand.DEBUG_CREATE_CARD),
      Map.entry(KeyBinding.of('2'), InputCommand.DEBUG_CLEAR_CARDS),
      Map.entry(KeyBinding.of('`'), InputCommand.DEBUG_TOGGLE),

      // Audio controls
      Map.entry(KeyBinding.of('M'), InputCommand.TOGGLE_SOUND),
      Map.entry(KeyBinding.of('+'), InputCommand.VOLUME_UP),
      Map.entry(KeyBinding.of('-'), InputCommand.VOLUME_DOWN),
      Map.entry(KeyBinding.of('='), InputCommand.VOLUME_UP),  // = key without shift

      // Fullscreen
      Map.entry(KeyBinding.of('F', false, true, false), InputCommand.TOGGLE_FULLSCREEN)  // Alt+F
  );

  @Override
  public Map<KeyBinding, InputCommand> getBindings() {
    return BINDINGS;
  }

  @Override
  public String getName() {
    return "Gameplay";
  }
}
```

### Example 3: Gameplay State Using Input Context

**File**: `src/main/java/net/luxsolari/game/states/GameplayState.java` (excerpt)

```java
public class GameplayState implements LoopableState {

  @Override
  public void start() {
    LOGGER.info("Gameplay started");

    // Set input context
    InputManager.setContext(new GameplayInputContext());

    // ... other initialization
  }

  @Override
  public void resume() {
    LOGGER.info("Gameplay resumed");

    // Re-set input context
    InputManager.setContext(new GameplayInputContext());
  }

  @Override
  public void handleInput() {
    if (!renderReady()) {
      return;
    }

    // Poll for command-based input
    InputResult input = InputManager.pollCommand();
    if (input == null || input.command() == null) {
      return;
    }

    // Handle commands
    switch (input.command()) {
      case QUIT -> MasterSubsystem.INSTANCE.stop();
      case PAUSE -> StateMachineManager.push(new PauseState());
      case DEBUG_CREATE_CARD -> createRandomCardEntity();
      case DEBUG_CLEAR_CARDS -> clearCards();

      // Future blackjack commands
      case HIT -> LOGGER.info("Hit command (not yet implemented)");
      case STAND -> LOGGER.info("Stand command (not yet implemented)");
      case DOUBLE_DOWN -> LOGGER.info("Double Down command (not yet implemented)");
      case SPLIT -> LOGGER.info("Split command (not yet implemented)");
      case SURRENDER -> LOGGER.info("Surrender command (not yet implemented)");

      default -> {}
    }
  }
}
```

### Example 4: Pause Menu Input Context

**File**: `src/main/java/net/luxsolari/game/input/PauseInputContext.java`

```java
public class PauseInputContext implements InputContext {

  private static final Map<KeyBinding, InputCommand> BINDINGS = Map.ofEntries(
      // Navigation
      Map.entry(KeyBinding.of(KeyType.ArrowUp), InputCommand.NAVIGATE_UP),
      Map.entry(KeyBinding.of(KeyType.ArrowDown), InputCommand.NAVIGATE_DOWN),
      Map.entry(KeyBinding.of(KeyType.Home), InputCommand.NAVIGATE_FIRST),
      Map.entry(KeyBinding.of(KeyType.End), InputCommand.NAVIGATE_LAST),

      // Actions
      Map.entry(KeyBinding.fromKeyStroke(new KeyStroke(KeyType.Enter)), InputCommand.CONFIRM),
      Map.entry(KeyBinding.of(KeyType.Escape), InputCommand.RESUME),
      Map.entry(KeyBinding.of(KeyType.EOF), InputCommand.QUIT),

      // Quick shortcuts
      Map.entry(KeyBinding.of('P'), InputCommand.RESUME),
      Map.entry(KeyBinding.of('R'), InputCommand.RESUME),
      Map.entry(KeyBinding.of('Q'), InputCommand.BACK)  // Quit to main menu
  );

  @Override
  public Map<KeyBinding, InputCommand> getBindings() {
    return BINDINGS;
  }

  @Override
  public String getName() {
    return "Pause";
  }
}
```

---

## Troubleshooting

### Input Not Responding

**Symptom**: Keys don't trigger any actions

**Solutions**:
1. Check that context is set:
```java
InputContext ctx = InputManager.getContext();
if (ctx == null) {
  // ❌ No context set!
  InputManager.setContext(new MyInputContext());
}
```

2. Verify bindings exist:
```java
InputResult input = InputManager.pollCommand();
if (input != null && !input.hasCommand()) {
  // Key pressed but no binding defined
  LOGGER.info("Unbound key: " + input.keyStroke());
}
```

3. Check render system is ready:
```java
if (!renderReady()) {
  return;  // Don't poll input if render not ready
}
```

### Commands Not Resolving

**Symptom**: `input.command()` is always null

**Solutions**:
1. Ensure KeyBinding matches exactly:
```java
// Character keys are normalized to uppercase
KeyBinding.of('a')  // Normalized to 'A'
KeyBinding.of('A')  // Already 'A'

// Both resolve to same binding ✅
```

2. Check modifier flags match:
```java
// These are DIFFERENT bindings:
KeyBinding.of('Q')                        // Q without modifiers
KeyBinding.of('Q', true, false, false)    // Ctrl+Q

// Bind both if you want both to work
```

### Context Not Persisting

**Symptom**: Context lost when resuming from overlay state

**Solution**: Always re-set context in `resume()`:
```java
@Override
public void resume() {
  InputManager.setContext(new MyInputContext());  // ✅ Re-set
}
```

### Same Key, Multiple States

**Symptom**: Key behavior doesn't change between states

**Solution**: Each state must set its own context:
```java
// MainMenuState
public void start() {
  InputManager.setContext(new MainMenuInputContext());  // Q = QUIT
}

// GameplayState
public void start() {
  InputManager.setContext(new GameplayInputContext());  // Q = PAUSE
}
```

### Missing Modifier Detection

**Symptom**: Ctrl/Alt/Shift modifiers not detected

**Solution**: Use correct KeyBinding factory method:
```java
// ✅ Correct
KeyBinding.of('Q', true, false, false)  // Ctrl+Q

// ❌ Wrong - ignores Ctrl
KeyBinding.of('Q')
```

---

## Advanced Topics

### Dynamic Context Switching

Some states may need to switch contexts dynamically:

```java
public class DialogState implements LoopableState {
  private boolean inTextMode = false;

  @Override
  public void handleInput() {
    if (inTextMode) {
      InputManager.setContext(new TextInputContext());
    } else {
      InputManager.setContext(new DialogNavigationContext());
    }

    // ... handle input
  }
}
```

### Hierarchical Contexts

Create base contexts that can be extended:

```java
public class BaseGameInputContext implements InputContext {
  protected static final Map<KeyBinding, InputCommand> BASE_BINDINGS = Map.ofEntries(
      Map.entry(KeyBinding.of(KeyType.EOF), InputCommand.QUIT),
      Map.entry(KeyBinding.of('Q', true, false, false), InputCommand.QUIT)
  );
}

public class SpecificGameInputContext extends BaseGameInputContext {
  private static final Map<KeyBinding, InputCommand> BINDINGS;

  static {
    Map<KeyBinding, InputCommand> combined = new HashMap<>(BASE_BINDINGS);
    combined.put(KeyBinding.of('P'), InputCommand.PAUSE);
    combined.put(KeyBinding.of('H'), InputCommand.HIT);
    BINDINGS = Map.copyOf(combined);
  }

  @Override
  public Map<KeyBinding, InputCommand> getBindings() {
    return BINDINGS;
  }
}
```

### Rebindable Keys

For user-configurable bindings:

```java
public class ConfigurableInputContext implements InputContext {
  private Map<KeyBinding, InputCommand> bindings;

  public ConfigurableInputContext(Map<KeyBinding, InputCommand> userBindings) {
    this.bindings = Map.copyOf(userBindings);
  }

  public void rebind(KeyBinding key, InputCommand command) {
    Map<KeyBinding, InputCommand> newBindings = new HashMap<>(bindings);
    newBindings.put(key, command);
    this.bindings = Map.copyOf(newBindings);
  }

  @Override
  public Map<KeyBinding, InputCommand> getBindings() {
    return bindings;
  }
}
```

---

## Additional Resources

- **Architecture Documentation**: See `ARCHITECTURE.md` for subsystem overview
- **Developer Guide**: See `DEVELOPER_GUIDE.md` for development workflow
- **UI Components Guide**: See `UI_COMPONENTS_GUIDE.md` for UI component input handling

**Source Code References**:
- Engine input package: `src/main/java/net/luxsolari/engine/input/`
- Game input contexts: `src/main/java/net/luxsolari/game/input/`
- InputManager: `src/main/java/net/luxsolari/engine/manager/InputManager.java`
- InputSubsystem: `src/main/java/net/luxsolari/engine/systems/internal/InputSubsystem.java`

---

*Last Updated: 2025*
*For Console Jack - Terminal-based Blackjack Game*
