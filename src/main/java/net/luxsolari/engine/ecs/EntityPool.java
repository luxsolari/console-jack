package net.luxsolari.engine.ecs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
  public long create() {
    Entity e = new Entity();
    entities.add(e);
    return e.id();
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
    entities.removeIf(e -> Arrays.stream(types).allMatch(e::has));
  }

  /**
   * Looks up a single entity by its unique numeric ID.
   *
   * <p>Entity IDs are assigned at creation time and never reused within a pool lifetime, so a
   * successful lookup always refers to the exact entity that was originally created with that ID.
   * The search is O(n) in the number of pooled entities because the pool is backed by an unindexed
   * list; prefer caching the returned {@link Entity} reference in hot paths rather than calling
   * this method on every tick.
   *
   * @param id the unique identifier of the entity to retrieve
   * @return an {@link Optional} containing the matching entity, or {@link Optional#empty()} if no
   *     entity with that ID exists in this pool
   */
  public Optional<Entity> getById(long id) {
    return entities.stream().filter(e -> e.id() == id).findFirst();
  }

  /**
   * Removes the entity with the specified id from the pool.
   *
   * <p>Returns true if an entity was removed, false if no entity with that id was found.
   *
   * @param id the id of the entity to remove
   * @return true if an entity was removed, false otherwise
   */
  public boolean removeById(long id) {
    return entities.removeIf(e -> e.id() == id);
  }

  /**
   * Checks if an entity with the specified ID exists in the pool.
   *
   * @param id the unique identifier of the entity to check
   * @return true if an entity with the specified ID exists, false otherwise
   */
  public boolean isValid(long id) {
    return entities.stream().anyMatch(e -> e.id() == id);
  }

  /**
   * This Java function returns a list of IDs for entities that have all specified types of components.
   * 
   * @return A list of `Long` ids of entities that have all the specified types of components.
   */
  @SafeVarargs
  public final List<Long> idsWith(Class<? extends Component>... types) {
    return entities.stream()
        .filter(e -> Arrays.stream(types).allMatch(e::has))
        .map(Entity::id)
        .toList();
  }
}
