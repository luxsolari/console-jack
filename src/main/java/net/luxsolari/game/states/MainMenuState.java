package net.luxsolari.game.states;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.logging.Logger;
import net.luxsolari.engine.manager.InputManager;
import net.luxsolari.engine.manager.RenderManager;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.internal.MasterSubsystem;

/**
 * Represents the main menu state of the game. This state handles the display and interaction of the
 * main menu interface. Implements LoopableState to integrate with the game's state management
 * system.
 */
public class MainMenuState implements LoopableState {

  private static final String TAG = MainMenuState.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private boolean running = true;

  @Override
  public void start() {
    LOGGER.info("Main menu started");
  }

  @Override
  public void pause() {
    LOGGER.info("Main menu paused");
  }

  @Override
  public void resume() {
    LOGGER.info("Main menu resumed");
  }

  @Override
  public void handleInput() {
    if (!renderReady() || !running) {
      return;
    }

    KeyStroke ks = InputManager.poll();
    if (ks == null) return;

    if (ks.getKeyType() == KeyType.Character) {
      char c = Character.toUpperCase(ks.getCharacter());
      switch (c) {
        case 'G', '\r' -> {
          MasterSubsystem.INSTANCE.stateManager().replace(new GameplayState());
          this.running = false;
        }
        case 'Q' -> MasterSubsystem.INSTANCE.stop();
        default -> {}
      }
    } else if (ks.getKeyType() == KeyType.EOF) {
      MasterSubsystem.INSTANCE.stop();
    }
  }

  @Override
  public void update() {
    // Menu update placeholder
  }

  @Override
  public void render() {
    RenderManager.clear(RenderManager.UI_LAYER);
    if (!renderReady()) return;
    String[] lines = {"Main Menu", "Press G to start game", "Press Q to quit"};
    RenderManager.drawCenteredTextBlock(RenderManager.UI_LAYER, lines, true);
  }

  @Override
  public void end() {
    LOGGER.info("Main menu ended");
  }
}
