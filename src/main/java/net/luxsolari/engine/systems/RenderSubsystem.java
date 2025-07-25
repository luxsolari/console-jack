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
import net.luxsolari.engine.records.ZLayer;
import net.luxsolari.engine.records.ZLayerData;
import net.luxsolari.engine.records.ZLayerPosition;

public class RenderSubsystem implements Subsystem {
  private static RenderSubsystem INSTANCE;

  private static final String TAG = RenderSubsystem.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  private static final int TARGET_FPS =
      30; // Target frames per second, this is the maximum FPS we want to achieve.
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
          drawMainScreenBorders();
          displayRenderStats(deltaTime, sleepTime);

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

  private void drawMainScreenBorders() {
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

  private void displayRenderStats(double deltaTime, double sleepTime) {
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
   * Clears all contents from a specific rendering layer.
   *
   * @param layerIndex The index of the layer to clear
   */
  public void clearLayer(int layerIndex) {
    ZLayer zlayer = new ZLayer("Layer %d".formatted(layerIndex), layerIndex);
    layers.put(zlayer, new ZLayerData(new ConcurrentHashMap<>()));
  }

  /** Clears all contents from all rendering layers. */
  public void clearAllLayers() {
    for (int i = 0; i < MAX_LAYERS; i++) {
      clearLayer(i);
    }
  }

  public Map<ZLayer, ZLayerData> getLayers() {
    return layers;
  }
}
