package net.luxsolari.engine.systems.internal;

import com.googlecode.lanterna.input.KeyStroke;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import net.luxsolari.engine.systems.Subsystem;

/**
 * Input subsystem implemented as an enum singleton (see {@link #INSTANCE}) using the pattern from
 * Effective Java Item 3. It polls keyboard input from the {@link RenderSubsystem} and exposes
 * lifecycle hooks via the {@link net.luxsolari.engine.systems.Subsystem} contract.
 */
public enum InputSubsystem implements Subsystem {
  INSTANCE;

  private static final String TAG = InputSubsystem.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private boolean running = false;

  private final Deque<KeyStroke> inputQueue = new ArrayDeque<>();
  private final ReentrantLock lock = new ReentrantLock();

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
      // Block and wait until the RenderSubsystem is fully initialized.
      RenderSubsystem.INSTANCE.getInitializedFuture().get();
    } catch (InterruptedException | ExecutionException e) {
      LOGGER.severe("[%s] Failed to wait for RenderSubsystem: %s".formatted(TAG, e.getMessage()));
      Thread.currentThread().interrupt(); // Preserve the interrupted status
      return; // Exit if we can't initialize
    }

    while (running) {
      try {
        // Since we waited for initialization, we can now safely get the screen.
        KeyStroke keyStroke = RenderSubsystem.INSTANCE.mainScreen().get().readInput();
        if (keyStroke != null) {
          lock.lock();
          try {
            inputQueue.addLast(keyStroke);
          } finally {
            lock.unlock();
          }
        }
      } catch (IOException e) {
        LOGGER.severe("[%s] Error reading input: %s".formatted(TAG, e.getMessage()));
      }
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
   * Polls and returns the latest {@link KeyStroke} from the input queue. Returns {@code null} if
   * the queue is empty or the subsystem is not ready.
   */
  public KeyStroke poll() {
    KeyStroke keyStroke;
    try {
      lock.lock();
      try {
        keyStroke = inputQueue.pollFirst();
      } finally {
        lock.unlock();
      }
    } catch (IllegalMonitorStateException e) {
      keyStroke = null;
    }
    return keyStroke;
  }
}
