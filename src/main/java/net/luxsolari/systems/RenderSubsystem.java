package net.luxsolari.systems;

import com.googlecode.lanterna.Symbols;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.swing.AWTTerminalFontConfiguration;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFontConfiguration;
import net.luxsolari.exceptions.ResourceCleanupException;
import net.luxsolari.exceptions.ResourceInitializationException;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;


public class RenderSubsystem implements Subsystem {
  private static RenderSubsystem INSTANCE;

  private static final String TAG = RenderSubsystem.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  private static final int TARGET_FPS = 4;  // Target frames per second, this is the maximum FPS we want to achieve.
  private static final int SECOND_IN_NANOS = 1_000_000_000; // 1 second, expressed in nanoseconds. This is used for time calculations.
  private static final long RENDER_INTERVAL = TimeUnit.MILLISECONDS.toNanos(1000L / TARGET_FPS); // ~100ms per frame
  private static final int MAX_LAYERS = 10; // maximum number of layers for rendering

  // tracking statistics for FPS and UPS counters
  private int frameCount = 0;
  private int currentFPS = 0;
  private int screenColumns;
  private int screenRows;

  private boolean running = false;

  // Layering/Render system
  record Position(int x, int y) {
  } // record class for storing x and y coordinates

  record ZIndex(String name, int index) {
  } // record class for storing layer name and index

  record Layer(Map<Position, TextCharacter> contents) {
  } // record class for storing layer contents

  private final AtomicReference<Screen> mainScreen = new AtomicReference<>();
  private Map<ZIndex, Layer> layers; // map of layers for rendering
  private TextCharacter mainBackgroundCharacter;

  private RenderSubsystem() {
  }

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
      // We want to make sure that we get a graphical terminal, no text terminal for us, since we're making a game that will be graphical,
      // even if it's text-graphics based.
      // The graphical terminal will be contained inside a Swing or AWT window provided by Lanterna library.
      DefaultTerminalFactory terminalFactory =
          new DefaultTerminalFactory()
              .setForceTextTerminal(false)
              .setPreferTerminalEmulator(true)
              .setTerminalEmulatorTitle(TAG);

      // Set custom font for the terminal, load font from resources
      Font font =
          Font.createFont(Font.PLAIN, Objects.requireNonNull(getClass().getResourceAsStream("/fonts/InputMono-Regular.ttf")))
              .deriveFont(Font.PLAIN, 24);
      SwingTerminalFontConfiguration fontConfig =
          new SwingTerminalFontConfiguration(true, AWTTerminalFontConfiguration.BoldMode.NOTHING, font);
      terminalFactory.setTerminalEmulatorFontConfiguration(fontConfig);

      int targetWidth = 1280;
      int targetHeight = 720;

      // calculate columns and rows based on font size using a rough scaling factor of 0.625 for width and 1.18 for height
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
      for (int i = 0; i < MAX_LAYERS; i++) {  // initialize each layer
        this.layers.put(
            new ZIndex("Layer %d".formatted(i), i),
            new Layer(new ConcurrentHashMap<>()
            ));
      }

      TextColor backgroundColor = new TextColor.RGB(40, 55, 40);

