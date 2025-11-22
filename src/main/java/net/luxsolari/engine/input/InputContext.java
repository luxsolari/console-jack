package net.luxsolari.engine.input;

import java.util.Map;

/**
 * Defines context-specific key bindings for a game state.
 * Each state provides its own InputContext to map keys to commands.
 */
public interface InputContext {

  /**
   * Returns the key-to-command mapping for this context.
   *
   * @return immutable map of key bindings to commands
   */
  Map<KeyBinding, InputCommand> getBindings();

  /**
   * Resolves a key binding to its command in this context.
   *
   * @param binding the key binding to resolve
   * @return the command, or null if no binding exists
   */
  default InputCommand resolve(KeyBinding binding) {
    return getBindings().get(binding);
  }

  /**
   * Returns the name of this context for debugging.
   */
  default String getName() {
    return this.getClass().getSimpleName();
  }
}
