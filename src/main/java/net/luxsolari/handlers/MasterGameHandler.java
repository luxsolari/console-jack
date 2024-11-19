package net.luxsolari.handlers;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.swing.AWTTerminalFontConfiguration;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFontConfiguration;
import net.luxsolari.exceptions.ResourceCleanupException;
import net.luxsolari.exceptions.ResourceInitializationException;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.util.logging.Logger;

public class MasterGameHandler implements SystemHandler {

  private static final String TAG = MasterGameHandler.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private static MasterGameHandler INSTANCE;

  private static final int TARGET_UPS = 4;  // 4 updates per second
  private static final int TARGET_FPS = 2; //  2 frames per second

  private static final long UPDATE_INTERVAL = 1000L / TARGET_UPS; // ~250ms per update
  private static final long RENDER_INTERVAL = 1000L / TARGET_FPS; // ~500ms per render

  // tracking statistics for FPS and UPS counters
  int frameCount = 0;
  int updateCount = 0;
  long lastStatsTime = System.nanoTime();
  int currentFPS = 0;
  int currentUPS = 0;

  private boolean running = false;
  private Screen screen;

  private MasterGameHandler() {
  }

  public static MasterGameHandler getInstance() {
    if (INSTANCE == null) {
      LOGGER.info("[%s] Creating new Master Game Handler instance".formatted(TAG));
      INSTANCE = new MasterGameHandler();
    }
    return INSTANCE;
  }

  @Override
  public void init() {
    LOGGER.info("[%s] Initializing Master Game Handler".formatted(TAG));
    try {
      // We want to make sure that we get a graphical terminal, no text terminal for us, since we're making a game that will be graphical,
      // even if it's text-graphics based.
      // The graphical terminal will be contained inside a Swing or AWT window provided by Lanterna library.
      DefaultTerminalFactory terminalFactory =
          new DefaultTerminalFactory().setForceTextTerminal(false).setPreferTerminalEmulator(true).setTerminalEmulatorTitle(TAG);

      // Set custom font for the terminal, load font from resources
      Font font =
          Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/fonts/FiraCode.ttf"))
              .deriveFont(Font.PLAIN, 18);
      SwingTerminalFontConfiguration fontConfig =
          new SwingTerminalFontConfiguration(true, AWTTerminalFontConfiguration.BoldMode.NOTHING, font);
      terminalFactory.setTerminalEmulatorFontConfiguration(fontConfig);

      int targetWidth = 1280;
      int targetHeight = 720;

      // calculate columns and rows based on font size using a rough scaling factor of 0.575 for width and 1.2 for height
      float fontPixelWidth = font.getSize() * .58f;   // width scaling factor - could be a configuration parameter for different fonts
      float fontPixelHeight = font.getSize() * 1.25f; // height scaling factor - same as above

      int columns = Math.round(targetWidth / fontPixelWidth);
      int rows = Math.round(targetHeight / fontPixelHeight);

      int finalPxWidth = Math.round(columns * fontPixelWidth);
      int finalPxHeight = Math.round(rows * fontPixelHeight);

      // add some padding to the terminal size
      columns = Math.max(1, columns);
      rows = Math.max(1, rows);

      terminalFactory.setInitialTerminalSize(new TerminalSize(columns, rows));

      this.screen = terminalFactory.createScreen();
      this.screen.startScreen();
      this.screen.setCursorPosition(null); // we don't need a cursor
      this.screen.doResizeIfNecessary();

      // set custom font size

    } catch (IOException | FontFormatException e) {
      LOGGER.severe("[%s] Error while initializing Master Game Handler: %s".formatted(TAG, e.getMessage()));
      throw new ResourceInitializationException("Error while initializing Master Game Handler", e);
    }
    this.start();
  }

  @Override
  public void start() {
    LOGGER.info("[%s] Starting Master Game Handler".formatted(TAG));
    running = true;
  }

