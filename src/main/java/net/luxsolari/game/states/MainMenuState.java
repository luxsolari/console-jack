package net.luxsolari.game.states;

import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.IOException;
import java.util.logging.Logger;

import net.luxsolari.engine.render.LayerRenderer;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.*;

/**
 * Represents the main menu state of the game. This state handles the display and interaction of the
 * main menu interface. Implements LoopableState to integrate with the game's state management
 * system.
 */
public class MainMenuState implements LoopableState {

  private static final String TAG = MainMenuState.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

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
    if (!RenderSubsystem.getInstance().running()
        || RenderSubsystem.getInstance().mainScreen().get() == null) {
      return;
    }

    try {
      KeyStroke ks = RenderSubsystem.getInstance().mainScreen().get().pollInput();
      if (ks == null) return;

      if (ks.getKeyType() == KeyType.Character) {
        char c = Character.toUpperCase(ks.getCharacter());
        switch (c) {
          case 'G', '\r' -> MasterSubsystem.getInstance().stateManager().replace(new GameplayState());
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

    // Menu lines
    String[] lines = {"Main Menu", "Press G to start game", "Press Q to quit"};

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
        0, boxX1, boxY1, boxX2, boxY2, TextColor.ANSI.WHITE, LayerRenderer.DEFAULT_BG);
  }

  @Override
  public void end() {
    LOGGER.info("Main menu ended");
  }
}
