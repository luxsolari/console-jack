package net.luxsolari.engine.ecs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stores all active {@link Entity} instances and offers very simple component-based queries.
 *
 * <p>For a text-based game the number of entities will be tiny, so a linear scan is perfectly
 * adequate—no fancy indexing required.
 */
public class EntityPool {

  private final List<Entity> entities = new ArrayList<>();

  /** 
   * Creates and registers a new Entity in the pool.
   *
   * @return newly created entity
   */
  public Entity create() {
    Entity e = new Entity();
    entities.add(e);
    return e;
  }

  /**
   * Gets an immutable snapshot of all entities in the pool.
   *
   * @return immutable snapshot of all entities.
   */
  public List<Entity> all() {
    return List.copyOf(entities);
  }

  /**
   * Filters entities that possess all of the requested component types.
   *
   * <p>Returns entities that possess <em>all</em> of the requested component types. Simple linear
   * filter—good enough for small entity counts.
   *
   * @param types the component types to filter by
   * @return list of entities that have all the specified components
   */
  @SafeVarargs
  public final List<Entity> with(Class<? extends Component>... types) {
    return entities.stream().filter(e -> Arrays.stream(types).allMatch(e::has)).toList();
  }

  /**
   * Removes entities from the pool that have all specified component types.
   * 
   * <p>Clears entities from the pool of the requested component types. Simple linear filter—good
   * enough for small entity counts.
   *
   * @param types the component types to filter by for removal
   */
  @SafeVarargs
  public final void removeWith(Class<? extends Component>... types) {
    entities.removeIf(e ->
            Arrays.stream(types).allMatch(e::has)
    );
  }
}
