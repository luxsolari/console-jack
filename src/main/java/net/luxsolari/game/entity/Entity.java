package net.luxsolari.game.entity;

import net.luxsolari.engine.systems.RenderSubsystem;

/**
 * Base class for all game entities that can be placed on the screen. This class provides core
 * functionality for position, size, and visibility.
 */
public abstract class Entity {
  protected EntityPosition position;
  protected float sizeWidth;
  protected float sizeHeight;
  protected boolean isVisible;
  private static final int ENTITY_LAYER = 0; // Default layer for entities

  /**
   * Creates a new Entity with the specified position and size.
   *
   * @param x The x-coordinate of the entity
   * @param y The y-coordinate of the entity
   * @param width The width of the entity
   * @param height The height of the entity
   */
  public Entity(float x, float y, float width, float height) {
    this.position = new EntityPosition((int) x, (int) y);
    this.sizeWidth = width;
    this.sizeHeight = height;
    this.isVisible = true;
  }

  /**
   * Creates a new Entity with the specified position and size.
   *
   * @param position The position of the entity
   * @param width The width of the entity
   * @param height The height of the entity
   */
  public Entity(EntityPosition position, float width, float height) {
    this.position = position;
    this.sizeWidth = width;
    this.sizeHeight = height;
    this.isVisible = true;
  }

  /**
   * Updates the entity's state. This method should be called every frame.
   *
   * @param deltaTime The time elapsed since the last update in seconds
   */
  public abstract void update(float deltaTime);

  /** Renders the entity to the screen. */
  public void render() {
    // Use the RenderSubsystem to render this entity
    RenderSubsystem.getInstance().renderEntity(ENTITY_LAYER, this);
  }

  // Getters and setters
  public float getX() {
    return position.x();
  }

  public void setX(float x) {
    this.position = new EntityPosition((int) x, position.y());
  }

  public float getY() {
    return position.y();
  }

  public void setY(float y) {
    this.position = new EntityPosition(position.x(), (int) y);
  }

  public EntityPosition getPosition() {
    return position;
  }

  public void setPosition(EntityPosition position) {
    this.position = position;
  }

  public float getWidth() {
    return sizeWidth;
  }

  public void setWidth(float width) {
    this.sizeWidth = width;
  }

  public float getHeight() {
    return sizeHeight;
  }

  public void setHeight(float height) {
    this.sizeHeight = height;
  }

  public boolean isVisible() {
    return isVisible;
  }

  public void setVisible(boolean visible) {
    this.isVisible = visible;
  }
}
