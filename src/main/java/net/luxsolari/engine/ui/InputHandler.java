package net.luxsolari.engine.ui;

import com.googlecode.lanterna.input.KeyStroke;

/**
 * Interface for UI components that can handle keyboard input.
 * Components implementing this interface can process KeyStroke events.
 */
public interface InputHandler {

  /**
   * Handles a keyboard input event.
   *
   * @param keyStroke the keyboard input to handle
   * @return true if the input was consumed and should not be passed to other handlers,
   *         false if the input was not handled and should continue propagating
   */
  boolean handleInput(KeyStroke keyStroke);
}