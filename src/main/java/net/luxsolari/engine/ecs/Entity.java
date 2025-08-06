package net.luxsolari.engine.ecs;

import java.util.HashMap;
import java.util.Map;

/**
 * A lightweight, ID-based container of {@link Component}s.
 *
 * <p>Entities are data bags—no behaviour. They gain behaviour when one or more {@code EcsSystem}s
 * operate on their components.
 */
public class Entity {

  private static int NEXT_ID = 0;

  private final int id = NEXT_ID++;
  private final Map<Class<? extends Component>, Component> components = new HashMap<>();

  /**
   * @return unique integer identifier
   */
  public int id() {
    return id;
  }

  /** Attach a component to this entity (replaces existing of same type). */
  public <T extends Component> void add(T component) {
    components.put(component.getClass(), component);
  }

  /** Retrieve a component of the requested type (null if missing). */
  @SuppressWarnings("unchecked")
  public <T extends Component> T get(Class<T> type) {
    return (T) components.get(type);
  }

  /**
   * @return true when this entity owns a component of the given type.
   */
  public boolean has(Class<? extends Component> type) {
    return components.containsKey(type);
  }
}
