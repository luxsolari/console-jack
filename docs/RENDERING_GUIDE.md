# Console Jack - Rendering System Guide

A comprehensive guide to the rendering system for displaying visuals in Console Jack's terminal interface.

## Table of Contents

- [Overview](#overview)
- [Z-Layer System](#z-layer-system)
- [RenderManager API](#rendermanager-api)
- [Viewport and Coordinates](#viewport-and-coordinates)
- [Colors and Styling](#colors-and-styling)
- [Drawing Primitives](#drawing-primitives)
- [High-Level Helpers](#high-level-helpers)
- [ECS Integration](#ecs-integration)
- [Best Practices](#best-practices)
- [Real-World Examples](#real-world-examples)
- [Troubleshooting](#troubleshooting)

---

## Overview

Console Jack's rendering system uses **Lanterna** for terminal UI and a custom **Z-layer architecture** for managing draw order. The system is designed for the 8 UPS game loop with thread-safe rendering on a dedicated render thread.

### Key Benefits

- **Z-Layer Composition**: Control draw order precisely
- **Thread-Safe**: Rendering happens on dedicated thread
- **Terminal-Agnostic**: Works across different terminal emulators
- **Simple API**: High-level methods for common operations
- **Relative Positioning**: Coordinate system adapts to screen size
- **Color Support**: ANSI and RGB colors

### Rendering Architecture

```
┌──────────────────────────────────────────────────────────┐
│                Game State (Main Thread)                   │
│  - Calls RenderManager.putString(), etc.                │
│  - Submits RenderCmd via DisplayListSystem              │
└───────────────────────┬──────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────┐
│              RenderManager (Static Facade)               │
│  - putString(), putChar(), drawBox()                    │
│  - Manages Z-layers                                     │
└───────────────────────┬──────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────┐
│            RenderSubsystem (Render Thread)               │
│  - Composites Z-layers                                  │
│  - Renders to Lanterna Screen                          │
│  - Refreshes display                                    │
└───────────────────────┬──────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────────────┐
│                Terminal (Lanterna)                       │
│  - Displays characters on screen                        │
└──────────────────────────────────────────────────────────┘
```

---

## Z-Layer System

The rendering system uses **Z-layers** (depth layers) to control draw order. Lower layer indices are drawn first (background), higher indices are drawn last (foreground).

### Layer Constants

```java
// Default layer for UI elements
RenderManager.UI_LAYER = 6

// Total available layers
RenderManager.getLayerCount() = 10 (typically)
```

### Layer Usage Pattern

```
Layer 0-1:   Background graphics
Layer 2-3:   Game objects (cards, chips, etc.)
Layer 4-5:   Game UI (score, timer)
Layer 6:     UI_LAYER (main UI overlay)
Layer 7-9:   Dialogs, tooltips, higher overlays
```

### Layer Operations

```java
// Clear a specific layer
RenderManager.clear(layerIndex);

// Clear all layers
RenderManager.clearAll();

// Get total number of layers
int maxLayers = RenderManager.getLayerCount();
```

### Example: Layering

```java
// Background
RenderManager.putString(0, 10, 5, "Background");

// Game objects
RenderManager.putString(2, 10, 5, "Card");

// UI overlay
RenderManager.putString(RenderManager.UI_LAYER, 10, 5, "Score: 100");

// Dialog (highest layer)
RenderManager.putString(RenderManager.UI_LAYER + 2, 10, 5, "Confirm?");
```

**Result**: Dialog appears on top, then UI, then card, then background.

---

## RenderManager API

The `RenderManager` is a stateless facade providing all rendering operations.

### Text Rendering

#### `putString()` - Basic Text

```java
// Default colors (white on dark gray)
RenderManager.putString(layerIdx, x, y, "Hello World");

// Custom foreground and background
RenderManager.putString(layerIdx, x, y, "Hello World",
    TextColor.ANSI.RED,
    TextColor.ANSI.BLACK);

// Custom foreground only (default background)
RenderManager.putStringCustomFg(layerIdx, x, y, "Hello World",
    TextColor.ANSI.CYAN);
```

#### `putStringRainbow()` - Rainbow Text

```java
// Each character cycles through rainbow colors
RenderManager.putStringRainbow(layerIdx, x, y, "RAINBOW TEXT");

// Colors: RED → YELLOW → GREEN → CYAN → BLUE → MAGENTA → repeat
```

#### `putStringGradient()` - Gradient Text

```java
// Linear gradient from color1 to color2
TextColor.RGB from = new TextColor.RGB(255, 0, 0);    // Red
TextColor.RGB to = new TextColor.RGB(0, 0, 255);      // Blue

RenderManager.putStringGradient(layerIdx, x, y, "Gradient Text", from, to);
```

### Character Rendering

#### `putChar()` - Single Character

```java
// Default colors
RenderManager.putChar(layerIdx, x, y, '@');

// Custom foreground only
RenderManager.putChar(layerIdx, x, y, '@', TextColor.ANSI.GREEN);

// Full control
RenderManager.putChar(layerIdx, x, y, '@',
    TextColor.ANSI.YELLOW,
    TextColor.ANSI.BLACK);
```

### Box Drawing

#### `drawBox()` - Rectangle Border

```java
// Draw box with inclusive coordinates
RenderManager.drawBox(
    layerIdx,
    x1, y1,    // Top-left corner
    x2, y2,    // Bottom-right corner
    TextColor.ANSI.WHITE,
    RenderManager.DEFAULT_BG
);
```

**Example**:
```java
// Draw 20x10 box at (5, 5)
RenderManager.drawBox(
    RenderManager.UI_LAYER,
    5, 5,      // Top-left
    25, 15,    // Bottom-right (5+20, 5+10)
    TextColor.ANSI.CYAN,
    TextColor.ANSI.BLACK
);
```

### Layer Management

```java
// Clear specific layer
RenderManager.clear(layerIdx);

// Clear all layers (use during state transitions)
RenderManager.clearAll();

// Get layer count
int count = RenderManager.getLayerCount();
```

---

## Viewport and Coordinates

Console Jack uses **relative positioning** (0.0-1.0) with **anchors** for resolution-independent layouts.

### ViewportManager

The `ViewportManager` converts relative coordinates to absolute screen coordinates.

#### Getting Screen Dimensions

```java
ViewportManager viewport = ViewportManager.INSTANCE;

int width = viewport.getWidth();    // Columns
int height = viewport.getHeight();  // Rows
```

#### Coordinate Conversion

```java
// Convert relative to absolute (simple)
int screenX = viewport.toScreenX(0.5f, Anchor.CENTER);
int screenY = viewport.toScreenY(0.5f, Anchor.CENTER);

// Convert with element size consideration
int screenX = viewport.toScreenX(0.5f, Anchor.CENTER, elementWidth);
int screenY = viewport.toScreenY(0.5f, Anchor.CENTER, elementHeight);
```

### Anchor System

Anchors define where an element's reference point is positioned relative to coordinates.

#### Available Anchors

```java
public enum Anchor {
  TOP_LEFT(0.0f, 0.0f),        // Element's top-left at coordinate
  TOP_CENTER(0.5f, 0.0f),      // Element's top-center at coordinate
  TOP_RIGHT(1.0f, 0.0f),       // Element's top-right at coordinate

  CENTER_LEFT(0.0f, 0.5f),     // Element's center-left at coordinate
  CENTER(0.5f, 0.5f),          // Element's center at coordinate
  CENTER_RIGHT(1.0f, 0.5f),    // Element's center-right at coordinate

  BOTTOM_LEFT(0.0f, 1.0f),     // Element's bottom-left at coordinate
  BOTTOM_CENTER(0.5f, 1.0f),   // Element's bottom-center at coordinate
  BOTTOM_RIGHT(1.0f, 1.0f);    // Element's bottom-right at coordinate
}
```

#### Anchor Examples

```java
// Center a 10-char string on screen
int x = viewport.toScreenX(0.5f, Anchor.CENTER, 10);
int y = viewport.toScreenY(0.5f, Anchor.CENTER, 1);
RenderManager.putString(layer, x, y, "CENTERED!");

// Top-left corner
int x = viewport.toScreenX(0.0f, Anchor.TOP_LEFT);
int y = viewport.toScreenY(0.0f, Anchor.TOP_LEFT);
RenderManager.putString(layer, x, y, "Top-left");

// Bottom-right corner
int x = viewport.toScreenX(1.0f, Anchor.BOTTOM_RIGHT, 12);
int y = viewport.toScreenY(1.0f, Anchor.BOTTOM_RIGHT, 1);
RenderManager.putString(layer, x, y, "Bottom-right");
```

### Relative Coordinates

```java
// Relative coordinates range from 0.0 to 1.0
0.0f = left/top edge
0.5f = center
1.0f = right/bottom edge

// Examples:
new Position(0.0f, 0.0f)  // Top-left of screen
new Position(0.5f, 0.5f)  // Center of screen
new Position(1.0f, 1.0f)  // Bottom-right of screen
new Position(0.25f, 0.75f) // 25% from left, 75% from top
```

---

## Colors and Styling

Console Jack supports both ANSI and RGB colors via Lanterna.

### Default Colors

```java
RenderManager.DEFAULT_FG = TextColor.ANSI.WHITE
RenderManager.DEFAULT_BG = new TextColor.RGB(53, 53, 47)  // Dark gray
```

### ANSI Colors

```java
TextColor.ANSI.BLACK
TextColor.ANSI.RED
TextColor.ANSI.GREEN
TextColor.ANSI.YELLOW
TextColor.ANSI.BLUE
TextColor.ANSI.MAGENTA
TextColor.ANSI.CYAN
TextColor.ANSI.WHITE
```

### RGB Colors

```java
// Custom RGB color
TextColor.RGB custom = new TextColor.RGB(255, 128, 0);  // Orange

// Usage
RenderManager.putString(layer, x, y, "Orange Text", custom, RenderManager.DEFAULT_BG);
```

### TextCharacter

`TextCharacter` combines character, foreground, and background:

```java
// Create TextCharacter
TextCharacter glyph = TextCharacter.fromCharacter(
    '@',                         // Character
    TextColor.ANSI.RED,         // Foreground
    TextColor.ANSI.BLACK        // Background
)[0];

// Use in rendering
RenderManager.putChar(layer, x, y, '@', TextColor.ANSI.RED, TextColor.ANSI.BLACK);
```

---

## Drawing Primitives

### Drawing Text

```java
// Simple text
RenderManager.putString(layer, 10, 5, "Hello World");

// Multiline text
String[] lines = {"Line 1", "Line 2", "Line 3"};
for (int i = 0; i < lines.length; i++) {
  RenderManager.putString(layer, 10, 5 + i, lines[i]);
}

// Centered text
ViewportManager viewport = ViewportManager.INSTANCE;
String text = "Centered Text";
int x = (viewport.getWidth() - text.length()) / 2;
int y = viewport.getHeight() / 2;
RenderManager.putString(layer, x, y, text);
```

### Drawing Boxes

```java
// Simple box
RenderManager.drawBox(
    layer,
    10, 5,     // Top-left
    30, 15,    // Bottom-right
    TextColor.ANSI.WHITE,
    RenderManager.DEFAULT_BG
);

// Centered box
int contentWidth = 20;
int contentHeight = 10;
RenderManager.drawCenteredBox(
    layer,
    contentWidth,
    contentHeight,
    TextColor.ANSI.CYAN,
    RenderManager.DEFAULT_BG
);
```

### Drawing Patterns

```java
// Horizontal line
for (int x = 10; x < 50; x++) {
  RenderManager.putChar(layer, x, 10, '-');
}

// Vertical line
for (int y = 5; y < 20; y++) {
  RenderManager.putChar(layer, 25, y, '|');
}

// Fill rectangle
for (int y = 5; y < 15; y++) {
  for (int x = 10; x < 30; x++) {
    RenderManager.putChar(layer, x, y, ' ',
        TextColor.ANSI.WHITE,
        TextColor.ANSI.BLUE);  // Blue background
  }
}
```

---

## High-Level Helpers

RenderManager provides high-level helpers for common patterns.

### Centered Text Block

```java
// Draws centered text with optional rainbow header and border
String[] lines = {
    "CONSOLE JACK",
    "Press any key to continue"
};

RenderManager.drawCenteredTextBlock(
    layer,
    lines,
    true  // Rainbow header for first line
);
```

**Result**:
```
┌────────────────────────┐
│                        │
│    CONSOLE JACK        │  <- Rainbow colored
│    Press any key...    │
│                        │
└────────────────────────┘
```

### Centered Box

```java
// Draws an empty centered box
RenderManager.drawCenteredBox(
    layer,
    40,  // Content width
    15,  // Content height
    TextColor.ANSI.WHITE,
    RenderManager.DEFAULT_BG
);
```

---

## ECS Integration

The DisplayListSystem integrates ECS entities with the rendering system.

### Render Flow

```
Entity Components → DisplayListSystem → RenderCmd → RenderSubsystem → Screen
```

### Required Components for Rendering

```java
// Single-glyph rendering
entity.add(new Position(0.5f, 0.5f, Anchor.CENTER));
entity.add(new Visual(TextCharacter.fromCharacter('A')[0]));
entity.add(new Layer(2));

// Multi-cell sprite rendering
entity.add(new Position(0.5f, 0.3f, Anchor.CENTER));
entity.add(new CardSprite(cardArt));
entity.add(new Layer(2));
```

### DisplayListSystem

The system queries entities and generates render commands:

```java
public class DisplayListSystem implements EcsSystem {
  @Override
  public void update(double dt, EntityPool pool) {
    List<RenderCmd> list = new ArrayList<>();
    ViewportManager viewport = ViewportManager.INSTANCE;

    // Query entities with Position + Visual + Layer
    pool.with(Position.class, Visual.class, Layer.class).forEach(entity -> {
      Position p = entity.get(Position.class);
      Visual v = entity.get(Visual.class);
      Layer l = entity.get(Layer.class);

      int screenX = viewport.toScreenX(p.relX(), p.anchor());
      int screenY = viewport.toScreenY(p.relY(), p.anchor());

      list.add(new RenderCmd(l.index(), screenX, screenY, v.glyph()));
    });

    RenderManager.submitDisplayList(list);
  }
}
```

### Manual Rendering vs ECS

```java
// Manual rendering (in state's render() method)
@Override
public void render() {
  RenderManager.clear(layer);
  RenderManager.putString(layer, 10, 5, "Score: " + score);
}

// ECS rendering (automatic via DisplayListSystem)
Entity scoreEntity = entityPool.create();
scoreEntity.add(new Position(0.1f, 0.1f, Anchor.TOP_LEFT));
scoreEntity.add(new Visual(TextCharacter.fromCharacter('S')[0]));
scoreEntity.add(new Layer(2));
// DisplayListSystem handles rendering automatically
```

---

## Best Practices

### 1. Clear Layers Before Rendering

```java
@Override
public void render() {
  RenderManager.clear(RenderManager.UI_LAYER);  // ✅ Clear first

  // Then render
  RenderManager.putString(RenderManager.UI_LAYER, x, y, text);
}
```

### 2. Use Appropriate Layers

```java
// ✅ Good: Use higher layers for overlays
RenderManager.putString(RenderManager.UI_LAYER, x, y, "Menu");
RenderManager.putString(RenderManager.UI_LAYER + 2, x, y, "Dialog");

// ❌ Bad: Everything on same layer
RenderManager.putString(RenderManager.UI_LAYER, x, y, "Everything");
```

### 3. Cache Coordinate Calculations

```java
// ✅ Good: Calculate once per frame
@Override
public void render() {
  ViewportManager viewport = ViewportManager.INSTANCE;
  int centerX = viewport.getWidth() / 2;
  int centerY = viewport.getHeight() / 2;

  // Use cached values
  for (Label label : labels) {
    RenderManager.putString(layer, centerX, centerY++, label.getText());
  }
}

// ❌ Bad: Recalculate every time
for (Label label : labels) {
  int centerX = ViewportManager.INSTANCE.getWidth() / 2;  // Wasteful
  RenderManager.putString(layer, centerX, centerY++, label.getText());
}
```

### 4. Use Relative Positioning

```java
// ✅ Good: Relative coordinates (adapts to screen size)
entity.add(new Position(0.5f, 0.5f, Anchor.CENTER));

// ❌ Bad: Absolute coordinates (breaks on different screen sizes)
int x = 40;  // What if screen is only 30 columns wide?
int y = 15;
```

### 5. Check Render Ready

```java
@Override
public void render() {
  if (!renderReady()) {  // ✅ Check before rendering
    return;
  }

  // Safe to render
  RenderManager.putString(layer, x, y, text);
}
```

### 6. Clean Up Layers in end()

```java
@Override
public void end() {
  // Clear layers used by this state
  RenderManager.clear(RenderManager.UI_LAYER);
  RenderManager.clear(RenderManager.UI_LAYER + 1);

  // Or clear all
  RenderManager.clearAll();
}
```

### 7. Use Named Constants for Layers

```java
// ✅ Good: Named constants
private static final int CARD_LAYER = 2;
private static final int UI_LAYER = RenderManager.UI_LAYER;
private static final int DIALOG_LAYER = RenderManager.UI_LAYER + 2;

// Then use
RenderManager.putString(CARD_LAYER, x, y, card);

// ❌ Bad: Magic numbers
RenderManager.putString(2, x, y, card);  // What is layer 2?
```

### 8. Bounds Check When Needed

```java
ViewportManager viewport = ViewportManager.INSTANCE;
int maxX = viewport.getWidth();
int maxY = viewport.getHeight();

// Check bounds
if (x >= 0 && x < maxX && y >= 0 && y < maxY) {
  RenderManager.putChar(layer, x, y, ch);
}
```

---

## Real-World Examples

### Example 1: Rendering a Menu Title

```java
@Override
public void render() {
  RenderManager.clear(RenderManager.UI_LAYER);

  if (!renderReady()) {
    return;
  }

  ViewportManager viewport = ViewportManager.INSTANCE;

  // Centered rainbow title
  String title = "CONSOLE JACK";
  int titleX = (viewport.getWidth() - title.length()) / 2;
  int titleY = 5;

  RenderManager.putStringRainbow(RenderManager.UI_LAYER, titleX, titleY, title);
}
```

### Example 2: Drawing a Centered Dialog

```java
private void renderDialog() {
  int layer = RenderManager.UI_LAYER + 2;
  RenderManager.clear(layer);

  ViewportManager viewport = ViewportManager.INSTANCE;

  String[] lines = {
      "Confirm Quit",
      "Are you sure?",
      "Press Y/N"
  };

  // Calculate centered position
  int maxWidth = 0;
  for (String line : lines) {
    maxWidth = Math.max(maxWidth, line.length());
  }

  int startX = (viewport.getWidth() - maxWidth) / 2;
  int startY = (viewport.getHeight() - lines.length) / 2;

  // Draw box
  RenderManager.drawBox(
      layer,
      startX - 2, startY - 1,
      startX + maxWidth + 1, startY + lines.length,
      TextColor.ANSI.YELLOW,
      RenderManager.DEFAULT_BG
  );

  // Draw text
  for (int i = 0; i < lines.length; i++) {
    int x = (viewport.getWidth() - lines[i].length()) / 2;
    RenderManager.putString(layer, x, startY + i, lines[i],
        TextColor.ANSI.WHITE, RenderManager.DEFAULT_BG);
  }
}
```

### Example 3: Rendering Score Display

```java
private void renderScore(int score, int x, int y) {
  String scoreText = "Score: " + score;

  // Gradient from green to yellow
  TextColor.RGB green = new TextColor.RGB(0, 255, 0);
  TextColor.RGB yellow = new TextColor.RGB(255, 255, 0);

  RenderManager.putStringGradient(
      RenderManager.UI_LAYER,
      x, y,
      scoreText,
      green,
      yellow
  );
}
```

### Example 4: Drawing Card on Table

```java
private void drawCardEntity(Entity cardEntity) {
  if (!cardEntity.has(Position.class) || !cardEntity.has(CardSprite.class)) {
    return;
  }

  Position pos = cardEntity.get(Position.class);
  CardSprite sprite = cardEntity.get(CardSprite.class);
  Layer layer = cardEntity.get(Layer.class);

  ViewportManager viewport = ViewportManager.INSTANCE;

  // Convert relative position to screen coordinates
  int screenX = viewport.toScreenX(pos.relX(), pos.anchor(), sprite.cols());
  int screenY = viewport.toScreenY(pos.relY(), pos.anchor(), sprite.rows());

  // Render each row of the card
  String[] art = sprite.current();
  for (int row = 0; row < sprite.rows(); row++) {
    String line = art[row];
    RenderManager.putString(layer.index(), screenX, screenY + row, line,
        TextColor.ANSI.WHITE, RenderManager.DEFAULT_BG);
  }
}
```

### Example 5: Instruction Labels

```java
private void renderInstructions() {
  if (instructionLabels == null) {
    return;
  }

  ViewportManager viewport = ViewportManager.INSTANCE;
  int screenWidth = viewport.getWidth();
  int screenHeight = viewport.getHeight();

  // Calculate vertical centering
  int totalHeight = instructionLabels.size();
  int startY = (screenHeight - totalHeight) / 2;

  // Render each label centered
  for (int i = 0; i < instructionLabels.size(); i++) {
    Label label = instructionLabels.get(i);
    int labelWidth = label.getText().length();
    int centerX = (screenWidth - labelWidth) / 2;

    label.setPosition(centerX, startY + i);
    label.render(RenderManager.UI_LAYER);
  }
}
```

---

## Troubleshooting

### Nothing Rendering

**Symptom**: Calls to RenderManager don't show anything

**Solutions**:
1. Check `renderReady()` returns true
2. Ensure layer is being rendered (not cleared immediately after)
3. Verify coordinates are within screen bounds
4. Check text color isn't same as background

### Text Appears Briefly Then Disappears

**Symptom**: Text flickers or disappears

**Solutions**:
1. Don't call `RenderManager.clear()` after rendering
2. Ensure render() is called every frame
3. Check if another state is clearing the layer

### Wrong Draw Order

**Symptom**: Elements rendering in wrong order

**Solutions**:
1. Use higher layer indices for elements that should be on top
2. Check layer indices: 0 (back) → 9 (front)
3. Verify RenderManager.UI_LAYER usage

### Text Cut Off

**Symptom**: Text is partially visible or cut off

**Solutions**:
1. Check screen bounds: `viewport.getWidth()`, `viewport.getHeight()`
2. Verify X coordinate + text length < screen width
3. Use relative positioning to adapt to screen size

### Colors Not Showing

**Symptom**: Colors appear as default white/black

**Solutions**:
1. Check terminal supports ANSI colors
2. Verify TextColor parameters are not null
3. Try ANSI colors before RGB (wider support)

### Performance Issues

**Symptom**: Rendering is slow or laggy

**Solutions**:
1. Don't render unchanged content every frame
2. Cache coordinate calculations
3. Minimize `putChar()` calls in loops
4. Use `putString()` instead of multiple `putChar()` calls

### Overlapping Text

**Symptom**: Text overlaps from previous frames

**Solutions**:
1. Call `RenderManager.clear(layer)` at start of `render()`
2. Use `RenderManager.clearAll()` during state transitions
3. Clear specific positions before redrawing

---

## Additional Resources

- **Architecture Documentation**: See `ARCHITECTURE.md` for overall system design
- **ECS Guide**: See `ECS_GUIDE.md` for entity rendering
- **State Machine Guide**: See `STATE_MACHINE_GUIDE.md` for state render() methods
- **UI Components Guide**: See `UI_COMPONENTS_GUIDE.md` for UI rendering
- **Developer Guide**: See `DEVELOPER_GUIDE.md` for development workflow

**Source Code References**:
- RenderManager: `src/main/java/net/luxsolari/engine/manager/RenderManager.java`
- ViewportManager: `src/main/java/net/luxsolari/engine/manager/ViewportManager.java`
- Anchor: `src/main/java/net/luxsolari/engine/viewport/Anchor.java`
- DisplayListSystem: `src/main/java/net/luxsolari/engine/ecs/systems/DisplayListSystem.java`
- RenderSubsystem: `src/main/java/net/luxsolari/engine/systems/internal/RenderSubsystem.java`

**Lanterna Documentation**: https://github.com/mabe02/lanterna

---

*Last Updated: 2025*
*For Console Jack - Terminal-based Blackjack Game*
