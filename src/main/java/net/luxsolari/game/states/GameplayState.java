package net.luxsolari.game.states;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.logging.Logger;
import net.luxsolari.engine.input.InputFacade;
import net.luxsolari.engine.render.LayerRenderer;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.internal.MasterSubsystem;

/** Simple placeholder gameplay state used to demonstrate state transitions. */
public class GameplayState implements LoopableState {

  private static final String TAG = GameplayState.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  @Override
  public void start() {
    LOGGER.info("Gameplay started");
  }

  @Override
  public void pause() {
    LOGGER.info("Gameplay paused");
  }

  @Override
  public void resume() {
    LOGGER.info("Gameplay resumed");
  }

  @Override
  public void handleInput() {
    if (!renderReady()) {
      return;
    }
    KeyStroke keyStroke = InputFacade.poll();
    if (keyStroke == null) {
      return;
    }
    if (keyStroke.getKeyType() == KeyType.EOF) {
      MasterSubsystem.getInstance().stop();
      return;
    }

    if (keyStroke.getKeyType() == KeyType.Character) {
      switch (Character.toUpperCase(keyStroke.getCharacter())) {
        case 'P' ->
            // push pause state
            MasterSubsystem.getInstance().stateManager().push(new PauseState());
        case 'Q' -> {
          // also push pause state
          MasterSubsystem.getInstance().stateManager().push(new PauseState());
        }
        default -> {}
      }
    }
    if (keyStroke.getKeyType() == KeyType.Escape) {
      // Esc behaves like P: open pause menu
      MasterSubsystem.getInstance().stateManager().push(new PauseState());
    }
  }

  @Override
  public void update() {
    // game logic placeholder
  }

  @Override
  public void render() {
    LayerRenderer.clear(LayerRenderer.UI_LAYER);
    if (!renderReady()) return;
    String[] lines = {" Gameplay state ", "Press P or Q", "or Esc to pause"};
    LayerRenderer.drawCenteredTextBlock(LayerRenderer.UI_LAYER, lines, true);
  }

  @Override
  public void end() {
    LOGGER.info("Gameplay ended");
  }
}
