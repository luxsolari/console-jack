package net.luxsolari.game.states;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.IOException;
import java.util.logging.Logger;

import net.luxsolari.engine.render.LayerRenderer;
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
    LayerRenderer.clear(0);

    // Menu lines
    String[] lines = {
            "Gameplay state",
            "Press Q pause state"
    };

    // Obtain screen safely – may be null during early startup
    var screenRef = RenderSubsystem.getInstance().mainScreen().get();
    if (screenRef == null) {
      // Render subsystem not ready; skip rendering this frame
      return;
    }

    int cols = screenRef.getTerminalSize().getColumns();
    int rows = screenRef.getTerminalSize().getRows();

    // Determine starting Y to vertically center the block
    int startY = rows / 2 - lines.length / 2;

    // Draw each line centered horizontally
    int longestLen = 0;
    for (String s : lines) {
      longestLen = Math.max(longestLen, s.length());
    }

    for (int i = 0; i < lines.length; i++) {
      String s = lines[i];
      int x = (cols - s.length()) / 2;
      int y = startY + i;

      if (i == 0) {
        LayerRenderer.putStringRainbow(0, x, y, s);
      } else {
        LayerRenderer.putString(0, x, y, s, LayerRenderer.DEFAULT_FG, LayerRenderer.DEFAULT_BG);
      }
    }

    // Draw border around the menu block with 1-char padding
    int boxX1 = (cols - longestLen) / 2 - 2;
    int boxY1 = startY - 2;
    int boxX2 = boxX1 + longestLen + 3; // +3 because left padding + content + right padding
    int boxY2 = boxY1 + lines.length + 3; // +3 because top padding + content + bottom padding

    LayerRenderer.drawBox(
            0,
            boxX1,
            boxY1,
            boxX2,
            boxY2,
            TextColor.ANSI.WHITE,
            LayerRenderer.DEFAULT_BG);
  }

  @Override
  public void end() {
    LOGGER.info("Gameplay ended");
  }
}
