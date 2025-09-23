package net.luxsolari.engine.ecs.systems;

import com.googlecode.lanterna.TextCharacter;
import java.util.ArrayList;
import java.util.List;
import net.luxsolari.engine.ecs.EcsSystem;
import net.luxsolari.engine.ecs.EntityPool;
import net.luxsolari.engine.ecs.Layer;
import net.luxsolari.engine.ecs.Position;
import net.luxsolari.engine.ecs.ScalableVisual;
import net.luxsolari.engine.ecs.Visual;
import net.luxsolari.engine.manager.RenderManager;
import net.luxsolari.engine.records.RenderCmd;
import net.luxsolari.engine.systems.internal.RenderSubsystem;
import net.luxsolari.game.display.CardSizeTier;
import net.luxsolari.engine.viewport.ViewportManager;
import net.luxsolari.game.ecs.CardSprite;

/**
 * Gathers all drawable ECS entities each tick and hands a flat display list to the render thread.
 *
 * <p>This replaces the earlier {@code EcsRenderSystem} queue-per-entity approach with a zero-lock
 * buffer hand-off: the logic thread assembles an immutable {@link List} of {@link RenderCmd}s and
 * the {@link RenderSubsystem} consumes it on the next frame.
 */
public class DisplayListSystem implements EcsSystem {

  @Override
  public void update(double dt, EntityPool pool) {
    if (!RenderSubsystem.INSTANCE.ready()) {
      return; // render thread not fully initialized yet
    }

    List<RenderCmd> list = new ArrayList<>();
    ViewportManager viewport = ViewportManager.INSTANCE;

    // Single-glyph visuals with relative positioning
    pool.with(Position.class, Visual.class, Layer.class)
        .forEach(
            e -> {
              Position p = e.get(Position.class);
              Visual v = e.get(Visual.class);
              Layer l = e.get(Layer.class);

              // Convert relative position to absolute screen coordinates
              int screenX = viewport.toScreenX(p.relX(), p.anchor());
              int screenY = viewport.toScreenY(p.relY(), p.anchor());

              list.add(new RenderCmd(l.index(), screenX, screenY, v.glyph()));
            });

    // Scalable visuals with tier-aware rendering - tier should be provided by game layer
    pool.with(Position.class, ScalableVisual.class, Layer.class)
        .forEach(
            e -> {
              Position p = e.get(Position.class);
              ScalableVisual sv = e.get(ScalableVisual.class);
              Layer l = e.get(Layer.class);

              // Convert relative position to absolute screen coordinates
              int screenX = viewport.toScreenX(p.relX(), p.anchor());
              int screenY = viewport.toScreenY(p.relY(), p.anchor());

              // For now, use MEDIUM as default - game layer should set appropriate tier
              TextCharacter glyph = sv.getVisualForTier(CardSizeTier.MEDIUM);

              list.add(new RenderCmd(l.index(), screenX, screenY, glyph));
            });

    // Multi-cell card sprites with relative positioning and size awareness
    pool.with(Position.class, Layer.class, CardSprite.class)
        .forEach(
            e -> {
              Position p = e.get(Position.class);
              Layer l = e.get(Layer.class);
              CardSprite sprite = e.get(CardSprite.class);
              String[] art = sprite.current();

              // Convert relative position to absolute screen coordinates
              // For multi-cell sprites, we need to consider the sprite size for anchor calculation
              int spriteWidth = sprite.cols();
              int spriteHeight = sprite.rows();
              int screenX = viewport.toScreenX(p.relX(), p.anchor(), spriteWidth);
              int screenY = viewport.toScreenY(p.relY(), p.anchor(), spriteHeight);

              // Render each cell of the sprite
              for (int row = 0; row < sprite.rows(); row++) {
                String line = art[row];
                for (int col = 0; col < sprite.cols(); col++) {
                  char ch = line.charAt(col);
                  // if (ch == ' ') continue; // skip transparent cells
                  list.add(
                      new RenderCmd(
                          l.index(), screenX + col, screenY + row, TextCharacter.fromCharacter(ch)[0]));
                }
              }
            });

    RenderManager.submitDisplayList(list);
  }
}
