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

  public static boolean hasStates() {
    return StateMachineSubsystem.INSTANCE.hasStates();
  }

  public static LoopableState active() {
    return StateMachineSubsystem.INSTANCE.active();
  }

  /* -------------------------- Commands -------------------------- */

  public static void push(LoopableState state) {
    StateMachineSubsystem.INSTANCE.push(state);
  }

  public static void pop() {
    StateMachineSubsystem.INSTANCE.pop();
  }

  public static void replace(LoopableState state) {
    StateMachineSubsystem.INSTANCE.replace(state);
  }

  public static void clear() {
    StateMachineSubsystem.INSTANCE.clear();
  }
}
