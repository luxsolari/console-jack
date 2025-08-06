/**
 * Marker interface for all data components in the Entity-Component-System (ECS) architecture.
 *
 * <p>Components are simple, immutable data holders that define the properties or attributes of an
 * {@link Entity}. They do not contain behavior or logic. Systems operate on entities by querying
 * for entities with specific sets of components.
 *
 * <p>Example: A {@code Position} component might store coordinates, while a {@code Visual}
 * component might store rendering information. To add new data to an entity, implement a new
 * component type. <strong>Note:</strong> All ECS components in this project must implement this
 * interface.
 */
package net.luxsolari.engine.ecs;

public interface Component {}
