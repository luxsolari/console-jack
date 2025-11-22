package net.luxsolari.engine.ui;

/**
 * Represents the position and size of a UI component.
 * Immutable record for component bounds.
 */
public record UIBounds(int x, int y, int width, int height) {

  /**
   * Creates bounds with position (0,0) and specified dimensions.
   *
   * @param width the width
   * @param height the height
   * @return new UIBounds at origin
   */
  public static UIBounds ofSize(int width, int height) {
    return new UIBounds(0, 0, width, height);
  }

  /**
   * Creates new bounds with updated position.
   *
   * @param newX the new x coordinate
   * @param newY the new y coordinate
   * @return new UIBounds with updated position
   */
  public UIBounds withPosition(int newX, int newY) {
    return new UIBounds(newX, newY, width, height);
  }

  /**
   * Creates new bounds with updated size.
   *
   * @param newWidth the new width
   * @param newHeight the new height
   * @return new UIBounds with updated size
   */
  public UIBounds withSize(int newWidth, int newHeight) {
    return new UIBounds(x, y, newWidth, newHeight);
  }

  /**
   * Gets the right edge x coordinate (x + width - 1).
   *
   * @return the right edge coordinate
   */
  public int right() {
    return x + width - 1;
  }

  /**
   * Gets the bottom edge y coordinate (y + height - 1).
   *
   * @return the bottom edge coordinate
   */
  public int bottom() {
    return y + height - 1;
  }

  /**
   * Checks if this bounds contains the specified point.
   *
   * @param pointX the x coordinate to check
   * @param pointY the y coordinate to check
   * @return true if the point is within bounds
   */
  public boolean contains(int pointX, int pointY) {
    return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
  }
}