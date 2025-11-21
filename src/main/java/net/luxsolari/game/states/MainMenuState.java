package net.luxsolari.game.states;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.logging.Logger;
import net.luxsolari.engine.input.InputCommand;
import net.luxsolari.engine.input.InputResult;
import net.luxsolari.engine.manager.AudioManager;
import net.luxsolari.engine.manager.InputManager;
import net.luxsolari.engine.manager.RenderManager;
import net.luxsolari.engine.manager.StateMachineManager;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.internal.MasterSubsystem;
import net.luxsolari.engine.ui.Menu;
import net.luxsolari.game.input.MainMenuInputContext;

/**
 * Represents the main menu state of the game. This state handles the display and interaction of the
 * main menu interface. Implements LoopableState to integrate with the game's state management
 * system.
 */
public class MainMenuState implements LoopableState {

  private static final String TAG = MainMenuState.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private boolean running = true;
  private Menu mainMenu;

  @Override
  public void start() {
    LOGGER.info("Main menu started");
    AudioManager.playBGM("menu_theme", true);

    // Set input context
    InputManager.setContext(new MainMenuInputContext());

    // Initialize the main menu
    mainMenu =
        new Menu("Console Jack")
            .addItem(
                "Start Game",
                () -> {
                  StateMachineManager.replace(new GameplayState());
                  this.running = false;
                })
            .addItem("Options", this::showOptions)
            .addItem("Quit", MasterSubsystem.INSTANCE::stop)
            .setBorder(true);

    mainMenu.focus();
  }

  @Override
  public void pause() {
    LOGGER.info("Main menu paused");
    AudioManager.stopBGM();
  }

  @Override
  public void resume() {
    LOGGER.info("Main menu resumed");
    AudioManager.playBGM("menu_theme", true);

    // Re-set input context
    InputManager.setContext(new MainMenuInputContext());

    // Force a complete redrawing when resuming to prevent artifacts
    if (mainMenu != null) {
      // First, clear all layers to ensure no artifacts
      RenderManager.clearAll();

      // Completely reset the focus state and then focus again
      mainMenu.resetFocus();
      mainMenu.focus();

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
    if (!renderReady() || !running || mainMenu == null) {
      return;
    }

    InputResult input = InputManager.pollCommand();
    if (input == null || input.command() == null) {
      return;
    }

    // Handle state-level commands
    if (input.command() == InputCommand.QUIT) {
      MasterSubsystem.INSTANCE.stop();
      return;
    }

    // Delegate menu commands to the menu
    mainMenu.handleCommand(input.command());
  }

  @Override
  public void update() {}

  @Override
  public void render() {
    // Ensure all UI layers are completely cleared before redrawing
    clearUILayers();

    if (!renderReady() || mainMenu == null) {
      return;
    }

    // Render the main menu
    mainMenu.render(RenderManager.UI_LAYER);
  }

  /**
   * Clears all UI layers to remove any rendering artifacts. Ensures that the UI layers are
   * completely cleared before rendering.
   */
  private void clearUILayers() {
    // Ensure all UI layers are completely cleared
    for (int layer = RenderManager.UI_LAYER; layer < RenderManager.getLayerCount(); layer++) {
      RenderManager.clear(layer);
    }
  }

  @Override
  public void end() {
    LOGGER.info("Main menu ended");
    AudioManager.stopBGM();

    // Clean up menu resources
    if (mainMenu != null) {
      mainMenu.unfocus();
      mainMenu = null;
    }
  }

  private void showOptions() {
    // Show a "Coming Soon" message for now
    LOGGER.info("Options menu requested - coming soon");

    // Create a temporary dialog to show the message - use a completely separate menu
    Menu optionsMenu =
        new Menu("Options")
            .setCenterOnScreen(true) // Ensure it's centered on the screen
            .addItem("Coming Soon!", () -> {})
            .addItem(
                "Back",
                () -> {
                  // Close the dialog and return to the main menu
                  StateMachineManager.pop();
                })
            .setBorder(true);

    // Define the layer for the option menu (much higher than the main menu to avoid any overlap)
    final int OPTIONS_LAYER = RenderManager.UI_LAYER + 3; // Use a layer with significant separation

    // Push a temporary state to show the dialog
    StateMachineManager.push(
        new LoopableState() {
          @Override
          public void start() {
            // First clear UI layers to ensure no artifacts remain
            clearUILayers();

            // Then clear all layers to ensure clean state
            RenderManager.clearAll();

            // Reset the main menu's focus state for when we return to it
            if (mainMenu != null) {
              mainMenu.resetFocus();
            }

            // Reset and focus the options menu
            optionsMenu.resetFocus();
            optionsMenu.focus();

            // Force a render immediately to show the options menu
            render();
          }

          @Override
          public void handleInput() {
            KeyStroke ks = InputManager.poll();
            if (ks != null) {
              if (ks.getKeyType() == KeyType.Escape) {
                // Escape key returns to main menu
                StateMachineManager.pop();
              } else {
                // Let the options menu handle other inputs
                optionsMenu.handleInput(ks);
              }
            }
          }

          @Override
          public void update() {}

          @Override
          public void render() {
            // First clear the UI layers to ensure no artifacts remain
            clearUILayers();

            // Then clear the options layer to prevent artifacts
            RenderManager.clear(OPTIONS_LAYER);

            // Render the options menu on its dedicated layer
            optionsMenu.render(OPTIONS_LAYER);
          }

          @Override
          public void end() {
            // Clean up the options menu completely
            optionsMenu.resetFocus();

            // Clear all layers to ensure no artifacts remain
            RenderManager.clearAll();

            // We don't need to redraw the main menu here as that will be handled by the resume()
            // method
            // of the MainMenuState when it becomes active again
          }

          @Override
          public void pause() {}

          @Override
          public void resume() {}
        });
  }
}
