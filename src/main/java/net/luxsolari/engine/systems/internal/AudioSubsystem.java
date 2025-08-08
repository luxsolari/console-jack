package net.luxsolari.engine.systems.internal;

import java.util.logging.Logger;
import net.luxsolari.engine.systems.Subsystem;

/**
 * Audio subsystem implemented as an enum singleton (see {@link #INSTANCE}) following Effective Java
 * Item 3. It manages audio initialization, lifecycle control and update logic for all in-game
 * sounds.
 */
public enum AudioSubsystem implements Subsystem {
  INSTANCE;

  private static final String TAG = AudioSubsystem.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private boolean running = false;

  public boolean running() {
    return running;
  }

  @Override
  public void init() {
    LOGGER.info("[%s] Initializing Audio Subsystem".formatted(TAG));
    this.start();
  }

  @Override
  public void start() {
    LOGGER.info("[%s] Starting Audio Subsystem".formatted(TAG));
    running = true;
  }

  @Override
  public void update() {
    try {
      // Block and wait until the RenderSubsystem is fully initialized.
      RenderSubsystem.INSTANCE.getInitializedFuture().get();
    } catch (Exception e) {
      LOGGER.severe("[%s] Failed to wait for RenderSubsystem: %s".formatted(TAG, e.getMessage()));
      Thread.currentThread().interrupt(); // Preserve the interrupted status
      return; // Exit if we can't initialize
    }

    while (running) {
      // TODO Add audio update logic here.
      try {
        Thread.sleep(16); // ~60fps, prevents busy-wait
      } catch (InterruptedException ie) {
        LOGGER.warning("[%s] Audio update loop interrupted: %s".formatted(TAG, ie.getMessage()));
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  @Override
  public void stop() {
    LOGGER.info("[%s] Stopping Audio Subsystem".formatted(TAG));
    running = false;
  }

  @Override
  public void cleanUp() {
    LOGGER.info("[%s] Cleaning Up Audio Subsystem".formatted(TAG));
  }

  /** Returns true if the subsystem is running and the RenderSubsystem screen is available. */
  public boolean ready() {
    return running && RenderSubsystem.INSTANCE.ready();
  }
}
