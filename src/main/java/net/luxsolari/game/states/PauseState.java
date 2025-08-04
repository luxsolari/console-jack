package net.luxsolari.game.states;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.IOException;
import java.util.logging.Logger;
import net.luxsolari.engine.render.LayerRenderer;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.MasterSubsystem;
import net.luxsolari.engine.systems.RenderSubsystem;

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
    try {
      KeyStroke ks = RenderSubsystem.getInstance().mainScreen().get().pollInput();
      if (ks == null) return;
      if (ks.getKeyType() == KeyType.EOF) {
        MasterSubsystem.getInstance().stop();
        return;
      }

      if (ks.getKeyType() == KeyType.Character) {
        char c = Character.toUpperCase(ks.getCharacter());
        switch (c) {
          case 'P', '\u001B' -> MasterSubsystem.getInstance().stateManager().pop(); // resume
          case 'Q' -> {
            // clear to main menu
            MasterSubsystem.getInstance().stateManager().clear();
            MasterSubsystem.getInstance().stateManager().push(new MainMenuState());
          }
          default -> {}
        }
      } else if (ks.getKeyType() == KeyType.Escape) {
        MasterSubsystem.getInstance().stateManager().pop();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void update() {}

  @Override
  public void render() {
    LayerRenderer.clear(LayerRenderer.UI_LAYER);
    if (!renderReady()) return;
    String[] lines = {"Paused", "Press P to resume", "Press Q to quit to main menu"};
    LayerRenderer.drawCenteredTextBlock(LayerRenderer.UI_LAYER, lines, true);
  }

  @Override
  public void end() {
    LOGGER.info("Pause menu closed");
  }
}
