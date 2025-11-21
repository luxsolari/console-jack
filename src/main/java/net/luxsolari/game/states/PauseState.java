package net.luxsolari.game.states;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.logging.Logger;
import net.luxsolari.engine.input.InputCommand;
import net.luxsolari.engine.input.InputResult;
import net.luxsolari.engine.manager.InputManager;
import net.luxsolari.engine.manager.RenderManager;
import net.luxsolari.engine.manager.StateMachineManager;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.internal.MasterSubsystem;
import net.luxsolari.engine.ui.Menu;
import net.luxsolari.game.input.PauseInputContext;

/**
 * Pause overlay state that displays a menu with options to resume or quit.
 * Uses the Menu UI component for consistent styling and navigation.
 */
public class PauseState implements LoopableState {

  private static final String TAG = PauseState.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private Menu pauseMenu;

  @Override
  public void start() {
    LOGGER.info("Pause menu opened");

    // Set input context
    InputManager.setContext(new PauseInputContext());

    // Initialize the pause menu
    pauseMenu =
        new Menu("Paused")
            .addItem(
                "Resume",
                () -> {
                  StateMachineManager.pop();
                })
            .addItem(
                "Quit to Main Menu",
                () -> {
                  StateMachineManager.clear();
                  StateMachineManager.push(new MainMenuState());
                })
            .setBorder(true);

    pauseMenu.focus();
  }

  @Override
  public void pause() {
    LOGGER.info("Pause menu paused");
  }

  @Override
  public void resume() {
    LOGGER.info("Pause menu resumed");

    // Re-set input context
    InputManager.setContext(new PauseInputContext());

    // Force a complete redrawing when resuming to prevent artifacts
    if (pauseMenu != null) {
      // First, clear all layers to ensure no artifacts
      RenderManager.clearAll();

      // Completely reset the focus state and then focus again
      pauseMenu.resetFocus();
      pauseMenu.focus();

      // Force a redrawing after a short delay to ensure the screen is updated
      try {
        Thread.sleep(50); // Small delay to ensure the screen buffer is updated
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void handleInput() {
    if (!renderReady() || pauseMenu == null) {
      return;
    }

    InputResult input = InputManager.pollCommand();
    if (input == null || input.command() == null) {
      return;
    }

    // Handle state-level commands
    switch (input.command()) {
      case QUIT -> {
        MasterSubsystem.INSTANCE.stop();
        return;
      }
      case RESUME -> {
        StateMachineManager.pop();
        return;
      }
      case BACK -> {
        StateMachineManager.clear();
        StateMachineManager.push(new MainMenuState());
        return;
      }
    }

    // Delegate menu commands to the menu
    pauseMenu.handleCommand(input.command());
  }

  @Override
  public void update() {}

  @Override
  public void render() {
    // Ensure all UI layers are completely cleared before redrawing
    clearUILayers();

    if (!renderReady() || pauseMenu == null) {
      return;
    }

    // Render the pause menu
    pauseMenu.render(RenderManager.UI_LAYER);
  }

  @Override
  public void end() {
    LOGGER.info("Pause menu closed");

    // Clean up menu resources
    if (pauseMenu != null) {
      pauseMenu.unfocus();
      pauseMenu = null;
    }
  }

  /**
   * Clears all UI layers to remove any rendering artifacts.
   * Ensures that the UI layers are completely cleared before rendering.
   */
  private void clearUILayers() {
    // Ensure all UI layers are completely cleared
    for (int layer = RenderManager.UI_LAYER; layer < RenderManager.getLayerCount(); layer++) {
      RenderManager.clear(layer);
    }
  }
}
