package net.luxsolari.game.states;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.IOException;
import java.util.logging.Logger;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.MasterSubsystem;
import net.luxsolari.engine.systems.RenderSubsystem;

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
    if (!RenderSubsystem.getInstance().running()
        || RenderSubsystem.getInstance().mainScreen().get() == null) {
      return;
    }
    try {
      KeyStroke keyStroke = RenderSubsystem.getInstance().mainScreen().get().pollInput();
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
          case 'Q' -> MasterSubsystem.getInstance().stop();
          default -> {}
        }
      }
      if (keyStroke.getKeyType() == KeyType.Escape) {
        // Esc behaves like P: open pause menu
        MasterSubsystem.getInstance().stateManager().push(new PauseState());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void update() {
    // game logic placeholder
  }

  @Override
  public void render() {
    // For now nothing extra; gameplay graphics would draw via RenderSubsystem layers
  }

  @Override
  public void end() {
    LOGGER.info("Gameplay ended");
  }
}
