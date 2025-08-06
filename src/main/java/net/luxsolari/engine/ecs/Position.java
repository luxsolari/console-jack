package net.luxsolari.engine.ecs;

/**
 * Holds the on-screen coordinates of an entity.
 *
 * @param x column index (0-based)
 * @param y row index (0-based)
 */
public record Position(int x, int y) implements Component {}
