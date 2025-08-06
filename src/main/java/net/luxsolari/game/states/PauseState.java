package net.luxsolari.game.states;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.logging.Logger;
import net.luxsolari.engine.manager.InputManager;
import net.luxsolari.engine.manager.RenderManager;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.internal.MasterSubsystem;

/** Pause overlay state. "Escape" or "P" resumes gameplay. "Q" quits to main menu (clears stack). */
public class PauseState implements LoopableState {

  private static final String TAG = PauseState.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  @Override
  public void start() {
    LOGGER.info("Pause menu opened");
  }

  @Override
  public void pause() {
    // not applicable
  }

  @Override
  public void resume() {
    LOGGER.info("Pause menu resumed");
  }

  @Override
  public void handleInput() {
    if (!renderReady()) {
      return;
    }
    KeyStroke ks = InputManager.poll();
    if (ks == null) return;
    if (ks.getKeyType() == KeyType.EOF) {
      MasterSubsystem.INSTANCE.stop();
      return;
    }

    if (ks.getKeyType() == KeyType.Character) {
      char c = Character.toUpperCase(ks.getCharacter());
      switch (c) {
        case 'P', '\u001B' -> MasterSubsystem.INSTANCE.stateManager().pop(); // resume
        case 'Q' -> {
          // clear to main menu
          MasterSubsystem.INSTANCE.stateManager().clear();
          MasterSubsystem.INSTANCE.stateManager().push(new MainMenuState());
        }
        default -> {}
      }
    } else if (ks.getKeyType() == KeyType.Escape) {
      MasterSubsystem.INSTANCE.stateManager().pop();
    }
  }

  @Override
  public void update() {}

  @Override
  public void render() {
    RenderManager.clear(RenderManager.UI_LAYER);
    if (!renderReady()) return;
    String[] lines = {"Paused", "Press P to resume", "Press Q to quit to main menu"};
    RenderManager.drawCenteredTextBlock(RenderManager.UI_LAYER, lines, true);
  }

  @Override
  public void end() {
    LOGGER.info("Pause menu closed");
  }
}
