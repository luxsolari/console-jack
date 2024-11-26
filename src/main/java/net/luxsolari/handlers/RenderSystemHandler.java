package net.luxsolari.handlers;

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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


public class RenderSystemHandler implements SystemHandler {
  private static RenderSystemHandler INSTANCE;

  private static final String TAG = RenderSystemHandler.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  private static final int TARGET_FPS = 10;  // Target frames per second, this is the maximum FPS we want to achieve.
  private static final int SECOND_IN_NANOS = 1_000_000_000; // 1 second, expressed in nanoseconds. This is used for time calculations.
  private static final long RENDER_INTERVAL = TimeUnit.MILLISECONDS.toNanos(1000L / TARGET_FPS); // ~100ms per frame
  private static final int MAX_LAYERS = 10; // maximum number of layers for rendering

  // tracking statistics for FPS and UPS counters
  private int frameCount = 0;
  private int currentFPS = 0;
  private int screenColumns;
  private int screenRows;

  private boolean running = false;

  private float fontPixelWidth;
  private float fontPixelHeight;

  // Layering/Render system
  record Position(int x, int y) {} // record class for storing x and y coordinates

  private Screen mainScreen;
  private Map<Integer, Map<Position, TextCharacter>>[] layers;
  private TextCharacter mainBackgroundCharacter;


  private RenderSystemHandler() {
  }

