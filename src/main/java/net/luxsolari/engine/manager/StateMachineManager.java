package net.luxsolari.engine.manager;

import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.internal.StateMachineSubsystem;

/**
 * Stateless utility facade that exposes the game state machine. All methods simply delegate to
 * {@link StateMachineSubsystem#INSTANCE}. This keeps the “Manager” pattern consistent: every
 * *Manager class in {@code net.luxsolari.engine.manager} is static & stateless.
 */
public final class StateMachineManager {

  // Utility class – no instances
  private StateMachineManager() {}

  /* -------------------------- Queries -------------------------- */

  /**
   * Returns true if the state machine has any states, false otherwise.
   *
   * @return true if states are present, false otherwise
   */
  public static boolean hasStates() {
    return StateMachineSubsystem.INSTANCE.hasStates();
  }

  /**
   * Returns the currently active state from the top of the state machine stack.
   *
   * @return the active game state
   */
  public static LoopableState active() {
    return StateMachineSubsystem.INSTANCE.active();
  }

  /* -------------------------- Commands -------------------------- */

  /**
   * Pushes a new state onto the state machine stack, making it the active state.
   *
   * @param state the new state to push
   */
  public static void push(LoopableState state) {
    StateMachineSubsystem.INSTANCE.push(state);
  }

  /**
   * Pops the currently active state from the state machine stack.
   */
  public static void pop() {
    StateMachineSubsystem.INSTANCE.pop();
  }

  /**
   * Replaces the currently active state with a new one.
   *
   * @param state the new state to replace the current one with
   */
  public static void replace(LoopableState state) {
    StateMachineSubsystem.INSTANCE.replace(state);
  }

  /**
   * Clears all states from the state machine.
   */
  public static void clear() {
    StateMachineSubsystem.INSTANCE.clear();
  }
}
