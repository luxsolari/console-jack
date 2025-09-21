package net.luxsolari.engine.ui;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Simple object pool for UI components to reduce garbage collection pressure.
 * This is particularly useful for frequently created/destroyed UI elements.
 * 
 * Note: This is a basic implementation. For production use, consider more
 * sophisticated pooling strategies with size limits and cleanup mechanisms.
 */
public class UIObjectPool<T> {

  private final Queue<T> pool = new ConcurrentLinkedQueue<>();
  private final Supplier<T> factory;
  private final int maxSize;

  /**
   * Creates a new object pool with the specified factory and maximum size.
   *
   * @param factory the factory function to create new objects
   * @param maxSize the maximum number of objects to keep in the pool
   */
  public UIObjectPool(Supplier<T> factory, int maxSize) {
    this.factory = factory;
    this.maxSize = maxSize;
  }

  /**
   * Retrieves an object from the pool, or creates a new one if the pool is empty.
   *
   * @return an object ready for use
   */
  public T acquire() {
    T object = pool.poll();
    return object != null ? object : factory.get();
  }

  /**
   * Returns an object to the pool for reuse.
   * Objects should be reset to a clean state before being returned.
   *
   * @param object the object to return to the pool
   */
  public void release(T object) {
    if (object != null && pool.size() < maxSize) {
      pool.offer(object);
    }
  }

  /**
   * Gets the current number of objects in the pool.
   *
   * @return the pool size
   */
  public int size() {
    return pool.size();
  }

  /**
   * Clears all objects from the pool.
   */
  public void clear() {
    pool.clear();
  }
}