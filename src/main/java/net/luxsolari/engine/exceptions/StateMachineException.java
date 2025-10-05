package net.luxsolari.engine.exceptions;

/**
 * Exception thrown when state machine operations fail.
 * This includes state transitions, state activation, or state lifecycle errors.
 */
public class StateMachineException extends EngineException {
  private static final long serialVersionUID = 1L;
  /**
   * Creates a new StateMachineException with a descriptive message.
   *
   * @param message descriptive message about the state machine failure
   */
  public StateMachineException(String message) {
    super(message);
  }

  /**
   * Creates a new StateMachineException with a message and underlying cause.
   *
   * @param message descriptive message about the state machine failure
   * @param cause   the underlying cause of the state machine failure
   */
  public StateMachineException(String message, Throwable cause) {
    super(message, cause);
  }
}
