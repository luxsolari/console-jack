package net.luxsolari.engine.systems;

import com.googlecode.lanterna.Symbols;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.swing.AWTTerminalFontConfiguration;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFontConfiguration;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import net.luxsolari.engine.exceptions.ResourceCleanupException;
import net.luxsolari.engine.exceptions.ResourceInitializationException;
import net.luxsolari.game.entity.EntityPosition;

public class RenderSubsystem implements Subsystem {
  private static RenderSubsystem INSTANCE;

  private static final String TAG = RenderSubsystem.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  private static final int TARGET_FPS =
      4; // Target frames per second, this is the maximum FPS we want to achieve.
  private static final int SECOND_IN_NANOS =
      1_000_000_000; // 1 second, expressed in nanoseconds. This is used for time calculations.
  private static final long RENDER_INTERVAL =
      TimeUnit.MILLISECONDS.toNanos(1000L / TARGET_FPS); // ~100ms per frame
  private static final int MAX_LAYERS = 10; // maximum number of layers for rendering

  // tracking statistics for FPS and UPS counters
  private int frameCount = 0;
  private int currentFps = 0;
  private int screenColumns;
  private int screenRows;

  private boolean running = false;
  private final AtomicReference<Screen> mainScreen = new AtomicReference<>();
  private Map<ZLayer, ZLayerData> layers; // map of layers for rendering
  private TextCharacter mainBackgroundCharacter;

  private RenderSubsystem() {}

