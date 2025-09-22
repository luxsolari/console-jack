package net.luxsolari.engine.ecs;

import net.luxsolari.engine.viewport.Anchor;

/**
 * Holds the relative position of an entity on screen using normalized coordinates.
 * Coordinates are relative to the viewport size (0.0-1.0 range).
 *
 * @param relX relative X coordinate (0.0 = left edge, 1.0 = right edge)
 * @param relY relative Y coordinate (0.0 = top edge, 1.0 = bottom edge)
 * @param anchor positioning anchor for the element
 */
public record Position(float relX, float relY, Anchor anchor) implements Component {

  /**
   * Creates a position with default TOP_LEFT anchor.
   *
   * @param relX relative X coordinate (0.0-1.0)
   * @param relY relative Y coordinate (0.0-1.0)
   */
  public Position(float relX, float relY) {
    this(relX, relY, Anchor.TOP_LEFT);
  }

  /**
   * Creates a centered position with CENTER anchor.
   *
   * @return position at center of screen
   */
  public static Position centered() {
    return new Position(0.5f, 0.5f, Anchor.CENTER);
  }

  /**
   * Creates a position at the top-left corner.
   *
   * @return position at top-left of screen
   */
  public static Position topLeft() {
    return new Position(0.0f, 0.0f, Anchor.TOP_LEFT);
  }

  /**
   * Creates a position at the bottom-center.
   *
   * @return position at bottom-center of screen
   */
  public static Position bottomCenter() {
    return new Position(0.5f, 1.0f, Anchor.BOTTOM_CENTER);
  }

  /**
   * Creates a new position with updated relative coordinates.
   *
   * @param newRelX new relative X coordinate
   * @param newRelY new relative Y coordinate
   * @return new Position with updated coordinates
   */
  public Position withCoordinates(float newRelX, float newRelY) {
    return new Position(newRelX, newRelY, anchor);
  }

  /**
   * Creates a new position with updated anchor.
   *
   * @param newAnchor new anchor
   * @return new Position with updated anchor
   */
  public Position withAnchor(Anchor newAnchor) {
    return new Position(relX, relY, newAnchor);
  }
}
