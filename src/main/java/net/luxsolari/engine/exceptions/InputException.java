package net.luxsolari.engine.exceptions;

/**
 * Exception thrown when input operations fail.
 * This includes keyboard input processing, input polling, or input subsystem errors.
 */
public class InputException extends EngineException {
  private static final long serialVersionUID = 1L;
  /**
   * Creates a new InputException with a descriptive message.
   *
   * @param message descriptive message about the input failure
   */
  public InputException(String message) {
    super(message);
  }

  /**
   * Creates a new InputException with a message and underlying cause.
   *
   * @param message descriptive message about the input failure
   * @param cause   the underlying cause of the input failure
   */
  public InputException(String message, Throwable cause) {
    super(message, cause);
  }
}
