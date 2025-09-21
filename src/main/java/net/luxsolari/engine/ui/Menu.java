package net.luxsolari.engine.ui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import net.luxsolari.engine.manager.RenderManager;
import net.luxsolari.engine.systems.internal.RenderSubsystem;

/**
 * A navigable menu container that displays a list of menu items with a title.
 * Supports arrow key navigation, automatic centering, and optional border decoration.
 */
public class Menu extends UIContainer implements Focusable {

  private final String title;
  private boolean focused = false;
  private boolean showBorder = true;
  private boolean centerOnScreen = true;
  private boolean layoutDirty = true; // Track when layout needs update
  private int cachedScreenWidth = -1;
  private int cachedScreenHeight = -1;

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
    layoutDirty = true; // Mark layout as needing update
    return this;
  }

  /**
   * Sets whether to show a border around the menu.
   *
   * @param showBorder true to show border, false to hide
   * @return this menu for method chaining
   */
  public Menu setBorder(boolean showBorder) {
    this.showBorder = showBorder;
    layoutDirty = true; // Mark layout as needing update
    return this;
  }

  /**
   * Sets whether to center the menu on screen automatically.
   *
   * @param centerOnScreen true to center automatically, false for manual positioning
   * @return this menu for method chaining
   */
  public Menu setCenterOnScreen(boolean centerOnScreen) {
    this.centerOnScreen = centerOnScreen;
    if (centerOnScreen) {
      layoutDirty = true; // Mark layout as needing update
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
    // Auto-focus first menu item if none is focused
    if (focusedIndex < 0 && !children.isEmpty()) {
      focusNext();
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

  @Override
  public boolean isFocused() {
    return focused;
  }

  @Override
  public boolean canFocus() {
    return isVisible() && !children.isEmpty();
  }

  @Override
  protected boolean handleContainerInput(KeyStroke keyStroke) {
    if (!isFocused() || keyStroke == null) {
      return false;
    }

    if (keyStroke.getKeyType() == KeyType.ArrowUp) {
      return focusPrevious();
    } else if (keyStroke.getKeyType() == KeyType.ArrowDown) {
      return focusNext();
    }

    return false;
  }

  @Override
  protected void doRender(int layerIdx) {
    if (children.isEmpty()) {
      return;
    }

    // Only update layout if needed or screen size changed
    if (layoutDirty || (centerOnScreen && hasScreenSizeChanged())) {
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

    // Calculate dimensions
    int maxItemWidth = title != null ? title.length() : 0;
    for (UIComponent child : children) {
      if (child instanceof MenuItem menuItem) {
        maxItemWidth = Math.max(maxItemWidth, menuItem.getText().length());
      }
    }

    int menuWidth = maxItemWidth + (showBorder ? 4 : 2); // padding
    int menuHeight = children.size() + (title != null ? 3 : 1) + (showBorder ? 2 : 0); // title + items + padding + border

    // Update menu size
    setSize(menuWidth, menuHeight);

    // Position menu on screen if centering is enabled
    if (centerOnScreen) {
      // Use thread-safe approach with AtomicReference
      Screen screen = RenderSubsystem.INSTANCE.mainScreen().get();
      if (screen != null) {
        int screenWidth = screen.getTerminalSize().getColumns();
        int screenHeight = screen.getTerminalSize().getRows();
        cachedScreenWidth = screenWidth;
        cachedScreenHeight = screenHeight;
        int centerX = (screenWidth - menuWidth) / 2;
        int centerY = (screenHeight - menuHeight) / 2;
        setPosition(centerX, centerY);
      }
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
   * Checks if the screen size has changed since the last layout update.
   *
   * @return true if the screen size has changed, false otherwise
   */
  private boolean hasScreenSizeChanged() {
    Screen screen = RenderSubsystem.INSTANCE.mainScreen().get();
    if (screen != null) {
      int currentWidth = screen.getTerminalSize().getColumns();
      int currentHeight = screen.getTerminalSize().getRows();
      return currentWidth != cachedScreenWidth || currentHeight != cachedScreenHeight;
    }
    return false;
  }
}