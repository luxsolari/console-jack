package net.luxsolari.systems;

import java.util.logging.Logger;

public class InputSubsystem implements Subsystem {
  private static final String TAG = InputSubsystem.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private static InputSubsystem INSTANCE;

  private boolean running = false;

  private InputSubsystem() {}

  public static InputSubsystem getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new InputSubsystem();
    }
    return INSTANCE;
  }

  public boolean running() {
    return running;
  }

  @Override
  public void init() {
    LOGGER.info("[%s] Initializing Input Subsystem".formatted(TAG));
    this.start();
  }

  @Override
  public void start() {
    LOGGER.info("[%s] Starting Input Subsystem".formatted(TAG));
    running = true;
  }

  @Override
  public void update() {
    LOGGER.info("[%s] Updating Input Subsystem".formatted(TAG));
    try {
      Thread.sleep(1000);
      LOGGER.info("[%s] Input update cycle complete".formatted(TAG));
    } catch (InterruptedException e) {
      LOGGER.severe("[%s] Input update interrupted: %s".formatted(TAG, e.getMessage()));
      // Thread.currentThread().interrupt();
    }
  }

  @Override
  public void stop() {
    LOGGER.info("[%s] Stopping Input Subsystem".formatted(TAG));
    running = false;
  }

  @Override
  public void cleanUp() {
    LOGGER.info("[%s] Cleaning Up Input Subsystem".formatted(TAG));
  }
}
