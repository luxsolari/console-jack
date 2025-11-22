package net.luxsolari.engine.ui;

/**
 * Base interface for all UI components in the engine.
 * Provides core functionality for rendering, positioning, and bounds management.
 */
public interface UIComponent {

  /**
   * Renders this component on the specified layer.
   *
   * @param layerIdx the Z-layer index to render on
   */
  void render(int layerIdx);

  /**
   * Gets the bounding rectangle of this component.
   *
   * @return the component's bounds
   */
  UIBounds getBounds();

  /**
   * Sets the position of this component.
   *
   * @param x the x coordinate
   * @param y the y coordinate
   */
  void setPosition(int x, int y);

  /**
   * Gets the x coordinate of this component.
   *
   * @return the x coordinate
   */
  int getX();

  /**
   * Gets the y coordinate of this component.
   *
   * @return the y coordinate
   */
  int getY();

  /**
   * Gets the width of this component.
   *
   * @return the width
   */
  int getWidth();

  /**
   * Gets the height of this component.
   *
   * @return the height
   */
  int getHeight();

  /**
   * Sets whether this component is visible.
   *
   * @param visible true to make visible, false to hide
   */
  void setVisible(boolean visible);

  /**
   * Checks if this component is visible.
   *
   * @return true if visible, false if hidden
   */
  boolean isVisible();
}