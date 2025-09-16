package net.luxsolari.engine.exceptions;

/**
 * Exception thrown when resources cannot be properly cleaned up.
 * This typically occurs during system shutdown or resource disposal.
 */
public class ResourceCleanupException extends RuntimeException {
  
  /**
   * Creates a new ResourceCleanupException with a message and cause.
   *
   * @param message descriptive message about the cleanup failure
   * @param cause   the underlying cause of the cleanup failure
   */
  public ResourceCleanupException(String message, Throwable cause) {
    super(message, cause);
  }
}
