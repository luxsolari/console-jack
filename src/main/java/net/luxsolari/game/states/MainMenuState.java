package net.luxsolari.game.states;

import net.luxsolari.engine.states.LoopableState;

/**
 * Represents the main menu state of the game.
 * This state handles the display and interaction of the main menu interface.
 * Implements LoopableState to integrate with the game's state management system.
 */
public class MainMenuState implements LoopableState {
  @Override
  public void start() {
    System.out.println("Starting Main Menu State");
  }

  @Override
  public void pause() {
    System.out.println("Pausing Main Menu State");
  }

  @Override
  public void resume() {
    System.out.println("Resuming Main Menu State");
  }

  @Override
  public void handleInput() {
    System.out.println("Handling Input in Main Menu State");
  }

  @Override
  public void update() {
    System.out.println("Updating Main Menu State");
  }

  @Override
  public void render() {
    System.out.println("Rendering Main Menu State");
  }

  @Override
  public void end() {
    System.out.println("Ending Main Menu State");
  }
}
