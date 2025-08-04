package net.luxsolari.engine.ecs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stores all active {@link Entity} instances and offers very simple component-based queries.
 * <p>
 *     For a text-based blackjack game the number of entities will be tiny, so a linear scan is
 *     perfectly adequate—no fancy indexing required.
 * </p>
 */
public class EntityPool {

    private final List<Entity> entities = new ArrayList<>();

    /** Creates and registers a new {@link Entity}. */
    public Entity create() {
        Entity e = new Entity();
        entities.add(e);
        return e;
    }

    /** @return immutable snapshot of all entities. */
    public List<Entity> all() {
        return List.copyOf(entities);
    }

    /**
     * Returns entities that possess <em>all</em> of the requested component types.
     * Simple linear filter—good enough for small entity counts.
     */
    @SafeVarargs
    public final List<Entity> with(Class<? extends Component>... types) {
        return entities.stream()
                .filter(e -> Arrays.stream(types).allMatch(e::has))
                .toList();
    }
}
