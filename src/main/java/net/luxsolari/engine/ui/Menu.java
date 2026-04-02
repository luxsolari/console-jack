package net.luxsolari.engine.ui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import net.luxsolari.engine.input.InputCommand;
import net.luxsolari.engine.manager.RenderManager;
import net.luxsolari.engine.manager.ViewportManager;

/**
 * A navigable menu container that displays a list of menu items with a title.
 * Supports arrow key navigation, automatic centering, and optional border decoration.
 */
public class Menu extends UIContainer implements Focusable {

  private final String title;
  private boolean focused = false;
  private boolean showBorder = true;
  private boolean centerOnScreen = true;
  private volatile boolean layoutDirty = true; // Track when layout needs update
  private volatile int cachedMenuWidth = -1;
  private volatile int cachedMenuHeight = -1;

  /**
   * Creates a menu with the specified title.
   *
   * @param title the menu title
   */
  public Menu(String title) {
    super(0, 0, 0, 0);
    this.title = title;
  }

  /**
   * Creates a menu with the specified title at the given position.
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @param title the menu title
   */
  public Menu(int x, int y, String title) {
    super(x, y, 0, 0);
    this.title = title;
    this.centerOnScreen = false;
  }

  /**
   * Adds a menu item with the specified text and action.
   *
   * @param text the menu item text
   * @param action the action to execute when the item is selected
   * @return this menu for method chaining
   */
  public Menu addItem(String text, MenuAction action) {
    MenuItem item = new MenuItem(0, 0, text, action);
    addChild(item);
    invalidateLayout(); // Mark layout as needing update
    return this;
  }

  /**
   * Sets whether to show a border around the menu.
   *
   * @param showBorder true to show border, false to hide
   * @return this menu for method chaining
   */
  public Menu setBorder(boolean showBorder) {
    if (this.showBorder != showBorder) {
      this.showBorder = showBorder;
      invalidateLayout(); // Mark layout as needing update
    }
    return this;
  }

  /**
   * Sets whether to center the menu on screen automatically.
   *
   * @param centerOnScreen true to center automatically, false for manual positioning
   * @return this menu for method chaining
   */
  public Menu setCenterOnScreen(boolean centerOnScreen) {
    if (this.centerOnScreen != centerOnScreen) {
      this.centerOnScreen = centerOnScreen;
      if (centerOnScreen) {
        invalidateLayout(); // Mark layout as needing update
      }
    }
    return this;
  }

  /**
   * Gets the currently selected menu item.
   *
   * @return the selected menu item, or null if no item is selected
   */
  public MenuItem getSelectedItem() {
    UIComponent focused = getFocusedChild();
    return focused instanceof MenuItem menuItem ? menuItem : null;
  }

  @Override
  public void focus() {
    focused = true;
    // Auto-focus first focusable menu item if none is focused
    if (focusedIndex < 0 && !children.isEmpty()) {
      // Find the first focusable item
      for (int i = 0; i < children.size(); i++) {
        UIComponent child = children.get(i);
        if (child instanceof Focusable focusable && focusable.canFocus()) {
          setFocusToChild(child);
          break;
        }
      }
    }
  }

  @Override
  public void unfocus() {
    focused = false;
    // Unfocus current child
    UIComponent focusedChild = getFocusedChild();
    if (focusedChild instanceof Focusable focusable) {
      focusable.unfocus();
    }
  }
  
  /**
   * Completely resets the focus state of this menu and all its children.
   * This is useful when transitioning between states to ensure a clean focus state.
   */
  public void resetFocus() {
    // Unfocus all children first
    for (UIComponent child : children) {
      if (child instanceof Focusable focusable) {
        focusable.unfocus();
      }
    }
    
    // Reset focus index
    focusedIndex = -1;
    focused = false;
  }

  @Override
  public boolean isFocused() {
    return focused;
  }

  @Override
  public boolean canFocus() {
    if (!isVisible() || children.isEmpty()) {
      return false;
    }
    
    // Can only focus if at least one child is focusable
    return children.stream()
        .anyMatch(child -> child instanceof Focusable f && f.canFocus());
  }

  @Override
  protected boolean handleContainerInput(KeyStroke keyStroke) {
    if (!isFocused() || keyStroke == null) {
      return false;
    }

    switch (keyStroke.getKeyType()) {
      case ArrowUp:
        return focusPrevious();
      case ArrowDown:
        return focusNext();
      case Enter:
        // Execute action of focused menu item
        MenuItem selected = getSelectedItem();
        if (selected != null && selected.getAction() != null) {
          selected.getAction().execute();
          return true;
        }
        break;
      case Home:
        // Focus first item
        return focusFirstItem();
      case End:
        // Focus last item
        return focusLastItem();
      default:
        break;
    }

    return false;
  }