  public static RenderSystemHandler getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new RenderSystemHandler();
    }
    return INSTANCE;
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
          Font.createFont(Font.PLAIN, getClass().getResourceAsStream("/fonts/InputMono-Regular.ttf"))
              .deriveFont(Font.PLAIN, 24);
      SwingTerminalFontConfiguration fontConfig =
          new SwingTerminalFontConfiguration(true, AWTTerminalFontConfiguration.BoldMode.NOTHING, font);
      terminalFactory.setTerminalEmulatorFontConfiguration(fontConfig);

      int targetWidth = 1280;
      int targetHeight = 720;

      // calculate columns and rows based on font size using a rough scaling factor of 0.625 for width and 1.18 for height
      // width scaling factor - could be a configuration parameter for different fonts.
      // Have to keep in mind these factors change depending on the font size and font family.
      fontPixelWidth = font.getSize() * .625f;
      // height scaling factor - same as above
      fontPixelHeight = font.getSize() * 1.175f;

      screenColumns = Math.round(targetWidth / fontPixelWidth);
      screenRows = Math.round(targetHeight / fontPixelHeight);

      terminalFactory.setInitialTerminalSize(new TerminalSize(screenColumns, screenRows));

      this.mainScreen = terminalFactory.createScreen();
      this.mainScreen.startScreen();
      this.mainScreen.setCursorPosition(null); // we don't need a cursor

      this.layers = new Map[MAX_LAYERS]; // initialize layer map
      for (int i = 0; i < MAX_LAYERS; i++) {  // initialize each layer
        layers[i] = new HashMap<>();
      }

      TextColor backgroundColor = new TextColor.RGB(40, 55, 40);

      this.mainBackgroundCharacter = TextCharacter.fromCharacter(' ', backgroundColor, backgroundColor)[0];
      // draw a green background to simulate a game table
      this.mainScreen.clear();
      for (int i = 0; i < screenColumns; i++) {
        for (int j = 0; j < screenRows; j++) {
          this.mainScreen.setCharacter(i, j, mainBackgroundCharacter);
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
  public void update() {
    long lastDeltaClock = System.nanoTime();
    long fpsCounterClock = System.nanoTime();
    double sleepTime = 0;

    while (running) {
      long now = System.nanoTime(); // current time in nanoseconds
      double deltaTime = (double) (now - lastDeltaClock) / SECOND_IN_NANOS; // time passed since last frame in seconds
      double elapsedTime = now - deltaTime; // accumulated time since start of loop in nanoseconds

      // calculate FPS
      if (now - fpsCounterClock >= SECOND_IN_NANOS) {
        currentFPS = frameCount;
        frameCount = 0;
        fpsCounterClock = now;

      }

      try {
        if (this.mainScreen.doResizeIfNecessary() != null) {
          // resize the screen if the terminal size has changed
          this.mainScreen.doResizeIfNecessary();
          this.screenColumns = this.mainScreen.getTerminalSize().getColumns();
          this.screenRows = this.mainScreen.getTerminalSize().getRows();

          // draw a green background to simulate a game table
          this.mainScreen.clear();
          for (int i = 0; i < screenColumns; i++) {
            for (int j = 0; j < screenRows; j++) {
              this.mainScreen.setCharacter(i, j, mainBackgroundCharacter);
            }
          }
        }

        if (running && (elapsedTime >= (RENDER_INTERVAL))) {
          // draw screen borders
          drawScreenBorders();

          TextGraphics textGraphics = this.mainScreen.newTextGraphics();
            textGraphics.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT);
            textGraphics.setBackgroundColor(this.mainBackgroundCharacter.getBackgroundColor());
            textGraphics.putString(1, 0, "FPS: %d".formatted(currentFPS));
            textGraphics.putString(1, 1, "Delta Time: %.3f sec.".formatted(deltaTime));
            textGraphics.putString(1, 2, "Sleep Time: %.3f sec.".formatted(sleepTime));

          this.mainScreen.refresh();
          frameCount++;
          lastDeltaClock = now;
        }

        // sleep for the remaining time to target render interval if needed to keep the game loop stable
        sleepTime = (RENDER_INTERVAL - (System.nanoTime() - elapsedTime)) / SECOND_IN_NANOS;
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
      this.mainScreen.stopScreen();
      this.mainScreen.close();
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
    TextGraphics textGraphics = this.mainScreen.newTextGraphics();
    // draw top border
    for (int i = 0; i < this.mainScreen.getTerminalSize().getColumns(); i++) {
      textGraphics.setCharacter(i, 0, new TextCharacter('─', TextColor.ANSI.RED, this.mainBackgroundCharacter.getBackgroundColor()));
    }

    // draw bottom border
    for (int i = 0; i < this.mainScreen.getTerminalSize().getColumns(); i++) {
      textGraphics.setCharacter(i, this.mainScreen.getTerminalSize().getRows() - 1,
              new TextCharacter('─', TextColor.ANSI.RED, this.mainBackgroundCharacter.getBackgroundColor()));
    }

    // draw left border
    for (int i = 0; i < this.mainScreen.getTerminalSize().getRows(); i++) {
      textGraphics.setCharacter(0, i, new TextCharacter('│', TextColor.ANSI.RED, this.mainBackgroundCharacter.getBackgroundColor()));
    }

    // draw right border

    for (int i = 0; i < this.mainScreen.getTerminalSize().getRows(); i++) {
      textGraphics.setCharacter(this.mainScreen.getTerminalSize().getColumns() - 1, i,
              new TextCharacter('│', TextColor.ANSI.RED, this.mainBackgroundCharacter.getBackgroundColor()));
    }

    // draw corners
    textGraphics.setCharacter(0, 0, new TextCharacter('┌', TextColor.ANSI.RED, this.mainBackgroundCharacter.getBackgroundColor()));
    textGraphics.setCharacter(this.mainScreen.getTerminalSize().getColumns() - 1, 0, new TextCharacter('┐', TextColor.ANSI.RED,
            this.mainBackgroundCharacter.getBackgroundColor()));
    textGraphics.setCharacter(0, this.mainScreen.getTerminalSize().getRows() - 1, new TextCharacter('└', TextColor.ANSI.RED,
            this.mainBackgroundCharacter.getBackgroundColor()));
    textGraphics.setCharacter(this.mainScreen.getTerminalSize().getColumns() - 1, this.mainScreen.getTerminalSize().getRows() - 1,
            new TextCharacter('┘', TextColor.ANSI.RED, this.mainBackgroundCharacter.getBackgroundColor()));
  }
}
