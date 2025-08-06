package net.luxsolari.engine.systems.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import net.luxsolari.engine.manager.RenderManager;
import net.luxsolari.engine.states.LoopableState;

/**
 * Internal subsystem that holds the game-state stack. Implemented as an enum singleton, following
 * the same pattern as other subsystems (RenderSubsystem, InputSubsystem, etc.).
 *
 * <p>All external code should interact with the state machine through the stateless facade
 * {@link net.luxsolari.engine.manager.StateMachineManager}. This class itself should <em>not</em>
 * be referenced directly by gameplay code.
 */
public enum StateMachineSubsystem {
  INSTANCE;

  private static final String TAG = StateMachineSubsystem.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  private final Deque<LoopableState> stack = new ArrayDeque<>();
  private final ReentrantLock lock = new ReentrantLock();

  /* -------------------------- Queries -------------------------- */

  /** @return {@code true} if at least one state exists in the stack. */
  public boolean hasStates() {
    lock.lock();
    try {
      return !stack.isEmpty();
    } finally {
      lock.unlock();
    }
  }

  /** @return The active (top) state, or {@code null} if the stack is empty. */
  public LoopableState active() {
    lock.lock();
    try {
      return stack.peek();
    } finally {
      lock.unlock();
    }
  }

  /* -------------------------- Commands -------------------------- */

  /**
   * Pushes a new state onto the stack. The previous active state (if any) is paused, and the new
   * state receives its {@link LoopableState#start()} callback.
   */
  public void push(LoopableState state) {
    if (state == null) {
      throw new IllegalArgumentException("State to push cannot be null");
    }
    lock.lock();
    try {
      if (!stack.isEmpty()) {
        stack.peek().pause();
      }
      stack.push(state);
      RenderManager.clearAll();
      LOGGER.fine(() -> "Pushed state: " + state.getClass().getSimpleName());
      state.start();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Pops the active state from the stack, invoking its {@link LoopableState#end()} callback. The
   * new top (if any) is resumed.
   */
  public void pop() {
    lock.lock();
    try {
      if (stack.isEmpty()) {
        return;
      }
      LoopableState removed = stack.pop();
      RenderManager.clearAll();
      LOGGER.fine(() -> "Popped state: " + removed.getClass().getSimpleName());
      removed.end();
      if (!stack.isEmpty()) {
        stack.peek().resume();
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Replaces the current active state with the given state. Equivalent to {@code pop(); push(newState)}.
   */
  public void replace(LoopableState state) {
    if (state == null) {
      throw new IllegalArgumentException("Replacement state cannot be null");
    }
    lock.lock();
    try {
      if (!stack.isEmpty()) {
        LoopableState removed = stack.pop();
        RenderManager.clearAll();
        LOGGER.fine(() ->
            "Replaced state: " + removed.getClass().getSimpleName() + " -> " + state.getClass().getSimpleName());
        removed.end();
      }
      stack.push(state);
      LOGGER.fine(() -> "Pushed state: " + state.getClass().getSimpleName());
      state.start();
    } finally {
      lock.unlock();
    }
  }

  /** Clears all states by calling {@link LoopableState#end()} on each in LIFO order. */
  public void clear() {
    lock.lock();
    try {
      while (!stack.isEmpty()) {
        LoopableState s = stack.pop();
        s.end();
      }
      RenderManager.clearAll();
    } finally {
      lock.unlock();
    }
  }
}
