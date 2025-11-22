package net.luxsolari.engine.input;

/**
 * Enumeration of all input commands recognized by the game.
 * Commands are context-independent and mapped to keys via {@link InputContext}.
 */
public enum InputCommand {
  // === Global Commands ===
  QUIT,           // Quit application
  BACK,           // Go back/cancel
  CONFIRM,        // Confirm selection
  CANCEL,         // Cancel action

  // === Navigation Commands ===
  NAVIGATE_UP,
  NAVIGATE_DOWN,
  NAVIGATE_LEFT,
  NAVIGATE_RIGHT,
  NAVIGATE_FIRST,
  NAVIGATE_LAST,

  // === Game State Commands ===
  PAUSE,
  RESUME,

  // === Blackjack Commands ===
  HIT,
  STAND,
  DOUBLE_DOWN,
  SPLIT,
  SURRENDER,

  // === Debug Commands ===
  DEBUG_CREATE_CARD,
  DEBUG_CLEAR_CARDS,
  DEBUG_TOGGLE,

  // === Audio Commands ===
  TOGGLE_SOUND,
  VOLUME_UP,
  VOLUME_DOWN,

  // === UI Commands ===
  TOGGLE_FULLSCREEN
}
