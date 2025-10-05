package net.luxsolari.engine.exceptions;

/**
 * Base exception class for all engine-related exceptions.
 * Provides a common exception hierarchy for better error handling and classification.
 * All custom exceptions in the engine package should extend this class.
 */
public class EngineException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Creates a new EngineException with a descriptive message.
   *
   * @param message descriptive message about the exception
   */
  public EngineException(String message) {
    super(message);
  }

  /**
   * Creates a new EngineException with a message and underlying cause.
   *
   * @param message descriptive message about the exception
   * @param cause   the underlying cause of the exception
   */
  public EngineException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new EngineException with an underlying cause.
   *
   * @param cause the underlying cause of the exception
   */
  public EngineException(Throwable cause) {
    super(cause);
  }
}
