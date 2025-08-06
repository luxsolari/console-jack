package net.luxsolari.engine.systems.internal;

import java.util.logging.Logger;
import net.luxsolari.engine.systems.Subsystem;

public class AudioSubsystem implements Subsystem {
  private static final String TAG = AudioSubsystem.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private static AudioSubsystem INSTANCE;

  private boolean running = false;

  private AudioSubsystem() {}

  public static AudioSubsystem getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new AudioSubsystem();
    }
    return INSTANCE;
  }

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
    LOGGER.info("[%s] Updating Audio Subsystem".formatted(TAG));
    try {
      Thread.sleep(1000);
      LOGGER.info("[%s] Audio update cycle complete".formatted(TAG));
    } catch (InterruptedException e) {
      LOGGER.severe("[%s] Audio update interrupted: %s".formatted(TAG, e.getMessage()));
      // Thread.currentThread().interrupt();
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
}
