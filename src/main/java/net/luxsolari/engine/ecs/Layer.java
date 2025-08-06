package net.luxsolari.engine.ecs;

import net.luxsolari.engine.systems.RenderSubsystem;

/**
 * Declares which Z-layer an entity should be rendered on.
 *
 * <p>Lower indices are drawn first (background), higher on top (foreground).
 *
 * @param index layer index (values must be non-negative, and less than or equal to {@link
 *     RenderSubsystem#MAX_LAYERS})
 */
public record Layer(int index) implements Component {
  public Layer {
    if (index < 0) {
      throw new IllegalArgumentException("Layer index must be non-negative");
    }
  }
}
