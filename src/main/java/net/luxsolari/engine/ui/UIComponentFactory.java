package net.luxsolari.engine.ui;

/**
 * Factory class for creating UI components with optional object pooling.
 * This helps reduce garbage collection pressure for frequently created UI elements.
 * 
 * Usage example:
 * <pre>
 * MenuItem item = UIComponentFactory.createMenuItem("Text", action);
 * // ... use the item
 * UIComponentFactory.releaseMenuItem(item); // Return to pool when done
 * </pre>
 */
public final class UIComponentFactory {

  // Object pools for different UI component types
  private static final UIObjectPool<MenuItem> MENU_ITEM_POOL = 
      new UIObjectPool<>(() -> new MenuItem(0, 0, "", null), 50);
  
  private static final UIObjectPool<Label> LABEL_POOL = 
      new UIObjectPool<>(() -> new Label(0, 0, ""), 20);

  private UIComponentFactory() {
    // Utility class - prevent instantiation
  }

  /**
   * Creates a new MenuItem directly (pooling not suitable due to immutable fields).
   * For MenuItem, direct creation is preferred since text and action are final.
   *
   * @param text the menu item text
   * @param action the action to execute when selected
   * @return a new MenuItem ready for use
   */
  public static MenuItem createMenuItem(String text, MenuAction action) {
    return new MenuItem(0, 0, text, action);
  }

  /**
   * No-op for MenuItem since they aren't pooled due to immutable fields.
   * This method exists for API consistency.
   *
   * @param item the MenuItem (ignored)
   */
  public static void releaseMenuItem(MenuItem item) {
    // No-op - MenuItems have immutable fields so pooling isn't beneficial
  }

  /**
   * Creates a new Label directly (pooling not suitable due to immutable text).
   * For Label, direct creation is preferred since text is final.
   *
   * @param text the label text
   * @return a new Label ready for use
   */
  public static Label createLabel(String text) {
    return new Label(0, 0, text);
  }

  /**
   * No-op for Label since they aren't pooled due to immutable text.
   * This method exists for API consistency.
   *
   * @param label the Label (ignored)
   */
  public static void releaseLabel(Label label) {
    // No-op - Labels have immutable text so pooling isn't beneficial
  }

  /**
   * Gets statistics about the current pool usage.
   * Currently no pools are active due to immutable component designs.
   *
   * @return a string with pool statistics
   */
  public static String getPoolStats() {
    return "UI Pool Stats - No active pools (components have immutable fields)";
  }

  /**
   * No-op since no pools are currently active.
   * This method exists for future extensibility.
   */
  public static void clearAllPools() {
    // No-op - no active pools currently
  }

  /**
   * Note: Object pooling for MenuItem and Label is not implemented
   * because their key properties (text, action) are immutable final fields.
   * 
   * For future UI components that need pooling:
   * 1. Ensure mutable state that can be reset
   * 2. Implement proper reset() methods
   * 3. Add pooling logic similar to the pattern above
   * 
   * Consider pooling for:
   * - Complex containers with many mutable properties
   * - Temporary UI elements like tooltips or dialogs
   * - Animation or effect objects
   */
}