  /**
   * Returns the singleton instance of the RenderSubsystem.
   *
   * @return The singleton instance of RenderSubsystem
   */
  public static RenderSubsystem getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new RenderSubsystem();
    }
    return INSTANCE;
  }

  /**
   * Checks if the render subsystem is currently running.
   *
   * @return true if the subsystem is running, false otherwise
   */
  public boolean running() {
    return running;
  }

  /**
   * Returns the main screen reference used for rendering.
   *
   * @return The AtomicReference containing the main Screen instance
   */
  public AtomicReference<Screen> mainScreen() {
    return mainScreen;
  }

  /**
   * Initializes the render subsystem by setting up the terminal, screen, and layers.
   *
   * @throws ResourceInitializationException if initialization fails
   */
  @Override
  public void init() {
    LOGGER.info("[%s] Initializing Render System".formatted(TAG));
    try {
      // We want to make sure that we get a graphical terminal, no text terminal for us, since we're
      // making a game that will be graphical,
      // even if it's text-graphics based.
      // The graphical terminal will be contained inside a Swing or AWT window provided by Lanterna
      // library.
      DefaultTerminalFactory terminalFactory =
          new DefaultTerminalFactory()
              .setForceTextTerminal(false)
              .setPreferTerminalEmulator(true)
              .setTerminalEmulatorTitle(TAG);

      // Set custom font for the terminal, load font from resources
      Font font =
          Font.createFont(
                  Font.PLAIN,
                  Objects.requireNonNull(
                      getClass().getResourceAsStream("/fonts/InputMono-Regular.ttf")))
              .deriveFont(Font.PLAIN, 20);
      SwingTerminalFontConfiguration fontConfig =
          new SwingTerminalFontConfiguration(
              true, AWTTerminalFontConfiguration.BoldMode.NOTHING, font);
      terminalFactory.setTerminalEmulatorFontConfiguration(fontConfig);

      int targetWidth = 1280;
      int targetHeight = 720;

      // calculate columns and rows based on font size using a rough scaling factor of 0.625 for
      // width and 1.18 for height
      // width scaling factor - could be a configuration parameter for different fonts.
      // Have to keep in mind these factors change depending on the font size and font family.
      float fontPixelWidth = font.getSize() * .625f;
      // height scaling factor - same as above
      float fontPixelHeight = font.getSize() * 1.175f;

      screenColumns = Math.round(targetWidth / fontPixelWidth);
      screenRows = Math.round(targetHeight / fontPixelHeight);

      terminalFactory.setInitialTerminalSize(new TerminalSize(screenColumns, screenRows));

      this.mainScreen.set(terminalFactory.createScreen());
      this.mainScreen.get().startScreen();
      this.mainScreen.get().setCursorPosition(null); // we don't need a cursor

      this.layers = new ConcurrentHashMap<>(); // initialize the layers map
      for (int i = 0; i < MAX_LAYERS; i++) { // initialize each layer
        this.layers.put(
            new ZLayer("Layer %d".formatted(i), i), new ZLayerData(new ConcurrentHashMap<>()));
      }

      TextColor backgroundColor = new TextColor.RGB(40, 55, 40);

      this.mainBackgroundCharacter =
          TextCharacter.fromCharacter(' ', backgroundColor, backgroundColor)[0];
      // draw a green background to simulate a game table
      this.mainScreen.get().clear();
      for (int i = 0; i < screenColumns; i++) {
        for (int j = 0; j < screenRows; j++) {
          this.mainScreen.get().setCharacter(i, j, mainBackgroundCharacter);
        }
      }
    } catch (IOException | FontFormatException e) {
      LOGGER.severe(
          "[%s] Error while initializing Master Game Handler: %s".formatted(TAG, e.getMessage()));
      throw new ResourceInitializationException("Error while initializing Master Game Handler", e);
    }
    this.start();
  }

  /** Starts the render subsystem and initializes the card layout. */
  @Override
  public void start() {
    LOGGER.info("[%s] Starting Render System".formatted(TAG));
    running = true;
  }

  /**
   * Updates the render subsystem, handling screen resizing and frame rendering.
   *
   * @throws ResourceInitializationException if update operations fail
   */
  @Override
  @SuppressWarnings("BusyWait")
  public void update() {
    long lastDeltaClock = java.lang.System.nanoTime();
    long fpsCounterClock = java.lang.System.nanoTime();
    double sleepTime = 0;

    while (running) {
      long now = java.lang.System.nanoTime(); // current time in nanoseconds
      double deltaTime =
          (double) (now - lastDeltaClock)
              / SECOND_IN_NANOS; // time passed since last frame in seconds
      double elapsedTime = now - deltaTime; // accumulated time since start of loop in nanoseconds

      // calculate FPS
      if (now - fpsCounterClock >= SECOND_IN_NANOS) {
        currentFps = frameCount;
        frameCount = 0;
        fpsCounterClock = now;
      }

      try {
        if (this.mainScreen.get().doResizeIfNecessary() != null) {
          // resize the screen if the terminal size has changed
          this.mainScreen.get().doResizeIfNecessary();
          this.screenColumns = this.mainScreen.get().getTerminalSize().getColumns();
          this.screenRows = this.mainScreen.get().getTerminalSize().getRows();

          // draw a green background to simulate a game table
          this.mainScreen.get().clear();
          for (int i = 0; i < screenColumns; i++) {
            for (int j = 0; j < screenRows; j++) {
              this.mainScreen.get().setCharacter(i, j, mainBackgroundCharacter);
            }
          }
        }

        if (running && (elapsedTime >= (RENDER_INTERVAL))) {
          drawScreenBorders();
          displayPerfStats(deltaTime, sleepTime);

          // Layer system rendering goes here
          renderLayers();

          // refresh the screen to apply changes
          this.mainScreen.get().refresh();
          frameCount++;
          lastDeltaClock = now;
        }

        // sleep for the remaining time to target render interval if needed to keep the game loop
        // stable
        sleepTime =
            (RENDER_INTERVAL - (java.lang.System.nanoTime() - elapsedTime)) / SECOND_IN_NANOS;
        if (sleepTime >= 0) {

          Thread.sleep((long) (sleepTime * 1000), (int) (sleepTime * 1000));
          Thread.yield(); // let other threads run
        }
      } catch (InterruptedException | IOException e) {
        LOGGER.severe("[%s] Error while updating Render System: %s".formatted(TAG, e.getMessage()));
        throw new ResourceInitializationException("Error while updating Render System", e);
      }
    }
  }

  /**
   * Stops the render subsystem and closes the screen.
   *
   * @throws ResourceCleanupException if stopping operations fail
   */
  @Override
  public void stop() {
    LOGGER.info("[%s] Stopping Render System".formatted(TAG));
    try {
      this.mainScreen.get().stopScreen();
      this.mainScreen.get().close();
      running = false;
    } catch (IOException e) {
      LOGGER.severe("[%s] Error while stopping Render System: %s".formatted(TAG, e.getMessage()));
      throw new ResourceCleanupException("Error while stopping Render System", e);
    }
  }

  /** Cleans up resources used by the render subsystem. */
  @Override
  public void cleanUp() {
    LOGGER.info("[%s] Cleaning up Render System".formatted(TAG));
  }

  private void drawScreenBorders() {
    TextGraphics textGraphics = this.mainScreen.get().newTextGraphics();
    // draw top border
    for (int i = 0; i < this.mainScreen.get().getTerminalSize().getColumns(); i++) {
      textGraphics
          .drawLine(i, 0, i, 0, Symbols.SINGLE_LINE_HORIZONTAL)
          .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
          .setForegroundColor(TextColor.ANSI.RED);
    }

    // draw bottom border
    for (int i = 0; i < this.mainScreen.get().getTerminalSize().getColumns(); i++) {
      textGraphics
          .drawLine(
              i,
              this.mainScreen.get().getTerminalSize().getRows() - 1,
              i,
              this.mainScreen.get().getTerminalSize().getRows() - 1,
              Symbols.SINGLE_LINE_HORIZONTAL)
          .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
          .setForegroundColor(TextColor.ANSI.RED);
    }

    // draw left border
    for (int i = 0; i < this.mainScreen.get().getTerminalSize().getRows(); i++) {
      textGraphics
          .drawLine(0, i, 0, i, Symbols.SINGLE_LINE_VERTICAL)
          .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
          .setForegroundColor(TextColor.ANSI.RED);
    }

    // draw right border

    for (int i = 0; i < this.mainScreen.get().getTerminalSize().getRows(); i++) {
      textGraphics
          .drawLine(
              this.mainScreen.get().getTerminalSize().getColumns() - 1,
              i,
              this.mainScreen.get().getTerminalSize().getColumns() - 1,
              i,
              Symbols.SINGLE_LINE_VERTICAL)
          .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
          .setForegroundColor(TextColor.ANSI.RED);
    }

    // draw corners
    textGraphics
        .setCharacter(0, 0, Symbols.SINGLE_LINE_TOP_LEFT_CORNER)
        .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
        .setForegroundColor(TextColor.ANSI.RED);
    textGraphics
        .setCharacter(
            this.mainScreen.get().getTerminalSize().getColumns() - 1,
            0,
            Symbols.SINGLE_LINE_TOP_RIGHT_CORNER)
        .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
        .setForegroundColor(TextColor.ANSI.RED);
    textGraphics
        .setCharacter(
            0,
            this.mainScreen.get().getTerminalSize().getRows() - 1,
            Symbols.SINGLE_LINE_BOTTOM_LEFT_CORNER)
        .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
        .setForegroundColor(TextColor.ANSI.RED);
    textGraphics
        .setCharacter(
            this.mainScreen.get().getTerminalSize().getColumns() - 1,
            this.mainScreen.get().getTerminalSize().getRows() - 1,
            Symbols.SINGLE_LINE_BOTTOM_RIGHT_CORNER)
        .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
        .setForegroundColor(TextColor.ANSI.RED);
  }

  private void displayPerfStats(double deltaTime, double sleepTime) {
    TextGraphics textGraphics = this.mainScreen.get().newTextGraphics();
    textGraphics.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT);
    textGraphics.setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor());
    textGraphics.putString(1, 1, "Render Subsystem Stats");
    textGraphics.putString(1, 2, "FPS: %d (Target %d)".formatted(currentFps, TARGET_FPS));
    textGraphics.putString(1, 3, "Delta Time: %dms".formatted((int) (deltaTime * 1000)));
    textGraphics.putString(1, 4, "Sleep Time: %dms".formatted((int) (sleepTime * 1000)));
    textGraphics.putString(
        1, 5, "Actual Render Time: %.3f".formatted((deltaTime - sleepTime) * 1000).concat("ms "));
    textGraphics.drawLine(
        1,
        6,
        this.mainScreen.get().getTerminalSize().getColumns() - 2,
        6,
        Symbols.SINGLE_LINE_HORIZONTAL);
  }

  private void drawRandomCard(
      int layerIndex, int x, int y, int width, int height, int cardValueIndex, int suitIndex) {

    // random value for the card (A, 2, 3, 4, 5, 6, 7, 8, 9, 10, J, Q, K, JOKER)
    String[] cardValues = {
      "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "JOKER"
    };
    final String cardValue = cardValues[cardValueIndex];

    // random suit for the card (hearts, diamonds, clubs, spades), represented by wide unicode
    // characters
    String[] suits = {"♥", "♦", "♣", "♠"};
    String[] suitNames = {"HEARTS", "DIAMONDS", "CLUBS", "SPADES"};
    final String suit = suits[suitIndex];

    // draw card frame using special border characters and color the card red if it's a heart or
    // diamond, white otherwise
    final TextColor white = TextColor.ANSI.WHITE;
    final TextColor black = TextColor.ANSI.BLACK;
    final TextColor red = TextColor.ANSI.RED;
    final TextColor green = TextColor.ANSI.GREEN;
    final TextColor yellow = TextColor.ANSI.YELLOW;
    final TextColor magenta = TextColor.ANSI.MAGENTA;
    final TextColor suitColor = (suitIndex < 2) ? red : black;
    final TextColor borderColor =
        (cardValueIndex == 0 || cardValueIndex == 13) ? yellow : suitColor;

    // Get the layer to draw to
    ZLayer zlayer = new ZLayer("Layer %d".formatted(layerIndex), layerIndex);
    ZLayerData layer = layers.get(zlayer);
    Map<ZLayerPosition, TextCharacter> layerContents = layer.contents();

    // I still fail to comprehend why the author of the lanterna library decided to use a 2D array
    // for the TextCharacter class...
    final TextCharacter topLeftCorner = TextCharacter.fromCharacter('┌', borderColor, white)[0];
    final TextCharacter topRightCorner = TextCharacter.fromCharacter('┐', borderColor, white)[0];
    final TextCharacter bottomLeftCorner = TextCharacter.fromCharacter('└', borderColor, white)[0];
    final TextCharacter bottomRightCorner = TextCharacter.fromCharacter('┘', borderColor, white)[0];
    final TextCharacter horizontalBorder = TextCharacter.fromCharacter('─', borderColor, white)[0];
    final TextCharacter verticalBorder = TextCharacter.fromCharacter('│', borderColor, white)[0];
    final TextCharacter blank = TextCharacter.fromCharacter(' ', white, white)[0];

    // Draw to layer instead of screen
    // draw top border of the card at specified position (x, y) using special border characters
    layerContents.put(new ZLayerPosition(x, y), topLeftCorner);
    layerContents.put(new ZLayerPosition(x + width, y), topRightCorner);
    for (int i = 1; i < width; i++) {
      layerContents.put(new ZLayerPosition(x + i, y), horizontalBorder);
    }

    // draw bottom border of the card at specified position (x, y) using special border characters
    layerContents.put(new ZLayerPosition(x, y + height), bottomLeftCorner);
    layerContents.put(new ZLayerPosition(x + width, y + height), bottomRightCorner);
    for (int i = 1; i < width; i++) {
      layerContents.put(new ZLayerPosition(x + i, y + height), horizontalBorder);
    }

    // draw left border of the card at specified position (x, y) using special border characters
    for (int i = 1; i < height; i++) {
      layerContents.put(new ZLayerPosition(x, y + i), verticalBorder);
    }

    // draw right border of the card at specified position (x, y) using special border characters
    for (int i = 1; i < height; i++) {
      layerContents.put(new ZLayerPosition(x + width, y + i), verticalBorder);
    }

    // clear the inside of the card
    for (int i = 1; i < width; i++) {
      for (int j = 1; j < height; j++) {
        layerContents.put(new ZLayerPosition(x + i, y + j), blank);
      }
    }

    // For single characters like card values and suits, use the first character
    char cardValueChar = cardValue.charAt(0);
    char suitChar = suit.charAt(0);

    // draw card value and suit in the center of the card
    // Special handling for "10" in top left
    if (!cardValue.equals("10")) {
      layerContents.put(
          new ZLayerPosition(x + 1, y + 1),
          TextCharacter.fromCharacter(cardValueChar, suitColor, white)[0]);
      layerContents.put(
          new ZLayerPosition(x + 1, y + 2),
          TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
    } else {
      // Handle "10" specially for top left
      layerContents.put(
          new ZLayerPosition(x + 1, y + 1), TextCharacter.fromCharacter('1', suitColor, white)[0]);
      layerContents.put(
          new ZLayerPosition(x + 2, y + 1), TextCharacter.fromCharacter('0', suitColor, white)[0]);
      layerContents.put(
          new ZLayerPosition(x + 1, y + 2),
          TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
    }

    // Special handling for "10" which is two characters
    if (!cardValue.equals("10")) {
      layerContents.put(
          new ZLayerPosition(x + width - 1, y + height - 2),
          TextCharacter.fromCharacter(cardValueChar, suitColor, white)[0]);
      layerContents.put(
          new ZLayerPosition(x + width - 1, y + height - 1),
          TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
    } else {
      // Handle "10" specially since it's two characters
      layerContents.put(
          new ZLayerPosition(x + width - 1, y + height - 2),
          TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      layerContents.put(
          new ZLayerPosition(x + width - 2, y + height - 1),
          TextCharacter.fromCharacter('1', suitColor, white)[0]);
      layerContents.put(
          new ZLayerPosition(x + width - 1, y + height - 1),
          TextCharacter.fromCharacter('0', suitColor, white)[0]);
    }

    // draw card corners
    layerContents.put(
        new ZLayerPosition(x + 2, y + 2), TextCharacter.fromCharacter('┌', suitColor, white)[0]);
    layerContents.put(
        new ZLayerPosition(x + 8, y + 2), TextCharacter.fromCharacter('┐', suitColor, white)[0]);
    layerContents.put(
        new ZLayerPosition(x + 2, y + 6), TextCharacter.fromCharacter('└', suitColor, white)[0]);
    layerContents.put(
        new ZLayerPosition(x + 8, y + 6), TextCharacter.fromCharacter('┘', suitColor, white)[0]);

    // Add the suit symbols for each card value
    switch (cardValue) {
      case "A" -> {
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "2" -> {
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "3" -> {
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "4" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "5" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "6" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "7" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "8" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 2),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 6),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "9" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "10" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 2),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 6),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "J" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 6),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 2),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
      }
      case "Q" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 4, y + 2),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 6, y + 2),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 6),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
      }
      case "K" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 4, y + 2),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 6, y + 2),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 1),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 6),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
      }
      case "JOKER" -> {
        // Special rendering for Joker card with a colorful pattern
        // Draw a jester hat pattern
        layerContents.put(
            new ZLayerPosition(x + 3, y + 2), TextCharacter.fromCharacter('▲', magenta, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 4, y + 2), TextCharacter.fromCharacter('▲', red, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 2), TextCharacter.fromCharacter('▲', yellow, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 6, y + 2), TextCharacter.fromCharacter('▲', green, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 2), TextCharacter.fromCharacter('▲', magenta, white)[0]);

        // Draw a joker face
        layerContents.put(
            new ZLayerPosition(x + 4, y + 3), TextCharacter.fromCharacter('(', white, black)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3), TextCharacter.fromCharacter('^', yellow, black)[0]);
        layerContents.put(
            new ZLayerPosition(x + 6, y + 3), TextCharacter.fromCharacter(')', white, black)[0]);

        // Draw a smile
        layerContents.put(
            new ZLayerPosition(x + 4, y + 4), TextCharacter.fromCharacter('\\', white, black)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4), TextCharacter.fromCharacter('_', white, black)[0]);
        layerContents.put(
            new ZLayerPosition(x + 6, y + 4), TextCharacter.fromCharacter('/', white, black)[0]);

        // Draw joker text
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5), TextCharacter.fromCharacter('J', magenta, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 4, y + 5), TextCharacter.fromCharacter('O', red, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5), TextCharacter.fromCharacter('K', yellow, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 6, y + 5), TextCharacter.fromCharacter('E', green, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5), TextCharacter.fromCharacter('R', magenta, white)[0]);
      }
      default -> {
        // Handle any unexpected card values by drawing a simple pattern
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
    }
  }

  /**
   * Renders all layers to the screen in order of their index. Lower index layers are rendered first
   * (background), higher index layers are rendered on top (foreground).
   */
  private void renderLayers() {
    // Render layers from bottom to top
    for (int i = 0; i < MAX_LAYERS; i++) {
      ZLayer zlayer = new ZLayer("Layer %d".formatted(i), i);
      ZLayerData layer = layers.get(zlayer);

      if (layer != null) {
        // Render all contents of this layer
        for (Map.Entry<ZLayerPosition, TextCharacter> entry : layer.contents().entrySet()) {
          ZLayerPosition pos = entry.getKey();
          TextCharacter character = entry.getValue();

          // Only render if within screen bounds
          if (pos.x() >= 0 && pos.x() < screenColumns && pos.y() >= 0 && pos.y() < screenRows) {
            this.mainScreen.get().setCharacter(pos.x(), pos.y(), character);
          }
        }
      }
    }
  }

  /**
   * Adds a card to a specific rendering layer.
   *
   * @param layerIndex The index of the layer to add the card to
   * @param x The x-coordinate position of the card
   * @param y The y-coordinate position of the card
   * @param cardValueIndex The index of the card value (0-12 for A,2-10,J,Q,K)
   * @param suitIndex The index of the card suit (0-3 for Hearts,Diamonds,Clubs,Spades)
   */
  public void addCardToLayer(int layerIndex, int x, int y, int cardValueIndex, int suitIndex) {
    // Standard card size
    int width = 10;
    int height = 8;

    // Draw the card to the specified layer
    drawRandomCard(layerIndex, x, y, width, height, cardValueIndex, suitIndex);
  }

  /**
   * Clears all contents from a specific rendering layer.
   *
   * @param layerIndex The index of the layer to clear
   */
  public void clearLayer(int layerIndex) {
    ZLayer zlayer = new ZLayer("Layer %d".formatted(layerIndex), layerIndex);
    layers.put(zlayer, new ZLayerData(new ConcurrentHashMap<>()));
  }

  /**
   * Renders an Entity object to the specified layer.
   *
   * @param layerIndex The index of the layer to render to
   * @param entity The Entity to render
   */
  public void renderEntity(int layerIndex, net.luxsolari.game.entity.Entity entity) {
    if (!entity.isVisible()) {
      return;
    }

    // Convert entity position and size to integers for rendering
    EntityPosition entityPos = entity.getPosition();
    ZLayerPosition pos = entityPos.toZLayerPosition();
    int x = pos.x();
    int y = pos.y();
    int width = (int) entity.getWidth();
    int height = (int) entity.getHeight();

    // Get the layer to draw to
    ZLayer zlayer = new ZLayer("Layer %d".formatted(layerIndex), layerIndex);
    ZLayerData layer = layers.get(zlayer);
    Map<ZLayerPosition, TextCharacter> layerContents = layer.contents();

    // Draw a simple rectangle for the entity
    TextColor borderColor = TextColor.ANSI.WHITE;
    TextColor fillColor = TextColor.ANSI.BLACK;

    // Draw borders
    for (int i = x; i <= x + width; i++) {
      layerContents.put(
          new ZLayerPosition(i, y), TextCharacter.fromCharacter('─', borderColor, fillColor)[0]);
      layerContents.put(
          new ZLayerPosition(i, y + height),
          TextCharacter.fromCharacter('─', borderColor, fillColor)[0]);
    }

    for (int i = y; i <= y + height; i++) {
      layerContents.put(
          new ZLayerPosition(x, i), TextCharacter.fromCharacter('│', borderColor, fillColor)[0]);
      layerContents.put(
          new ZLayerPosition(x + width, i),
          TextCharacter.fromCharacter('│', borderColor, fillColor)[0]);
    }

    // Draw corners
    layerContents.put(
        new ZLayerPosition(x, y), TextCharacter.fromCharacter('┌', borderColor, fillColor)[0]);
    layerContents.put(
        new ZLayerPosition(x + width, y),
        TextCharacter.fromCharacter('┐', borderColor, fillColor)[0]);
    layerContents.put(
        new ZLayerPosition(x, y + height),
        TextCharacter.fromCharacter('└', borderColor, fillColor)[0]);
    layerContents.put(
        new ZLayerPosition(x + width, y + height),
        TextCharacter.fromCharacter('┘', borderColor, fillColor)[0]);
  }

  /**
   * Renders a Card object to the specified layer.
   *
   * @param layerIndex The index of the layer to render to
   * @param card The Card to render
   */
  public void renderCard(int layerIndex, net.luxsolari.game.entity.Card card) {
    if (!card.isVisible()) {
      return;
    }

    // Convert card position and size to integers for rendering
    int x = (int) card.getX();
    int y = (int) card.getY();
    int width = (int) card.getWidth();
    int height = (int) card.getHeight();

    // Get the layer to draw to
    ZLayer zlayer = new ZLayer("Layer %d".formatted(layerIndex), layerIndex);
    ZLayerData layer = layers.get(zlayer);
    Map<ZLayerPosition, TextCharacter> layerContents = layer.contents();

    // Determine card colors based on suit
    TextColor suitColor;
    if (card.getSuit() == net.luxsolari.game.entity.Card.Suit.HEARTS
        || card.getSuit() == net.luxsolari.game.entity.Card.Suit.DIAMONDS) {
      suitColor = TextColor.ANSI.RED;
    } else {
      suitColor = TextColor.ANSI.BLACK;
    }

    TextColor borderColor = suitColor;
    TextColor fillColor = TextColor.ANSI.WHITE;

    // Draw card borders
    for (int i = x; i <= x + width; i++) {
      layerContents.put(
          new ZLayerPosition(i, y), TextCharacter.fromCharacter('─', borderColor, fillColor)[0]);
      layerContents.put(
          new ZLayerPosition(i, y + height),
          TextCharacter.fromCharacter('─', borderColor, fillColor)[0]);
    }

    for (int i = y; i <= y + height; i++) {
      layerContents.put(
          new ZLayerPosition(x, i), TextCharacter.fromCharacter('│', borderColor, fillColor)[0]);
      layerContents.put(
          new ZLayerPosition(x + width, i),
          TextCharacter.fromCharacter('│', borderColor, fillColor)[0]);
    }

    // Draw corners
    layerContents.put(
        new ZLayerPosition(x, y), TextCharacter.fromCharacter('┌', borderColor, fillColor)[0]);
    layerContents.put(
        new ZLayerPosition(x + width, y),
        TextCharacter.fromCharacter('┐', borderColor, fillColor)[0]);
    layerContents.put(
        new ZLayerPosition(x, y + height),
        TextCharacter.fromCharacter('└', borderColor, fillColor)[0]);
    layerContents.put(
        new ZLayerPosition(x + width, y + height),
        TextCharacter.fromCharacter('┘', borderColor, fillColor)[0]);

    // If card is face up, draw the suit and rank
    if (card.isFaceUp()) {
      // Draw rank in top-left corner
      String rankStr = getRankString(card.getRank());
      for (int i = 0; i < rankStr.length(); i++) {
        layerContents.put(
            new ZLayerPosition(x + 1 + i, y + 1),
            TextCharacter.fromCharacter(rankStr.charAt(i), suitColor, fillColor)[0]);
      }

      // Draw suit symbol in top-left corner
      char suitChar = getSuitChar(card.getSuit());
      layerContents.put(
          new ZLayerPosition(x + 1, y + 2),
          TextCharacter.fromCharacter(suitChar, suitColor, fillColor)[0]);

      // Draw rank and suit in bottom-right corner (upside down)
      for (int i = 0; i < rankStr.length(); i++) {
        layerContents.put(
            new ZLayerPosition(x + width - 1 - i, y + height - 1),
            TextCharacter.fromCharacter(rankStr.charAt(i), suitColor, fillColor)[0]);
      }

      layerContents.put(
          new ZLayerPosition(x + width - 1, y + height - 2),
          TextCharacter.fromCharacter(suitChar, suitColor, fillColor)[0]);

      // Draw suit symbol in the center
      layerContents.put(
          new ZLayerPosition(x + width / 2, y + height / 2),
          TextCharacter.fromCharacter(suitChar, suitColor, fillColor)[0]);
    } else {
      // Draw card back pattern
      for (int i = x + 1; i < x + width; i += 2) {
        for (int j = y + 1; j < y + height; j += 2) {
          layerContents.put(
              new ZLayerPosition(i, j),
              TextCharacter.fromCharacter('█', TextColor.ANSI.BLUE, TextColor.ANSI.BLUE)[0]);
        }
      }
    }
  }

  /**
   * Converts a Card.Rank to its string representation.
   *
   * @param rank The rank to convert
   * @return The string representation of the rank
   */
  private String getRankString(net.luxsolari.game.entity.Card.Rank rank) {
    switch (rank) {
      case ACE:
        return "A";
      case TWO:
        return "2";
      case THREE:
        return "3";
      case FOUR:
        return "4";
      case FIVE:
        return "5";
      case SIX:
        return "6";
      case SEVEN:
        return "7";
      case EIGHT:
        return "8";
      case NINE:
        return "9";
      case TEN:
        return "10";
      case JACK:
        return "J";
      case QUEEN:
        return "Q";
      case KING:
        return "K";
      default:
        return "?";
    }
  }

  /**
   * Converts a Card.Suit to its character representation.
   *
   * @param suit The suit to convert
   * @return The character representation of the suit
   */
  private char getSuitChar(net.luxsolari.game.entity.Card.Suit suit) {
    switch (suit) {
      case HEARTS:
        return '♥';
      case DIAMONDS:
        return '♦';
      case CLUBS:
        return '♣';
      case SPADES:
        return '♠';
      default:
        return '?';
    }
  }
}
