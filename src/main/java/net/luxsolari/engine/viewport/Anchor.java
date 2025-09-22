package net.luxsolari.engine.viewport;

/**
 * Anchor points for positioning elements relative to their coordinate.
 * Each anchor defines where the element's reference point is positioned
 * relative to the given coordinates.
 */
public enum Anchor {
  /** Top-left corner of element at the given coordinates */
  TOP_LEFT(0.0f, 0.0f),

  /** Top edge center of element at the given coordinates */
  TOP_CENTER(0.5f, 0.0f),

  /** Top-right corner of element at the given coordinates */
  TOP_RIGHT(1.0f, 0.0f),

  /** Left edge center of element at the given coordinates */
  CENTER_LEFT(0.0f, 0.5f),

  /** Center of element at the given coordinates */
  CENTER(0.5f, 0.5f),

  /** Right edge center of element at the given coordinates */
  CENTER_RIGHT(1.0f, 0.5f),

  /** Bottom-left corner of element at the given coordinates */
  BOTTOM_LEFT(0.0f, 1.0f),

  /** Bottom edge center of element at the given coordinates */
  BOTTOM_CENTER(0.5f, 1.0f),

  /** Bottom-right corner of element at the given coordinates */
  BOTTOM_RIGHT(1.0f, 1.0f);

  /**
   * Horizontal offset factor (0.0 = left edge, 0.5 = center, 1.0 = right edge).
   * This represents what part of the element's width should be subtracted
   * from the X coordinate to position the element correctly.
   */
  public final float xOffset;

  /**
   * Vertical offset factor (0.0 = top edge, 0.5 = center, 1.0 = bottom edge).
   * This represents what part of the element's height should be subtracted
   * from the Y coordinate to position the element correctly.
   */
  public final float yOffset;

  Anchor(float xOffset, float yOffset) {
    this.xOffset = xOffset;
    this.yOffset = yOffset;
  }

  /**
   * Calculates the final X coordinate for an element given its position and width.
   *
   * @param baseX the base X coordinate
   * @param elementWidth the width of the element
   * @return the adjusted X coordinate
   */
  public int adjustX(int baseX, int elementWidth) {
    return baseX - Math.round(xOffset * elementWidth);
  }

  /**
   * Calculates the final Y coordinate for an element given its position and height.
   *
   * @param baseY the base Y coordinate
   * @param elementHeight the height of the element
   * @return the adjusted Y coordinate
   */
  public int adjustY(int baseY, int elementHeight) {
    return baseY - Math.round(yOffset * elementHeight);
  }
}