package net.luxsolari.handlers;

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
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MasterGameHandler implements SystemHandler {

  private static final String TAG = MasterGameHandler.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  public static final int SECOND_IN_NANOS = 1_000_000_000;
  private static MasterGameHandler INSTANCE;

  private static final int TARGET_UPS = 4;  // 4 updates per second
  private static final int TARGET_FPS = 2; //  2 frames per second

  private static final long UPDATE_INTERVAL = TimeUnit.MILLISECONDS.toNanos(1000L / TARGET_UPS); // ~250ms per update
  private static final long RENDER_INTERVAL = TimeUnit.MILLISECONDS.toNanos(1000L / TARGET_FPS); // ~500ms per render

  // tracking statistics for FPS and UPS counters
  private long lastStatsTime = System.nanoTime();
  private int frameCount = 0;
  private int updateCount = 0;
  private int currentFPS = 0;
  private int currentUPS = 0;
  private int effectivePxWidth;
  private int effectivePxHeight;
  private int screenColumns;
  private int screenRows;

  private boolean running = false;
  private Screen screen;
  private float fontPixelWidth;
  private float fontPixelHeight;

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
          Font.createFont(Font.PLAIN, getClass().getResourceAsStream("/fonts/InputMono-Regular.ttf"))
              .deriveFont(Font.PLAIN, 24);
      SwingTerminalFontConfiguration fontConfig =
          new SwingTerminalFontConfiguration(true, AWTTerminalFontConfiguration.BoldMode.NOTHING, font);
      terminalFactory.setTerminalEmulatorFontConfiguration(fontConfig);

      int targetWidth = 1280;
      int targetHeight = 720;

      // calculate columns and rows based on font size using a rough scaling factor of 0.58 for width and 1.25 for height
      // width scaling factor - could be a configuration parameter for different fonts
      fontPixelWidth = font.getSize() * .625f;
      // height scaling factor - same as above
      fontPixelHeight = font.getSize() * 1.18f;

      screenColumns = Math.round(targetWidth / fontPixelWidth);
      screenRows = Math.round(targetHeight / fontPixelHeight);

      effectivePxWidth = Math.round(screenColumns * fontPixelWidth);
      effectivePxHeight = Math.round(screenRows * fontPixelHeight);

      terminalFactory.setInitialTerminalSize(new TerminalSize(screenColumns, screenRows));

      this.screen = terminalFactory.createScreen();
      this.screen.startScreen();
      this.screen.setCursorPosition(null); // we don't need a cursor

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

    long cardDrawClock = System.nanoTime();
    long timeToDrawCardSeconds = TimeUnit.MILLISECONDS.toNanos(1000);

    while (running) {
      try {
        long currentTime = System.nanoTime();

        if (currentTime - lastStatsTime >= SECOND_IN_NANOS) {
          currentFPS = frameCount;
          currentUPS = updateCount;
          frameCount = 0;
          updateCount = 0;
          lastStatsTime = currentTime;
        }

        // calculate elapsed time since last update and render
        long elapsedTime = currentTime - previousUpdateTime;
        long renderElapsedTime = currentTime - previousRenderTime;

        // update lag is the time that has passed since the last update, and we need to keep track of it
        // so we can update the game logic at a fixed rate, even if the rendering is slower or faster
        updateLag += elapsedTime;
        previousUpdateTime = currentTime;

        // update game logic at fixed rate
        while (running && (updateLag >= UPDATE_INTERVAL)) {
          // INPUT HANDLING SECTION //
          // poll for key presses
          KeyStroke keyStroke = this.screen.pollInput();
          if (keyStroke != null) {
            if ((keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter().toString().equalsIgnoreCase("c"))) {
              this.screen.clear();
            }
            if ((keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter().toString().equalsIgnoreCase("q")) ||
                keyStroke.getKeyType() == KeyType.EOF) {
              this.stop();
            }
          }
          // END INPUT HANDLING SECTION //

          // GAME LOGIC SECTION //
          // In a real game, this is where you would update the game state.
          // This is a common pattern in game development, where you have a game loop that runs at a fixed rate, and you update
          // the game state at each iteration of the loop. This is called the "game loop" pattern.
          // In this case, we're running the game logic at 4 updates per second (UPS), which means we update the game state 4 times per second.
          // And we're running the rendering at 2 frames per second (FPS), which means we render the game screen 2 times per second.
          // This is a very simple example, but it shows the basic structure of a game loop.

          // END GAME LOGIC SECTION //

          updateCount++;
          updateLag -= UPDATE_INTERVAL;
        }

        if (running && (renderElapsedTime >= RENDER_INTERVAL)) {
          // RENDERING SECTION //
          // draw border around screen using special border characters and color it red
          if (this.screen.doResizeIfNecessary() != null) {
            this.screen.clear();

            // recalculate screen size
            screenColumns = this.screen.getTerminalSize().getColumns();
            screenRows = this.screen.getTerminalSize().getRows();

            // recalculate effective pixel size
            effectivePxWidth = Math.round(screenColumns * fontPixelWidth);
            effectivePxHeight = Math.round(screenRows * fontPixelHeight);
          }

          drawScreenBorders();
          displayStatsCounters(currentFPS, currentUPS, effectivePxWidth, effectivePxHeight, screenColumns, screenRows);

          // the update logic of this method should be decoupled from the rendering logic, but for now, we'll keep it simple
          // and do both in the same method, just to test things out. Of course this is not ideal for a real game,
          // and it means that the rendering logic will be tied to the logic that updates the model of the card, and that
          // WILL cause memory leaks, but we'll fix that later when we divide all the concerns of this system into separate classes.
          // draw a random card at a random position on the screen (x, y) every second
          if (currentTime - cardDrawClock >= timeToDrawCardSeconds) {
            drawRandomCard((int) (Math.random() * (screenColumns - 10)),
                (int) (Math.random() * (screenRows - 10)), 10, 8, (int) (Math.random() * 13), (int) (Math.random() * 4));
            cardDrawClock = currentTime;
          }

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
        Thread.yield();

      } catch (InterruptedException | IOException e) {
        this.stop();
        LOGGER.severe("[%s] Error while running Master Game Handler: %s".formatted(TAG, e.getMessage()));
      }
    }
  }

  private void displayStatsCounters(int currentFPS, int currentUPS, int effectivePxWidth, int effectivePxHeight, int screenColumns,
                                    int screenRows) {
    // Display FPS/UPS in top-right corner
    String stats = "FPS: " + currentFPS + " | UPS: " + currentUPS;
    String resolution = "RES: " + effectivePxWidth + "x" + effectivePxHeight;
    String canvasSize = "SCR: " + screenColumns + "x" + screenRows;
    screen.newTextGraphics().putString(
        screen.getTerminalSize().getColumns() - stats.length() - 2,
        1,
        stats
    );
    screen.newTextGraphics().putString(
        screen.getTerminalSize().getColumns() - resolution.length() - 2,
        2,
        resolution
    );
    screen.newTextGraphics().putString(
        screen.getTerminalSize().getColumns() - canvasSize.length() - 2,
        3,
        canvasSize
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
      textGraphics.setCharacter(i, this.screen.getTerminalSize().getRows() - 1,
          new TextCharacter('─', TextColor.ANSI.RED, TextColor.ANSI.BLACK));
    }

    // draw left border
    for (int i = 0; i < this.screen.getTerminalSize().getRows(); i++) {
      textGraphics.setCharacter(0, i, new TextCharacter('│', TextColor.ANSI.RED, TextColor.ANSI.BLACK));
    }

    // draw right border

    for (int i = 0; i < this.screen.getTerminalSize().getRows(); i++) {
      textGraphics.setCharacter(this.screen.getTerminalSize().getColumns() - 1, i,
          new TextCharacter('│', TextColor.ANSI.RED, TextColor.ANSI.BLACK));
    }

    // draw corners
    textGraphics.setCharacter(0, 0, new TextCharacter('┌'));
    textGraphics.setCharacter(this.screen.getTerminalSize().getColumns() - 1, 0, new TextCharacter('┐'));
    textGraphics.setCharacter(0, this.screen.getTerminalSize().getRows() - 1, new TextCharacter('└'));
    textGraphics.setCharacter(this.screen.getTerminalSize().getColumns() - 1, this.screen.getTerminalSize().getRows() - 1,
        new TextCharacter('┘'));
  }

  private void drawRandomCard(int x, int y, int width, int height, int cardValueIndex, int suitIndex) {

    // random value for the card (A, 2, 3, 4, 5, 6, 7, 8, 9, 10, J, Q, K)
    String[] cardValues = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
    String cardValue = cardValues[cardValueIndex];

    // random suit for the card (hearts, diamonds, clubs, spades), represented by wide unicode characters
    String[] suits = {"♥", "♦", "♣", "♠"};
    String[] suitNames = {"HEARTS", "DIAMONDS", "CLUBS", "SPADES"};
    String suit = suits[suitIndex];

    // draw card frame using special border characters and color the card red if it's a heart or diamond, white otherwise
    TextColor white = TextColor.ANSI.WHITE;
    TextColor black = TextColor.ANSI.BLACK;
    TextColor red = TextColor.ANSI.RED;
    TextColor green = TextColor.ANSI.GREEN;
    TextColor yellow = TextColor.ANSI.YELLOW;
    TextColor suitColor = (suitIndex < 2) ? red : black;
    TextColor borderColor = (cardValueIndex == 0) ? yellow : suitColor;

    // I still fail to comprehend why the author of the lanterna library decided to use a 2D array for the TextCharacter class...
    TextCharacter topLeftCorner = TextCharacter.fromCharacter('┌', borderColor, white)[0];
    TextCharacter topRightCorner = TextCharacter.fromCharacter('┐', borderColor, white)[0];
    TextCharacter bottomLeftCorner = TextCharacter.fromCharacter('└', borderColor, white)[0];
    TextCharacter bottomRightCorner = TextCharacter.fromCharacter('┘', borderColor, white)[0];
    TextCharacter horizontalBorder = TextCharacter.fromCharacter('─', borderColor, white)[0];
    TextCharacter verticalBorder = TextCharacter.fromCharacter('│', borderColor, white)[0];
    TextCharacter blank = TextCharacter.fromCharacter(' ', white, white)[0];

    // draw top border of the card at specified position (x, y) using special border characters
    screen.newTextGraphics().setCharacter(x, y, topLeftCorner);
    screen.newTextGraphics().setCharacter(x + width, y, topRightCorner);
    for (int i = 1; i < width; i++) {
      screen.newTextGraphics().setCharacter(x + i, y, horizontalBorder);
    }

    // draw bottom border of the card at specified position (x, y) using special border characters
    screen.newTextGraphics().setCharacter(x, y + height, bottomLeftCorner);
    screen.newTextGraphics().setCharacter(x + width, y + height, bottomRightCorner);
    for (int i = 1; i < width; i++) {
      screen.newTextGraphics().setCharacter(x + i, y + height, horizontalBorder);
    }

    // draw left border of the card at specified position (x, y) using special border characters
    for (int i = 1; i < height; i++) {
      screen.newTextGraphics().setCharacter(x, y + i, verticalBorder);
    }

    // draw right border of the card at specified position (x, y) using special border characters
    for (int i = 1; i < height; i++) {
      screen.newTextGraphics().setCharacter(x + width, y + i, verticalBorder);
    }

    // clear the inside of the card
    for (int i = 1; i < width; i++) {
      for (int j = 1; j < height; j++) {
        screen.newTextGraphics().setCharacter(x + i, y + j, blank);
      }
    }


    // draw card value and suit in the center of the card
    screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(x + 1, y + 1, cardValue);
    screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(x + 1, y + 2, suit);

    // draw card value and suit at the opposite corner of the card
    if (!cardValue.equals("10")) {
      screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(x + width - 1, y + height - 2, cardValue);
      screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(x + width - 1, y + height - 1, suit);
    } else {
      screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(x + width - 3, y + height - 1, cardValue);
      screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(x + width - 1, y + height - 1, suit);
    }

    // draw card value and suit in the center of the card
    screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(x + 2, y + 2, "┌");
    screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(x + 8, y + 2, "┐");
    screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(x + 2, y + 6, "└");
    ;
    screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white).putString(x + 8, y + 6, "┘");

    switch (cardValue) {
      case "A" -> {
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 4, suit);
      }
      case "2" -> {
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 5, suit);
      }
      case "3" -> {
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 5, suit);
      }
      case "4" -> {
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 5, suit);
      }
      case "5" -> {
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 5, suit);
      }
      case "6" -> {
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 5, suit);
      }
      case "7" -> {
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 4, suit);
      }
      case "8" -> {
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 2, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 6, suit);
      }
      case "9" -> {
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 5, suit);
      }
      case "10" -> {
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 2, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 6, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 3, suit);
      }
      case "J" -> {
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(TextColor.ANSI.YELLOW).setBackgroundColor(white)
            .putString(x + 5, y + 6, suit);
        screen.newTextGraphics().setForegroundColor(TextColor.ANSI.YELLOW).setBackgroundColor(white)
            .putString(x + 5, y + 2, suit);
      }
      case "Q" -> {
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(TextColor.ANSI.YELLOW).setBackgroundColor(white)
            .putString(x + 4, y + 2, suit);
        screen.newTextGraphics().setForegroundColor(TextColor.ANSI.YELLOW).setBackgroundColor(white)
            .putString(x + 6, y + 2, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(TextColor.ANSI.YELLOW).setBackgroundColor(white)
            .putString(x + 5, y + 6, suit);
      }
      case "K" -> {
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 3, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 7, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(TextColor.ANSI.YELLOW).setBackgroundColor(white)
            .putString(x + 4, y + 2, suit);
        screen.newTextGraphics().setForegroundColor(TextColor.ANSI.YELLOW).setBackgroundColor(white)
            .putString(x + 6, y + 2, suit);
        screen.newTextGraphics().setForegroundColor(TextColor.ANSI.YELLOW).setBackgroundColor(white)
            .putString(x + 5, y + 1, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 3, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 5, suit);
        screen.newTextGraphics().setForegroundColor(suitColor).setBackgroundColor(white)
            .putString(x + 5, y + 4, suit);
        screen.newTextGraphics().setForegroundColor(TextColor.ANSI.YELLOW).setBackgroundColor(white)
            .putString(x + 5, y + 6, suit);
      }
    }

    // shadow color for the card
    TextColor shadowColor = new TextColor.Indexed(235); // dark gray

    // "shadow" character from half-blocks unicode range
