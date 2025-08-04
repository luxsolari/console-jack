package net.luxsolari.engine.ecs.systems;

import java.util.ArrayList;
import java.util.List;
import net.luxsolari.engine.ecs.EntityPool;
import net.luxsolari.engine.ecs.EcsSystem;
import net.luxsolari.engine.ecs.Layer;
import net.luxsolari.engine.ecs.Position;
import net.luxsolari.engine.ecs.Visual;
import net.luxsolari.engine.records.RenderCmd;
import net.luxsolari.engine.systems.RenderSubsystem;

/**
 * Gathers all drawable ECS entities each tick and hands a flat display list to the render thread.
 * <p>
 *     This replaces the earlier {@code EcsRenderSystem} queue-per-entity approach with a
 *     zero-lock buffer hand-off: the logic thread assembles an immutable {@link List} of
 *     {@link RenderCmd}s and the {@link RenderSubsystem} consumes it on the next frame.
 * </p>
 */
public class DisplayListSystem implements EcsSystem {

    private final RenderSubsystem render = RenderSubsystem.getInstance();

    @Override
    public void update(double dt, EntityPool pool) {
        if (!render.ready()) {
            return; // render thread not fully initialised yet
        }

        List<RenderCmd> list = new ArrayList<>();
        pool.with(Position.class, Visual.class, Layer.class).forEach(e -> {
            Position p = e.get(Position.class);
            Visual v = e.get(Visual.class);
            Layer l = e.get(Layer.class);
            list.add(new RenderCmd(l.index(), p.x(), p.y(), v.glyph()));
        });

        render.submitDisplayList(list);
    }
}
