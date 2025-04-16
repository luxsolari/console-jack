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

  // Layering/Render system
  record Position(int x, int y) {} // record class for storing x and y coordinates

  record Zlayer(String name, int index) {} // record class for storing layer name and index

  record Layer(Map<Position, TextCharacter> contents) {} // record class for storing layer contents

  private final AtomicReference<Screen> mainScreen = new AtomicReference<>();
  private Map<Zlayer, Layer> layers; // map of layers for rendering
  private TextCharacter mainBackgroundCharacter;

  private RenderSubsystem() {}

  public static RenderSubsystem getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new RenderSubsystem();
    }
    return INSTANCE;
  }

  public boolean running() {
    return running;
  }

  public AtomicReference<Screen> mainScreen() {
    return mainScreen;
  }

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
            new Zlayer("Layer %d".formatted(i), i), new Layer(new ConcurrentHashMap<>()));
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

  @Override
  public void start() {
    LOGGER.info("[%s] Starting Render System".formatted(TAG));
    running = true;

    // Draw all cards of the deck, one suit per layer - with overlapping for compactness
    int cardWidth = 10; // Actual card width
    int cardHeight = 8; // Actual card height
    int horizontalOffset = 6; // Cards will overlap horizontally (smaller = more overlap)
    int verticalOffset = 3; // Minimal vertical spacing between suits
    int cardsPerRow = 13; // Show all cards in one row per suit

    // Calculate total width and height of the card layout
    int totalWidth = (cardsPerRow - 1) * horizontalOffset + cardWidth;
    int totalHeight = (4 - 1) * verticalOffset + cardHeight;

    // Calculate starting position to center the layout
    int startX = (screenColumns - totalWidth) / 2;
    int startY = (screenRows - totalHeight) / 2;

    // For each suit (Hearts, Diamonds, Clubs, Spades)
    for (int suit = 0; suit < 4; suit++) {
      int suitLayer = suit; // Use suit index as layer

      // For each card value (A, 2-10, J, Q, K)
      for (int value = 0; value < 13; value++) {
        // Calculate position in grid
        int row = value / cardsPerRow;
        int col = value % cardsPerRow;

        int x = startX + (col * horizontalOffset);
        int y = startY + (row * cardHeight) + (suit * verticalOffset);

        // Add card to appropriate layer
        addCardToLayer(suitLayer, x, y, value, suit);
      }
    }
  }

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

    // random value for the card (A, 2, 3, 4, 5, 6, 7, 8, 9, 10, J, Q, K)
    String[] cardValues = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
    String cardValue = cardValues[cardValueIndex];

    // random suit for the card (hearts, diamonds, clubs, spades), represented by wide unicode
    // characters
    String[] suits = {"♥", "♦", "♣", "♠"};
    String[] suitNames = {"HEARTS", "DIAMONDS", "CLUBS", "SPADES"};
    String suit = suits[suitIndex];

    // draw card frame using special border characters and color the card red if it's a heart or
    // diamond, white otherwise
    TextColor white = TextColor.ANSI.WHITE;
    TextColor black = TextColor.ANSI.BLACK;
    TextColor red = TextColor.ANSI.RED;
    TextColor green = TextColor.ANSI.GREEN;
    TextColor yellow = TextColor.ANSI.YELLOW;
    TextColor suitColor = (suitIndex < 2) ? red : black;
    TextColor borderColor = (cardValueIndex == 0) ? yellow : suitColor;

    // Get the layer to draw to
    Zlayer zlayer = new Zlayer("Layer %d".formatted(layerIndex), layerIndex);
    Layer layer = layers.get(zlayer);
    Map<Position, TextCharacter> layerContents = layer.contents();

    // I still fail to comprehend why the author of the lanterna library decided to use a 2D array
    // for the TextCharacter class...
    TextCharacter topLeftCorner = TextCharacter.fromCharacter('┌', borderColor, white)[0];
    TextCharacter topRightCorner = TextCharacter.fromCharacter('┐', borderColor, white)[0];
    TextCharacter bottomLeftCorner = TextCharacter.fromCharacter('└', borderColor, white)[0];
    TextCharacter bottomRightCorner = TextCharacter.fromCharacter('┘', borderColor, white)[0];
    TextCharacter horizontalBorder = TextCharacter.fromCharacter('─', borderColor, white)[0];
    TextCharacter verticalBorder = TextCharacter.fromCharacter('│', borderColor, white)[0];
    TextCharacter blank = TextCharacter.fromCharacter(' ', white, white)[0];

    // Draw to layer instead of screen
    // draw top border of the card at specified position (x, y) using special border characters
    layerContents.put(new Position(x, y), topLeftCorner);
    layerContents.put(new Position(x + width, y), topRightCorner);
    for (int i = 1; i < width; i++) {
      layerContents.put(new Position(x + i, y), horizontalBorder);
    }

    // draw bottom border of the card at specified position (x, y) using special border characters
    layerContents.put(new Position(x, y + height), bottomLeftCorner);
    layerContents.put(new Position(x + width, y + height), bottomRightCorner);
    for (int i = 1; i < width; i++) {
      layerContents.put(new Position(x + i, y + height), horizontalBorder);
    }

    // draw left border of the card at specified position (x, y) using special border characters
    for (int i = 1; i < height; i++) {
      layerContents.put(new Position(x, y + i), verticalBorder);
    }

    // draw right border of the card at specified position (x, y) using special border characters
    for (int i = 1; i < height; i++) {
      layerContents.put(new Position(x + width, y + i), verticalBorder);
    }

    // clear the inside of the card
    for (int i = 1; i < width; i++) {
      for (int j = 1; j < height; j++) {
        layerContents.put(new Position(x + i, y + j), blank);
      }
    }

    // For single characters like card values and suits, use the first character
    char cardValueChar = cardValue.charAt(0);
    char suitChar = suit.charAt(0);

    // draw card value and suit in the center of the card
    // Special handling for "10" in top left
    if (!cardValue.equals("10")) {
      layerContents.put(
          new Position(x + 1, y + 1),
          TextCharacter.fromCharacter(cardValueChar, suitColor, white)[0]);
      layerContents.put(
          new Position(x + 1, y + 2), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
    } else {
      // Handle "10" specially for top left
      layerContents.put(
          new Position(x + 1, y + 1), TextCharacter.fromCharacter('1', suitColor, white)[0]);
      layerContents.put(
          new Position(x + 2, y + 1), TextCharacter.fromCharacter('0', suitColor, white)[0]);
      layerContents.put(
          new Position(x + 1, y + 2), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
    }

    // Special handling for "10" which is two characters
    if (!cardValue.equals("10")) {
      layerContents.put(
          new Position(x + width - 1, y + height - 2),
          TextCharacter.fromCharacter(cardValueChar, suitColor, white)[0]);
      layerContents.put(
          new Position(x + width - 1, y + height - 1),
          TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
    } else {
      // Handle "10" specially since it's two characters
      layerContents.put(
          new Position(x + width - 1, y + height - 2),
          TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      layerContents.put(
          new Position(x + width - 2, y + height - 1),
          TextCharacter.fromCharacter('1', suitColor, white)[0]);
      layerContents.put(
          new Position(x + width - 1, y + height - 1),
          TextCharacter.fromCharacter('0', suitColor, white)[0]);
    }

    // draw card corners
    layerContents.put(
        new Position(x + 2, y + 2), TextCharacter.fromCharacter('┌', suitColor, white)[0]);
    layerContents.put(
        new Position(x + 8, y + 2), TextCharacter.fromCharacter('┐', suitColor, white)[0]);
    layerContents.put(
        new Position(x + 2, y + 6), TextCharacter.fromCharacter('└', suitColor, white)[0]);
    layerContents.put(
        new Position(x + 8, y + 6), TextCharacter.fromCharacter('┘', suitColor, white)[0]);

    // Add the suit symbols for each card value
    switch (cardValue) {
      case "A":
        layerContents.put(
            new Position(x + 5, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        break;
      case "2":
        layerContents.put(
            new Position(x + 5, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        break;
      case "3":
        layerContents.put(
            new Position(x + 5, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        break;
      case "4":
        layerContents.put(
            new Position(x + 3, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        break;
      case "5":
        layerContents.put(
            new Position(x + 3, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        break;
      case "6":
        layerContents.put(
            new Position(x + 3, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        break;
      case "7":
        layerContents.put(
            new Position(x + 3, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        break;
      case "8":
        layerContents.put(
            new Position(x + 3, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 2), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 6), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        break;
      case "9":
        layerContents.put(
            new Position(x + 3, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        break;
      case "10":
        layerContents.put(
            new Position(x + 3, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 2), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 6), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        break;
      case "J":
        layerContents.put(
            new Position(x + 3, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 6),
            TextCharacter.fromCharacter('*', TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 2),
            TextCharacter.fromCharacter('*', TextColor.ANSI.YELLOW, white)[0]);
        break;
      case "Q":
        layerContents.put(
            new Position(x + 3, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 4, y + 2),
            TextCharacter.fromCharacter('*', TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new Position(x + 6, y + 2),
            TextCharacter.fromCharacter('*', TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 6),
            TextCharacter.fromCharacter('*', TextColor.ANSI.YELLOW, white)[0]);
        break;
      case "K":
        layerContents.put(
            new Position(x + 3, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 3, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 7, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 4, y + 2),
            TextCharacter.fromCharacter('*', TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new Position(x + 6, y + 2),
            TextCharacter.fromCharacter('*', TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 1), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 3), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 5), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 4), TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new Position(x + 5, y + 6),
            TextCharacter.fromCharacter('*', TextColor.ANSI.YELLOW, white)[0]);
        break;
    }
  }

  /**
   * Renders all layers to the screen in order of their index. Lower index layers are rendered first
   * (background), higher index layers are rendered on top (foreground).
   */
  private void renderLayers() {
    // Render layers from bottom to top
    for (int i = 0; i < MAX_LAYERS; i++) {
      Zlayer zlayer = new Zlayer("Layer %d".formatted(i), i);
      Layer layer = layers.get(zlayer);

      if (layer != null) {
        // Render all contents of this layer
        for (Map.Entry<Position, TextCharacter> entry : layer.contents().entrySet()) {
          Position pos = entry.getKey();
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
   * Public method to add a card to a specific layer. This can be called from outside the render
   * subsystem.
   */
  public void addCardToLayer(int layerIndex, int x, int y, int cardValueIndex, int suitIndex) {
    // Standard card size
    int width = 10;
    int height = 8;

    // Draw the card to the specified layer
    drawRandomCard(layerIndex, x, y, width, height, cardValueIndex, suitIndex);
  }

  /** Clears a specific layer by removing all its contents. */
  public void clearLayer(int layerIndex) {
    Zlayer zlayer = new Zlayer("Layer %d".formatted(layerIndex), layerIndex);
    layers.put(zlayer, new Layer(new ConcurrentHashMap<>()));
  }
}
