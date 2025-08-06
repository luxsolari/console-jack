package net.luxsolari.engine.ecs;

import com.googlecode.lanterna.TextCharacter;

/**
 * Describes how an entity should look when rendered (single-glyph version).
 *
 * <p>More elaborate sprites/animations can extend this later.
 */
public record Visual(TextCharacter glyph) implements Component {}
