package net.luxsolari.engine.systems.internal;

import com.googlecode.lanterna.input.KeyStroke;
import java.util.logging.Logger;
import net.luxsolari.engine.systems.Subsystem;

/**
 * Input subsystem implemented as an enum singleton (see {@link #INSTANCE}) using the
 * pattern from Effective Java Item 3. It polls keyboard input from the {@link RenderSubsystem}
 * and exposes lifecycle hooks via the {@link net.luxsolari.engine.systems.Subsystem} contract.
 */
public enum InputSubsystem implements Subsystem {
  INSTANCE;

  private static final String TAG = InputSubsystem.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  private boolean running = false;

  private InputSubsystem() {}

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
    try {
      while (running) {
        Thread.sleep(10);
      }
    } catch (InterruptedException e) {
      LOGGER.severe("[%s] Input update interrupted: %s".formatted(TAG, e.getMessage()));
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

  /** Returns true if the subsystem is running and the RenderSubsystem screen is available. */
  public boolean ready() {
    return running && RenderSubsystem.INSTANCE.ready();
  }

  /**
   * Polls and returns the latest {@link KeyStroke} from the terminal
   * screen or {@code null} if none is available / not ready.
   */
  public KeyStroke poll() {
    if (!ready()) {
      return null;
    }
    try {
      return RenderSubsystem.INSTANCE.mainScreen().get().pollInput();
    } catch (java.io.IOException e) {
      LOGGER.severe("[" + TAG + "] Error polling input: " + e.getMessage());
      return null;
    }
  }
}
