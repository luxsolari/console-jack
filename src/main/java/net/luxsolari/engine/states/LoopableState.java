package net.luxsolari.engine.states;

import net.luxsolari.engine.systems.RenderSubsystem;

/**
 * This is an interface that will be implemented by the different states of the game loop. Each
 * state will implement the core methods of every state, such as update, render, and handleInput.
 */
public interface LoopableState {

  /**
   * This method sets up the state of the game. It should be called once when the state is loaded.
   */
  void start();

  /** This method will pause the state of the game. It should be called when the state is paused. */
  void pause();

  /**
   * This method will resume the state of the game. It should be called when the state is resumed.
   */
  void resume();

  /**
   * This method will handle the input of the game. In a text-based game, input should be handled
   * first, and then the game state should be updated and rendered.
   */
  void handleInput();

  /**
   * This method will update the state of the game. This happens second, since the inputs from the
   * player will determine possible changes in the game state.
   */
  void update();

  /**
   * This method will render the state of the game. This happens last, since the game state should
   * be updated before it is rendered.
   */
  void render();

  /**
   * This method will clean up the state of the game. It should be called once when the state is
   * unloaded.
   */
  void end();

  /**
   * Convenience helper available to every state: returns {@code true} when the {@link
   * RenderSubsystem} is running <em>and</em> its main {@code Screen} reference has been
   * initialised. States can use this to early-exit input or render logic in a multi-threaded
   * startup scenario without duplicating the same checks.
   */
  default boolean renderReady() {
    return RenderSubsystem.getInstance().ready();
  }
}