      this.mainBackgroundCharacter = TextCharacter.fromCharacter(' ', backgroundColor, backgroundColor)[0];
      // draw a green background to simulate a game table
      this.mainScreen.get().clear();
      for (int i = 0; i < screenColumns; i++) {
        for (int j = 0; j < screenRows; j++) {
          this.mainScreen.get().setCharacter(i, j, mainBackgroundCharacter);
        }
      }
    } catch (IOException | FontFormatException e) {
      LOGGER.severe("[%s] Error while initializing Master Game Handler: %s".formatted(TAG, e.getMessage()));
      throw new ResourceInitializationException("Error while initializing Master Game Handler", e);
    }
    this.start();
  }

  @Override
  public void start() {
    LOGGER.info("[%s] Starting Render System".formatted(TAG));
    running = true;
  }

  @Override
  @SuppressWarnings("BusyWait")
  public void update() {
    long lastDeltaClock = java.lang.System.nanoTime();
    long fpsCounterClock = java.lang.System.nanoTime();
    double sleepTime = 0;

    while (running) {
      long now = java.lang.System.nanoTime(); // current time in nanoseconds
      double deltaTime = (double) (now - lastDeltaClock) / SECOND_IN_NANOS; // time passed since last frame in seconds
      double elapsedTime = now - deltaTime; // accumulated time since start of loop in nanoseconds

      // calculate FPS
      if (now - fpsCounterClock >= SECOND_IN_NANOS) {
        currentFPS = frameCount;
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

          // refresh the screen to apply changes
          this.mainScreen.get().refresh();
          frameCount++;
          lastDeltaClock = now;
        }

        // sleep for the remaining time to target render interval if needed to keep the game loop stable
        sleepTime = (RENDER_INTERVAL - (java.lang.System.nanoTime() - elapsedTime)) / SECOND_IN_NANOS;
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
      textGraphics.drawLine(i, 0, i, 0, Symbols.SINGLE_LINE_HORIZONTAL)
          .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
          .setForegroundColor(TextColor.ANSI.RED);
    }

    // draw bottom border
    for (int i = 0; i < this.mainScreen.get().getTerminalSize().getColumns(); i++) {
      textGraphics.drawLine(i, this.mainScreen.get().getTerminalSize().getRows() - 1, i,
              this.mainScreen.get().getTerminalSize().getRows() - 1,
              Symbols.SINGLE_LINE_HORIZONTAL)
          .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
          .setForegroundColor(TextColor.ANSI.RED);
    }

    // draw left border
    for (int i = 0; i < this.mainScreen.get().getTerminalSize().getRows(); i++) {
      textGraphics.drawLine(0, i, 0, i, Symbols.SINGLE_LINE_VERTICAL)
          .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
          .setForegroundColor(TextColor.ANSI.RED);
    }

    // draw right border

    for (int i = 0; i < this.mainScreen.get().getTerminalSize().getRows(); i++) {
      textGraphics.drawLine(this.mainScreen.get().getTerminalSize().getColumns() - 1, i,
              this.mainScreen.get().getTerminalSize().getColumns() - 1, i, Symbols.SINGLE_LINE_VERTICAL)
          .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
          .setForegroundColor(TextColor.ANSI.RED);
    }

    // draw corners
    textGraphics.setCharacter(0, 0, Symbols.SINGLE_LINE_TOP_LEFT_CORNER)
        .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
        .setForegroundColor(TextColor.ANSI.RED);
    textGraphics.setCharacter(this.mainScreen.get().getTerminalSize().getColumns() - 1, 0, Symbols.SINGLE_LINE_TOP_RIGHT_CORNER)
        .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
        .setForegroundColor(TextColor.ANSI.RED);
    textGraphics.setCharacter(0, this.mainScreen.get().getTerminalSize().getRows() - 1, Symbols.SINGLE_LINE_BOTTOM_LEFT_CORNER)
        .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
        .setForegroundColor(TextColor.ANSI.RED);
    textGraphics.setCharacter(
            this.mainScreen.get().getTerminalSize().getColumns() - 1, this.mainScreen.get().getTerminalSize().getRows() - 1,
            Symbols.SINGLE_LINE_BOTTOM_RIGHT_CORNER)
        .setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor())
        .setForegroundColor(TextColor.ANSI.RED);
  }

  private void displayPerfStats(double deltaTime, double sleepTime) {
    TextGraphics textGraphics = this.mainScreen.get().newTextGraphics();
    textGraphics.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT);
    textGraphics.setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor());
    textGraphics.putString(1, 1, "Render System Stats");
    textGraphics.putString(1, 2, "FPS: %d (Target %d)".formatted(currentFPS, TARGET_FPS));
    textGraphics.putString(1, 3, "Delta Time: %dms".formatted((int) (deltaTime * 1000)));
    textGraphics.putString(1, 4, "Sleep Time: %dms".formatted((int) (sleepTime * 1000)));
    textGraphics.putString(1, 5, "Actual Render Time: %.3f".formatted((deltaTime - sleepTime) * 1000).concat("ms "));
    textGraphics.drawLine(1, 6, this.mainScreen.get().getTerminalSize().getColumns() - 2, 6, Symbols.SINGLE_LINE_HORIZONTAL);
  }

  private void drawRandomCard(Screen screen, int x, int y, int width, int height, int cardValueIndex, int suitIndex) {

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
//    char shadowSide = '▕';
//    char shadowBottom = '░';
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
}
