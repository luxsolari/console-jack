package net.luxsolari.engine.records;

import com.googlecode.lanterna.TextCharacter;

/**
 * Immutable draw instruction prepared by the logic thread and consumed by the render thread.
 */
public record RenderCmd(int layer, int x, int y, TextCharacter glyph) {
}
