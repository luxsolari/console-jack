package net.luxsolari.engine.exceptions;

/**
 * Exception thrown when resources cannot be properly initialized.
 * This typically occurs during system startup or resource loading.
 */
public class ResourceInitializationException extends EngineException {
  private static final long serialVersionUID = 1L;
  /**
   * Creates a new ResourceInitializationException with a message and cause.
   *
   * @param message descriptive message about the initialization failure
   * @param cause   the underlying cause of the initialization failure
   */
  public ResourceInitializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
