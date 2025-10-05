package net.luxsolari.engine.exceptions;

/**
 * Exception thrown when the main game loop encounters an error.
 * This includes update cycle failures, timing issues, or thread coordination problems.
 */
public class GameLoopException extends EngineException {
  private  static final long serialVersionUID = 1L;
  /**
   * Creates a new GameLoopException with a descriptive message.
   *
   * @param message descriptive message about the game loop failure
   */
  public GameLoopException(String message) {
    super(message);
  }

  /**
   * Creates a new GameLoopException with a message and underlying cause.
   *
   * @param message descriptive message about the game loop failure
   * @param cause   the underlying cause of the game loop failure
   */
  public GameLoopException(String message, Throwable cause) {
    super(message, cause);
  }
}
