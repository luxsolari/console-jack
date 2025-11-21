package net.luxsolari.engine.input;

import com.googlecode.lanterna.input.KeyStroke;

/**
 * Wrapper containing both the raw keystroke and resolved command.
 * Returned by InputManager to provide both low-level and high-level input data.
 */
public record InputResult(KeyStroke keyStroke, InputCommand command) {

  /**
   * Returns true if this result has a resolved command.
   */
  public boolean hasCommand() {
    return command != null;
  }

  /**
   * Returns true if this result has a keystroke.
   */
  public boolean hasKeyStroke() {
    return keyStroke != null;
  }
}
