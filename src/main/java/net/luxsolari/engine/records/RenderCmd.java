package net.luxsolari.engine.records;

import com.googlecode.lanterna.TextCharacter;
import net.luxsolari.engine.ecs.systems.DisplayListSystem;
import net.luxsolari.engine.systems.RenderSubsystem;

/**
 * Immutable draw command representing a single glyph to render on the terminal.
 *
 * <p>Produced by ECS systems (e.g., {@link DisplayListSystem}) each tick, {@code RenderCmd}
 * encapsulates all information needed for the render thread to draw a character at a specific
 * position and layer. Enables lock-free, thread-safe handoff between logic and rendering.
 *
 * <ul>
 *   <li><b>layer</b>: Render layer index for draw order
 *   <li><b>x, y</b>: Terminal grid coordinates
 *   <li><b>glyph</b>: {@link TextCharacter} to display
 * </ul>
 *
 * <p>Consumed by {@link RenderSubsystem} during rendering.
 */
public record RenderCmd(int layer, int x, int y, TextCharacter glyph) {}
