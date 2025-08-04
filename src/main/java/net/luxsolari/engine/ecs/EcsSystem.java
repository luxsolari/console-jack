package net.luxsolari.engine.ecs;

/**
 * Functional interface for logic systems that operate on an {@link EntityPool} each engine tick.
 * <p>
 *     Unlike heavyweight engine {@code Subsystem}s, an {@code EcsSystem} owns no thread and carries
 *     no lifecycle methods. It is invoked by the master loop once per update cycle to transform
 *     component data into behaviour.
 * </p>
 */
@FunctionalInterface
public interface EcsSystem {
    /**
     * @param dt   time elapsed since the previous update (seconds)
     * @param pool shared pool containing all active entities
     */
    void update(double dt, EntityPool pool);
}
