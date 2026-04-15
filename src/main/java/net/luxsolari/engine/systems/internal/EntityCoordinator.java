package net.luxsolari.engine.systems.internal;

import net.luxsolari.engine.ecs.EcsSystem;
import net.luxsolari.engine.ecs.EntityPool;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Internal coordinator that owns the shared {@link EntityPool} and the ordered list of
 * {@link EcsSystem} instances. Implemented as an enum singleton that owns no thread;
 * it is driven on every game-loop tick by {@link MasterSubsystem}.
 *
 * <p>All external code should interact with entities through the stateless facade
 * {@link net.luxsolari.engine.manager.EntityManager}. This class itself should
 * <em>not</em> be referenced directly by game code.
 */
public enum EntityCoordinator {
    INSTANCE;
    private static final String TAG = EntityCoordinator.class.getSimpleName();
    private static final Logger LOGGER = Logger.getLogger(TAG);
    private final EntityPool pool = new EntityPool();
    private final List<EcsSystem> systems = new ArrayList<>();

    /**
     * Registers an ECS system so it will be updated on every coordinator tick.
     * Systems are updated in registration order during each call to {@link #update(double)}.
     *
     * @param system the system to register
     */
    public void registerSystem(EcsSystem system) {
        systems.add(system);
    }

    /**
     * Removes an ECS system so it will no longer be updated on coordinator ticks.
     * If the system is not currently registered, this method has no effect.
     *
     * @param system the system to unregister
     */
    public void unregisterSystem(EcsSystem system) {
        systems.remove(system);
    }

    /**
     * Advances all registered ECS systems by one tick.
     * Each system receives the shared {@link EntityPool} and the elapsed time so it can
     * read and mutate component data for the current frame.
     *
     * @param dt elapsed time in seconds since the last update
     */
    public void update(double dt) {
        for (EcsSystem system : systems) {
            system.update(dt, this.pool);
        }
    }

    /**
     * Returns the shared {@link EntityPool} managed by this coordinator.
     * Prefer accessing entities through {@link net.luxsolari.engine.manager.EntityManager}
     * rather than calling this method directly from game code.
     *
     * @return the coordinator's entity pool
     */
    public EntityPool getPool() {
        return pool;
    }
}
