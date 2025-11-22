package net.luxsolari.engine.ui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import net.luxsolari.engine.manager.RenderManager;

/**
 * A menu item widget that displays text and can execute an action when activated.
 * Supports focus highlighting and keyboard activation.
 */
public class MenuItem extends UIWidget implements Focusable, InputHandler {

  private final String text;
  private final String focusedText; // Pre-computed text for focused state
  private final MenuAction action;
  private boolean focused = false;

  private static final TextColor NORMAL_FG = RenderManager.DEFAULT_FG;
  private static final TextColor NORMAL_BG = RenderManager.DEFAULT_BG;
  private static final TextColor FOCUSED_FG = TextColor.ANSI.BLACK;
  private static final TextColor FOCUSED_BG = TextColor.ANSI.WHITE;

  /**
   * Creates a menu item with the specified text and action.
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @param text the text to display
   * @param action the action to execute when activated
   */
  public MenuItem(int x, int y, String text, MenuAction action) {
    super(x, y, text.length() + 2, 1); // +2 for '>' and '<' markers
    this.text = text;
    this.focusedText = ">" + text + "<"; // Pre-compute focused text
    this.action = action;
  }

  /**
   * Gets the text displayed by this menu item.
   *
   * @return the menu item text
   */
  public String getText() {
    return text;
  }

  /**
   * Gets the action associated with this menu item.
   *
   * @return the menu action
   */
  public MenuAction getAction() {
    return action;
  }

  /**
   * Activates this menu item, executing its associated action.
   */
  public void activate() {
    if (action != null) {
      action.execute();
    }
  }

  @Override
  public void focus() {
    focused = true;
  }

  @Override
  public void unfocus() {
    focused = false;
  }

  @Override
  public boolean isFocused() {
    return focused;
  }

  @Override
  public boolean canFocus() {
    return isVisible();
  }

  @Override
  public boolean handleInput(KeyStroke keyStroke) {
    if (!isFocused() || keyStroke == null) {
      return false;
    }

    // Handle activation (Enter or Space)
    if (keyStroke.getKeyType() == KeyType.Enter ||
        (keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == ' ')) {
      activate();
      return true;
    }

    return false;
  }

  @Override
  protected void doRender(int layerIdx) {
    TextColor fg = focused ? FOCUSED_FG : NORMAL_FG;
    TextColor bg = focused ? FOCUSED_BG : NORMAL_BG;

    // Use pre-computed text for focused state to avoid string allocations
    if (focused) {
      RenderManager.putString(layerIdx, getX() - 1, getY(), focusedText, fg, bg);
    } else {
      RenderManager.putString(layerIdx, getX(), getY(), text, fg, bg);
    }
  }
}