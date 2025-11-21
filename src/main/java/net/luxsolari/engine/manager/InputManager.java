package net.luxsolari.engine.manager;

import com.googlecode.lanterna.input.KeyStroke;
import net.luxsolari.engine.input.InputCommand;
import net.luxsolari.engine.input.InputContext;
import net.luxsolari.engine.input.InputResult;
import net.luxsolari.engine.input.KeyBinding;
import net.luxsolari.engine.systems.internal.InputSubsystem;

/**
 * Public façade for the internal {@link InputSubsystem}.
 *
 * <p>Provides command-based input handling with context-sensitive key bindings.
 * Game states set their InputContext and poll for InputResults containing both
 * the raw keystroke and resolved command.
 *
 * <p>Example usage:
 * <pre>
 * // In state's start():
 * InputManager.setContext(new MainMenuInputContext());
 *
 * // In state's handleInput():
 * InputResult input = InputManager.pollCommand();
 * if (input != null && input.command() == InputCommand.QUIT) {
 *     quit();
 * }
 * </pre>
 */
public final class InputManager {
  private static volatile InputContext currentContext = null;

  private InputManager() {}

  /**
   * Returns true if the input subsystem is ready to be polled.
   */
  public static boolean ready() {
    return InputSubsystem.INSTANCE.ready();
  }

  /**
   * Sets the current input context for command resolution.
   * Should be called by states in their start() or resume() methods.
   *
   * @param context the input context to use
   */
  public static void setContext(InputContext context) {
    currentContext = context;
  }

  /**
   * Gets the current input context.
   *
   * @return the current context, or null if none set
   */
  public static InputContext getContext() {
    return currentContext;
  }

  /**
   * Polls for input and returns both the keystroke and resolved command.
   * This is the primary method for game states to use.
   *
   * @return InputResult with keystroke and command, or null if no input
   */
  public static InputResult pollCommand() {
    KeyStroke keyStroke = poll();
    if (keyStroke == null) {
      return null;
    }

    InputCommand command = null;
    if (currentContext != null) {
      System.out.println(currentContext.getClass().getSimpleName() + " resolving keyStroke: " + keyStroke);
      KeyBinding binding = KeyBinding.fromKeyStroke(keyStroke);
      System.out.println("Resolving binding: " + binding);
      command = currentContext.resolve(binding);
      System.out.println("Resolved command: " + command);
    }

    return new InputResult(keyStroke, command);
  }

  /**
   * Low-level poll for raw keystroke.
   * Most code should use pollCommand() instead.
   *
   * @return the next keystroke, or null if none available
   */
  public static KeyStroke poll() {
    return InputSubsystem.INSTANCE.poll();
  }
}
