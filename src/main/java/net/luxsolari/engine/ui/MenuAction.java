package net.luxsolari.engine.ui;

/**
 * Functional interface representing an action that can be executed by a menu item.
 * This allows menu items to trigger custom behavior when selected.
 */
@FunctionalInterface
public interface MenuAction {

  /**
   * Executes the action associated with a menu item.
   * This method is called when the menu item is activated (e.g., Enter key pressed).
   */
  void execute();
}