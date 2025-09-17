package net.luxsolari.game.states;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.logging.Logger;
import net.luxsolari.engine.manager.AudioManager;
import net.luxsolari.engine.manager.InputManager;
import net.luxsolari.engine.manager.RenderManager;
import net.luxsolari.engine.manager.StateMachineManager;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.internal.MasterSubsystem;
import net.luxsolari.engine.ui.Menu;

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

    // Initialize the main menu
    mainMenu = new Menu("Main Menu")
        .addItem("Start Game", () -> {
          StateMachineManager.replace(new GameplayState());
          this.running = false;
        })
        .addItem("Options", this::showOptions)
        .addItem("Quit", () -> MasterSubsystem.INSTANCE.stop())
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
  }

  @Override
  public void handleInput() {
    if (!renderReady() || !running || mainMenu == null) {
      return;
    }

    KeyStroke ks = InputManager.poll();
    if (ks == null) {
      return;
    }

    // Handle EOF to quit
    if (ks.getKeyType() == KeyType.EOF) {
      MasterSubsystem.INSTANCE.stop();
      return;
    }

    // Delegate input handling to the menu
    mainMenu.handleInput(ks);
  }

  @Override
  public void update() {
    // Menu update placeholder
  }

  @Override
  public void render() {
    redrawLayers();
  }

  private void redrawLayers() {
    RenderManager.clear(RenderManager.UI_LAYER);
    if (!renderReady() || mainMenu == null) {
      return;
    }
    mainMenu.render(RenderManager.UI_LAYER);
  }

  @Override
  public void end() {
    LOGGER.info("Main menu ended");
    AudioManager.stopBGM();
  }

  private void showOptions() {
    // Placeholder for options menu - could push an OptionsState or show dialog
    LOGGER.info("Options menu requested - not implemented yet");
  }
}
