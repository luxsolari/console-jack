package net.luxsolari.handlers;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
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
              .deriveFont(Font.PLAIN, 20);
      SwingTerminalFontConfiguration fontConfig =
          new SwingTerminalFontConfiguration(true, AWTTerminalFontConfiguration.BoldMode.NOTHING, font);
      terminalFactory.setTerminalEmulatorFontConfiguration(fontConfig);

      int targetWidth = 800;
      int targetHeight = 600;

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
        // this "delta time" is used to update the game state and render the game screen and can be passed to the game logic
        // and rendering methods to make sure that the game runs at a consistent speed on different systems and hardware.
        long elapsedTime = currentTime - previousUpdateTime; // delta time in nanoseconds

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
          // For now, we just "simulate" game logic by doing some random drawing on the screen.
          // This is a common pattern in game development, where you have a game loop that runs at a fixed rate, and you update
          // the game state at each iteration of the loop. This is called the "game loop" pattern.
          // In this case, we're running the game logic at 4 updates per second (UPS), which means we update the game state 4 times per second.
          // And we're running the rendering at 2 frames per second (FPS), which means we render the game screen 2 times per second.
          // This is a very simple example, but it shows the basic structure of a game loop.

          // first, draw a 3x5 rectangle with white color representing the card border and black as its inner color.
          // then, draw the card value in the center of the rectangle.
          drawRandomCard();
          // END GAME LOGIC SECTION //

          updateCount++;
          updateLag -= UPDATE_INTERVAL * 1_000_000;
        }

        if (running && (currentTime - previousRenderTime >= RENDER_INTERVAL * 1_000_000)) {
          // RENDERING SECTION //
          // draw border around screen using special border characters and color it red
          if (this.screen.doResizeIfNecessary() != null) {
            this.screen.clear();
          }

          drawScreenBorders();
          displayStatsCounters();

          this.screen.refresh();
          // END RENDERING SECTION //
          frameCount++;
          previousRenderTime = currentTime;
        }

        // sleep for a short time to avoid busy-waiting. This is a standard game development pattern.
        // Specifying 1ms signals our intent to yield CPU time to other processes,
        // though the actual sleep time is subject to OS timer resolution.
        // noinspection BusyWait
        Thread.sleep(1);

      } catch (InterruptedException | IOException e) {
        this.stop();
        LOGGER.severe("[%s] Error while running Master Game Handler: %s".formatted(TAG, e.getMessage()));
      }
    }
  }

  private void displayStatsCounters() {
    // Display FPS/UPS in top-right corner
    String stats = "FPS: " + currentFPS + " | UPS: " + currentUPS;
    screen.newTextGraphics().putString(
        screen.getTerminalSize().getColumns() - stats.length() - 2,
        1,
        stats
    );
  }

  private void drawScreenBorders() {
    TextGraphics textGraphics = this.screen.newTextGraphics();
    // draw top border
    for (int i = 0; i < this.screen.getTerminalSize().getColumns(); i++) {
      textGraphics.setCharacter(i, 0, new TextCharacter('─', TextColor.ANSI.RED, TextColor.ANSI.BLACK));
    }

    // draw bottom border
    for (int i = 0; i < this.screen.getTerminalSize().getColumns(); i++) {
      textGraphics.setCharacter(i, this.screen.getTerminalSize().getRows() - 1, new TextCharacter('─', TextColor.ANSI.RED, TextColor.ANSI.BLACK));
    }

    // draw left border
    for (int i = 0; i < this.screen.getTerminalSize().getRows(); i++) {
      textGraphics.setCharacter(0, i, new TextCharacter('│', TextColor.ANSI.RED, TextColor.ANSI.BLACK));
    }

    // draw right border

    for (int i = 0; i < this.screen.getTerminalSize().getRows(); i++) {
      textGraphics.setCharacter(this.screen.getTerminalSize().getColumns() - 1, i, new TextCharacter('│', TextColor.ANSI.RED, TextColor.ANSI.BLACK));
    }

    // draw corners
    textGraphics.setCharacter(0, 0, new TextCharacter('┌'));
    textGraphics.setCharacter(this.screen.getTerminalSize().getColumns() - 1, 0, new TextCharacter('┐'));
    textGraphics.setCharacter(0, this.screen.getTerminalSize().getRows() - 1, new TextCharacter('└'));
    textGraphics.setCharacter(this.screen.getTerminalSize().getColumns() - 1, this.screen.getTerminalSize().getRows() - 1, new TextCharacter('┘'));
  }

  private void drawRandomCard() {

    // random value for the card (A, 2, 3, 4, 5, 6, 7, 8, 9, 10, J, Q, K)
    String[] cardValues = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "JOKER"};
    int cardValueIndex = (int) (Math.random() * cardValues.length);
    String cardValue = cardValues[cardValueIndex];

    // random suit for the card (hearts, diamonds, clubs, spades), represented by wide unicode characters
    String[] suits = {"♥", "♦", "♣", "♠", "?"};
    String[] suitNames = {"HRT", "DIA", "CLB", "SPD", "JOK"};
    int suitIndex = (int) (Math.random() * suits.length);
    String suit = suits[suitIndex];

    // draw card frame using special border characters and color the card red if it's a heart or diamond, white otherwise
    TextColor white = TextColor.ANSI.WHITE;
    TextColor black = TextColor.ANSI.BLACK;
    TextColor red = TextColor.ANSI.RED;
    TextColor green = TextColor.ANSI.GREEN;
    TextColor suitColor = (suitIndex < 2) ? red : black;

    TextCharacter topLeftCorner = new TextCharacter('┌', suitColor, white);
    TextCharacter topRightCorner = new TextCharacter('┐', suitColor, white);
    TextCharacter bottomLeftCorner = new TextCharacter('└', suitColor, white);
    TextCharacter bottomRightCorner = new TextCharacter('┘', suitColor, white);
    TextCharacter horizontalBorder = new TextCharacter('─', suitColor, white);
    TextCharacter verticalBorder = new TextCharacter('│', suitColor, white);

    // draw top border
    screen.newTextGraphics().setCharacter(2, 2, topLeftCorner);
    for (int i = 3; i < 10; i++) {
      screen.newTextGraphics().setCharacter(i, 2, horizontalBorder);
    }
    screen.newTextGraphics().setCharacter(10, 2, topRightCorner);

    // draw bottom border
    screen.newTextGraphics().setCharacter(2, 8, bottomLeftCorner);
    for (int i = 3; i < 10; i++) {
      screen.newTextGraphics().setCharacter(i, 8, horizontalBorder);
    }

    screen.newTextGraphics().setCharacter(10, 8, bottomRightCorner);

    // draw left and right borders
    for (int i = 3; i < 8; i++) {
      screen.newTextGraphics().setCharacter(2, i, verticalBorder);
      screen.newTextGraphics().setCharacter(10, i, verticalBorder);
    }

    // clear all the inner cells of the card
    for (int i = 3; i < 8; i++) {
      for (int j = 3; j < 10; j++) {
        screen.newTextGraphics().setCharacter(j, i, new TextCharacter(' ', white, white));
      }
    }

    // draw card value and suit in the center of the card
    screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(3, 3, cardValue);
    if (cardValue.equals("10")) {
      screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(8, 7, cardValue);
    } else if (cardValue.equals("JOKER")) {
      screen.newTextGraphics().setForegroundColor(suitColor)
          .setBackgroundColor(white)
          .putString(5, 7, cardValue);
    } else {
      screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(9, 7, cardValue);
    }

    screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(3, 4, suit);
    screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(9, 6, suit);
    screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(5, 5, suitNames[suitIndex]);
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
