package net.luxsolari.engine.ui;

/**
 * Interface for UI components that can receive focus and be selected.
 * Components implementing this interface can participate in keyboard navigation.
 */
public interface Focusable {

  /**
   * Sets focus on this component.
   * Should update visual state to indicate focus.
   */
  void focus();

  /**
   * Removes focus from this component.
   * Should update visual state to indicate loss of focus.
   */
  void unfocus();

  /**
   * Checks if this component currently has focus.
   *
   * @return true if focused, false otherwise
   */
  boolean isFocused();

  /**
   * Checks if this component can receive focus.
   * Components may be temporarily unfocusable due to state (disabled, hidden, etc.).
   *
   * @return true if can receive focus, false otherwise
   */
  default boolean canFocus() {
    return true;
  }
}