  /**
   * Handles input commands for menu navigation and actions.
   * This allows menus to respond to semantic commands rather than raw keystrokes.
   *
   * @param command the input command to handle
   * @return true if the command was handled, false otherwise
   */
  public boolean handleCommand(InputCommand command) {
    if (!isFocused() || command == null) {
      return false;
    }

    return switch (command) {
      case NAVIGATE_UP -> focusPrevious();
      case NAVIGATE_DOWN -> focusNext();
      case NAVIGATE_FIRST -> focusFirstItem();
      case NAVIGATE_LAST -> focusLastItem();
      case CONFIRM -> {
        MenuItem selected = getSelectedItem();
        if (selected != null && selected.getAction() != null) {
          selected.getAction().execute();
          yield true;
        }
        yield false;
      }
      default -> false;
    };
  }

  @Override
  protected void doRender(int layerIdx) {
    if (children.isEmpty()) {
      return;
    }
    
    // Optimize condition logic - only check what's necessary
    boolean shouldUpdate = layoutDirty;
    
    if (shouldUpdate) {
      updateLayout();
      layoutDirty = false;
    }

    // Render title
    if (title != null && !title.isEmpty()) {
      int titleX = getX() + (getWidth() - title.length()) / 2;
      RenderManager.putStringRainbow(layerIdx, titleX, getY(), title);
    }

    // Render border if enabled
    if (showBorder) {
      RenderManager.drawBox(layerIdx, getX() - 1, getY() - 1,
                           getX() + getWidth(), getY() + getHeight(),
                           TextColor.ANSI.WHITE, RenderManager.DEFAULT_BG);
    }
  }

  private void updateLayout() {
    if (children.isEmpty()) {
      return;
    }

    // Use cached dimensions calculation
    calculateMenuDimensions();
    int menuWidth = cachedMenuWidth;
    int menuHeight = cachedMenuHeight;

    // Update menu size
    setSize(menuWidth, menuHeight);

    // Position menu on screen if centering is enabled
    if (centerOnScreen) {
      int screenWidth = ViewportManager.INSTANCE.getWidth();
      int screenHeight = ViewportManager.INSTANCE.getHeight();
      int centerX = (screenWidth - menuWidth) / 2;
      int centerY = (screenHeight - menuHeight) / 2;
      setPosition(centerX, centerY);
    }

    // Position menu items
    int itemY = getY() + (title != null ? 2 : 1) + (showBorder ? 1 : 0);
    int itemX = getX() + (showBorder ? 2 : 1);

    for (int i = 0; i < children.size(); i++) {
      UIComponent child = children.get(i);
      child.setPosition(itemX, itemY + i);
    }
  }

  /**
   * Focuses the first focusable menu item.
   *
   * @return true if focus was set, false if no focusable items exist
   */
  private boolean focusFirstItem() {
    for (UIComponent child : children) {
      if (child instanceof Focusable focusable && focusable.canFocus()) {
        return setFocusToChild(child);
      }
    }
    return false;
  }

  /**
   * Focuses the last focusable menu item.
   *
   * @return true if focus was set, false if no focusable items exist
   */
  private boolean focusLastItem() {
    for (int i = children.size() - 1; i >= 0; i--) {
      UIComponent child = children.get(i);
      if (child instanceof Focusable focusable && focusable.canFocus()) {
        return setFocusToChild(child);
      }
    }
    return false;
  }

  /**
   * Invalidates the current layout, forcing a recalculation on the next render.
   */
  public void invalidateLayout() {
    layoutDirty = true;
    cachedMenuWidth = -1;
    cachedMenuHeight = -1;
  }

  /**
   * Optimized layout calculation with caching to prevent redundant calculations.
   */
  private void calculateMenuDimensions() {
    if (cachedMenuWidth != -1 && cachedMenuHeight != -1 && !layoutDirty) {
      return; // Use cached values
    }

    // Calculate dimensions
    int maxItemWidth = title != null ? title.length() : 0;
    for (UIComponent child : children) {
      if (child instanceof MenuItem menuItem) {
        maxItemWidth = Math.max(maxItemWidth, menuItem.getText().length());
      }
    }

    cachedMenuWidth = maxItemWidth + (showBorder ? 4 : 2); // padding
    cachedMenuHeight = children.size() + (title != null ? 3 : 1) + (showBorder ? 2 : 0);
  }
}