//    char shadowSide = '\u2595';
//    char shadowBottom = '\u2591';
//
//    TextCharacter shadowSideChar = TextCharacter.fromCharacter(shadowSide, shadowColor, shadowColor)[0];
//    TextCharacter shadowBottomChar = TextCharacter.fromCharacter(shadowBottom, shadowColor, shadowColor)[0];

//    // draw shadow for the card
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + width + 1, y + 1, shadowSideChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + width + 1, y + 2, shadowSideChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + width + 1, y + 3, shadowSideChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + width + 1, y + 4, shadowSideChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + width + 1, y + 5, shadowSideChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + width + 1, y + 6, shadowSideChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + width + 1, y + 7, shadowSideChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + width + 1, y + 8, shadowSideChar);
//
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + 1, y + height + 1, shadowBottomChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + 2, y + height + 1, shadowBottomChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + 3, y + height + 1, shadowBottomChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + 4, y + height + 1, shadowBottomChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + 5, y + height + 1, shadowBottomChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + 6, y + height + 1, shadowBottomChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + 7, y + height + 1, shadowBottomChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + 8, y + height + 1, shadowBottomChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + 9, y + height + 1, shadowBottomChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + 10, y + height + 1, shadowBottomChar);
//    screen.newTextGraphics().setForegroundColor(black).setBackgroundColor(shadowColor)
//        .setCharacter(x + 11, y + height + 1, shadowBottomChar);

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
