package net.luxsolari.engine.ecs;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A lightweight, ID-based container of {@link Component}s.
 *
 * <p>Entities are data bags—no behaviour. They gain behaviour when one or more {@code EcsSystem}s
 * operate on their components.
 */
public class Entity {

  private static final AtomicLong NEXT_ID = new AtomicLong(0);
  private final long id = NEXT_ID.getAndIncrement();
  private final Map<Class<? extends Component>, Component> components = new HashMap<>();

  /**
   * Gets the unique integer identifier for this entity.
   *
   * @return unique integer identifier
   */
  public long id() {
    return id;
  }

  /** 
   * Attach a component to this entity (replaces existing of same type).
   *
   * @param component the component to attach to this entity
   */
  public <T extends Component> void add(T component) {
    components.put(component.getClass(), component);
  }

  /** 
   * Retrieve a component of the requested type (null if missing).
   *
   * @param type the class type of the component to retrieve
   * @return the component instance or null if not found
   */
  @SuppressWarnings("unchecked")
  public <T extends Component> T get(Class<T> type) {
    return (T) components.get(type);
  }

  /**
   * Checks if this entity has a component of the given type.
   *
   * @return true when this entity owns a component of the given type.
   */
  public boolean has(Class<? extends Component> type) {
    return components.containsKey(type);
  }
}
