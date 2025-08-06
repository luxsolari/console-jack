package net.luxsolari.engine.systems.internal;

import java.util.logging.Logger;

import com.googlecode.lanterna.input.KeyStroke;
import net.luxsolari.engine.systems.interfaces.Subsystem;

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
    return running && RenderSubsystem.getInstance().ready();
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
      return RenderSubsystem.getInstance().mainScreen().get().pollInput();
    } catch (java.io.IOException e) {
      LOGGER.severe("[" + TAG + "] Error polling input: " + e.getMessage());
      return null;
    }
  }
}
