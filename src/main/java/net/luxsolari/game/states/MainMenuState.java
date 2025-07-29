package net.luxsolari.game.states;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.IOException;
import java.util.logging.Logger;
import net.luxsolari.engine.render.LayerRenderer;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.MasterSubsystem;
import net.luxsolari.engine.systems.RenderSubsystem;

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
    if (renderReady() || !this.running) {
      return;
    }

    try {
      KeyStroke ks = RenderSubsystem.getInstance().mainScreen().get().pollInput();
      if (ks == null) return;

      if (ks.getKeyType() == KeyType.Character) {
        char c = Character.toUpperCase(ks.getCharacter());
        switch (c) {
          case 'G', '\r' -> {
            MasterSubsystem.getInstance().stateManager().replace(new GameplayState());
            this.running = false;
          }
          case 'Q' -> MasterSubsystem.getInstance().stop();
          default -> {}
        }
      } else if (ks.getKeyType() == KeyType.EOF) {
        MasterSubsystem.getInstance().stop();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void update() {
    // Menu update placeholder
  }

  @Override
  public void render() {
    LayerRenderer.clear(0);
    if (!RenderSubsystem.getInstance().ready()) return;
    var screen = RenderSubsystem.getInstance().mainScreen().get();
    String[] lines = {"Main Menu", "Press G to start game", "Press Q to quit"};
    LayerRenderer.drawCenteredTextBlock(0, screen, lines, true);
  }

  @Override
  public void end() {
    LOGGER.info("Main menu ended");
    // Final cleanup of this state's layer when it is removed from the stack
    LayerRenderer.clear(0);
  }
}
