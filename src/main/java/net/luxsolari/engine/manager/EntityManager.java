package net.luxsolari.engine.manager;

import net.luxsolari.engine.ecs.Component;
import net.luxsolari.engine.ecs.EcsSystem;
import net.luxsolari.engine.ecs.Entity;
import net.luxsolari.engine.systems.internal.EntityCoordinator;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * <strong>Public API boundary for all entity and ECS-system operations.</strong>
 *
 * <p>Game code must use this class exclusively when creating, querying, or destroying entities,
 * and when registering ECS systems. Direct access to
 * {@link net.luxsolari.engine.systems.internal.EntityCoordinator} or
 * {@link net.luxsolari.engine.ecs.EntityPool} from outside the engine package is
 * strongly discouraged — those types are internal implementation details that may change
 * without notice.
 *
 * <p>This is a stateless static-utility façade: it holds no data of its own and simply
 * delegates every call to the {@link EntityCoordinator#INSTANCE} singleton. Keeping game
 * code behind this boundary means the underlying coordinator can be refactored freely
 * without touching any game-side call sites.
 *
 * <p><strong>Typical usage:</strong>
 * <pre>{@code
 * long id = EntityManager.create();
 * EntityManager.getById(id).ifPresent(e -> e.addComponent(new Position(0, 0)));
 * EntityManager.registerSystem(new MovementSystem());
 * // later…
 * EntityManager.destroy(id);
 * }</pre>
 */
public final class EntityManager {
    private static final String TAG = EntityManager.class.getSimpleName();
    private static final Logger LOGGER = Logger.getLogger(TAG);

    /**
     * Not instantiable — all methods are static.
     */
    private EntityManager() {
    }

    /**
     * Creates a new entity in the pool and returns its unique ID.
     *
     * @return the ID of the newly created entity
     */
    public static long create() {
        return EntityCoordinator.INSTANCE.getPool().create();
    }

    /**
     * Destroys the entity with the given ID, removing it and all its components from the pool.
     * Logs a warning if no entity with that ID exists.
     *
     * @param entityId the ID of the entity to destroy
     */
    public static void destroy(long entityId) {
        boolean removedSuccessfully = EntityCoordinator.INSTANCE.getPool().removeById(entityId);

        if (!removedSuccessfully) {
            LOGGER.warning("Entity with ID " + entityId + " does not exist and cannot be destroyed.");
        }
    }

    /**
     * Returns {@code true} if the entity with the given ID exists in the pool.
     *
     * @param entityId the entity ID to check
     * @return {@code true} if the entity is alive, {@code false} otherwise
     */
    public static boolean isValid(long entityId) {
        return EntityCoordinator.INSTANCE.getPool().isValid(entityId);
    }

    /**
     * Returns {@code true} if the entity with the given ID has been destroyed or never existed.
     * Convenience inverse of {@link #isValid(long)}.
     *
     * @param entityId the entity ID to check
     * @return {@code true} if the entity is absent from the pool
     */
    public static boolean isDestroyed(long entityId) {
        return !isValid(entityId);
    }

    /**
     * Destroys all entities that carry every one of the specified component types.
     * Useful for bulk cleanup (e.g., clearing all card sprites between hands).
     *
     * @param componentClasses one or more component types that an entity must have to be removed
     */
    @SafeVarargs
    public static void removeWith(Class<? extends Component>... componentClasses) {
        EntityCoordinator.INSTANCE.getPool().removeWith(componentClasses);
    }

    /**
     * Returns all live entities that carry every one of the specified component types.
     * This is the primary ECS query used by systems to find the entities they should process.
     *
     * @param componentClasses one or more component types that a returned entity must have
     * @return a list of matching entities; empty if none match
     */
    @SafeVarargs
    public static List<Entity> queryWith(Class<? extends Component>... componentClasses) {
        return EntityCoordinator.INSTANCE.getPool().with(componentClasses);
    }

    /**
     * Looks up a single entity by its ID.
     *
     * @param entityId the ID to look up
     * @return an {@link Optional} containing the entity, or empty if it does not exist
     */
    public static Optional<Entity> getById(long entityId) {
        return EntityCoordinator.INSTANCE.getPool().getById(entityId);
    }

    /**
     * Registers an ECS system with the coordinator so it receives updates on every game tick.
     * Systems are updated in registration order; register earlier for higher priority.
     *
     * @param system the system to register
     */
    public static void registerSystem(EcsSystem system) {
        EntityCoordinator.INSTANCE.registerSystem(system);
    }

    /**
     * Removes an ECS system from the coordinator so it no longer receives game-tick updates.
     * If the system is not currently registered, this method has no effect.
     *
     * @param system the system to unregister
     */
    public static void unregisterSystem(EcsSystem system) {
        EntityCoordinator.INSTANCE.unregisterSystem(system);
    }
}