  @Override
  public void update() {

    long previousUpdateTime = System.nanoTime();
    long previousRenderTime = System.nanoTime();
    long updateLag = 0;

    while (running) {
      try {
        long currentTime = System.nanoTime();

        if (currentTime - lastStatsTime >= 1_000_000_000) {
          currentFPS = frameCount;
          currentUPS = updateCount;
          frameCount = 0;
          updateCount = 0;
          lastStatsTime = currentTime;
        }

        // calculate elapsed time since last update/render
        long elapsedTime = currentTime - previousUpdateTime;
        updateLag += elapsedTime;
        previousUpdateTime = currentTime;

        // update game logic at fixed rate
        while (running && (updateLag >= UPDATE_INTERVAL * 1_000_000)) {
          // INPUT HANDLING SECTION //
          // poll for key presses
          KeyStroke keyStroke = this.screen.pollInput();
          if (keyStroke != null) {
            if ((keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter().toString().equalsIgnoreCase("q")) ||
                keyStroke.getKeyType() == KeyType.EOF) {
              this.stop();
            }
          }
          // END INPUT HANDLING SECTION //

          // GAME LOGIC SECTION //
          // I know what you're thinking, but since this is scaffolding code, it's going to get replaced
          // with actual game logic in the future. Don't worry about it.
          // For now, we just "simulate" game logic by sleeping for a bit.
          // This is a common pattern in game development, where you have a game loop that runs at a fixed rate, and you update
          // the game state at each iteration of the loop. This is called the "game loop" pattern.
          // In this case, we're running the game loop at 30 frames per second (FPS), which means we update the game state 30 times per second.
          // And we're running the game logic at 60 updates per second (UPS), which means we update the game state 60 times per second.

          // simulate work by drawing a card from a deck with a random value in the range [1, 19] using lanterna
          // this is just a placeholder for actual game logic
          // first, draw a 3x5 rectangle with white color representing the card border and black as its inner color.
          // then, draw the card value in the center of the rectangle.

          TextColor white = TextColor.ANSI.WHITE;
          TextColor black = TextColor.ANSI.BLACK;

          TextCharacter borderCharacter = new TextCharacter(' ', white, white);
          TextCharacter innerCharacter = new TextCharacter(' ', black, black);
          for (int row = 0; row < 7; row++) {
            for (int column = 0; column < 9; column++) {
              if (row == 0 || row == 6 || column == 0 || column == 8) {
                screen.setCharacter(column + 2, row + 2, borderCharacter);
              } else {
                screen.setCharacter(column + 2, row + 2, innerCharacter);
              }
            }
          }

          String[] cardValues = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
          int cardValueIndex = (int) (Math.random() * cardValues.length);
          String cardValue = cardValues[cardValueIndex];
          screen.newTextGraphics().putString(4, 4, cardValue);
          // random suit for the card (hearts, diamonds, clubs, spades)
          String[] suits = {"♥", "♦", "♣", "♠"};
          int suitIndex = (int) (Math.random() * suits.length);
          String suit = suits[suitIndex];
          screen.newTextGraphics().putString(8, 6, suit);
          String[] suitNames = {"HRT", "DIA", "CLB", "SPD"};
          screen.newTextGraphics().putString(4, 7, suitNames[suitIndex]);

          // END GAME LOGIC SECTION //

          updateCount++;
          updateLag -= UPDATE_INTERVAL * 1_000_000;
        }

        if (running && (currentTime - previousRenderTime >= RENDER_INTERVAL * 1_000_000)) {
          // RENDERING SECTION //
          // draw border around screen
          this.screen.doResizeIfNecessary(); // resize screen if necessary (e.g. when window is resized)

          for (int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
            TextColor red = TextColor.ANSI.RED;
            TextCharacter borderCharacter = new TextCharacter(' ', red, red);
            screen.setCharacter(column, 0, borderCharacter); // top border
            screen.setCharacter(column, screen.getTerminalSize().getRows() - 1, borderCharacter); // bottom border
          }
          for (int row = 0; row < screen.getTerminalSize().getRows(); row++) {
            TextColor red = TextColor.ANSI.RED;
            TextCharacter borderCharacter = new TextCharacter(' ', red, red);
            screen.setCharacter(0, row, borderCharacter); // left border
            screen.setCharacter(screen.getTerminalSize().getColumns() - 1, row, borderCharacter); // right border
          }

          // Display FPS/UPS in top-right corner
          String stats = String.format("FPS: %d | UPS: %d", currentFPS, currentUPS);
          screen.newTextGraphics().putString(
              screen.getTerminalSize().getColumns() - stats.length() - 2,
              1,
              stats
          );

          this.screen.refresh();
          // END RENDERING SECTION //
          frameCount++;
          previousRenderTime = currentTime;
        }

        Thread.yield(); // let other threads run while we sleep
      } catch (IOException e) {
        this.stop();
        LOGGER.severe("[%s] Error while running Master Game Handler: %s".formatted(TAG, e.getMessage()));
      }
    }
  }

  @Override
  public void stop() {
    LOGGER.info("[%s] Stopping Master Game Handler".formatted(TAG));
    try {
      this.screen.stopScreen();
      this.screen.close();
      running = false;
    } catch (IOException e) {
      LOGGER.severe("[%s] Error while stopping Master Game Handler: %s".formatted(TAG, e.getMessage()));
      throw new ResourceCleanupException("Error while stopping Master Game Handler", e);
    }
  }

  @Override
  public void cleanUp() {
    LOGGER.info("[%s] Cleaning up Master Game Handler".formatted(TAG));
  }
}
