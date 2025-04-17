package net.luxsolari.game.entity;

import net.luxsolari.engine.systems.ZLayerPosition;

/**
 * Represents a position in the game world using integer coordinates. This class is used for
 * positioning entities and can be converted to a ZLayerPosition for rendering.
 */
public record EntityPosition(int x, int y) {

  /**
   * Creates a new position translated by the specified offsets.
   *
   * @param dx The x-offset to translate by
   * @param dy The y-offset to translate by
   * @return A new EntityPosition with the translated coordinates
   */
  public EntityPosition translate(int dx, int dy) {
    return new EntityPosition(x + dx, y + dy);
  }

  /**
   * Calculates the distance to another position using the Manhattan distance.
   *
   * @param other The other position to calculate the distance to
   * @return The Manhattan distance between this position and the other position
   */
  public int distanceTo(EntityPosition other) {
    return Math.abs(x - other.x) + Math.abs(y - other.y);
  }

  /**
   * Converts this EntityPosition to a ZLayerPosition for rendering.
   *
   * @return A ZLayerPosition with the same coordinates
   */
  public ZLayerPosition toZLayerPosition() {
    return new ZLayerPosition(x, y);
  }
}
