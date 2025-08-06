package net.luxsolari.engine.input;

import com.googlecode.lanterna.input.KeyStroke;
import net.luxsolari.engine.systems.internal.InputSubsystem;

/**
 * Public façade for the internal {@link InputSubsystem}.
 *
 * <p>External game logic can use this utility class to query user input without directly depending
 * on the internal subsystem implementation. This keeps the subsystem encapsulated while providing a
 * simple, stateless API surface.
 */
public final class InputFacade {

  // utility class – no instances
  private InputFacade() {}

  /** Returns {@code true} if the input subsystem is initialized and ready to be polled. */
  public static boolean ready() {
    return InputSubsystem.getInstance().ready();
  }

  /**
   * Non-blocking poll for the next {@link KeyStroke}. Returns {@code null} when there is no pending
   * input or the subsystem is not ready.
   */
  public static KeyStroke poll() {
    return InputSubsystem.getInstance().poll();
  }
}
