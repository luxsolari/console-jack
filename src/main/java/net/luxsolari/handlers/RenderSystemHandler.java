package net.luxsolari.handlers;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.swing.AWTTerminalFontConfiguration;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFontConfiguration;
import net.luxsolari.exceptions.ResourceInitializationException;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RenderSystemHandler implements SystemHandler {
  private static RenderSystemHandler INSTANCE;
  private static final String TAG = RenderSystemHandler.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  private static final int TARGET_UPS = 4;  // 4 updates per second
  private static final int TARGET_FPS = 2; //  2 frames per second

  private static final int SECOND_IN_NANOS = 1_000_000_000;
  private static final long UPDATE_INTERVAL = TimeUnit.MILLISECONDS.toNanos(1000L / TARGET_UPS); // ~250ms per update
  private static final long RENDER_INTERVAL = TimeUnit.MILLISECONDS.toNanos(1000L / TARGET_FPS); // ~500ms per render

  // tracking statistics for FPS and UPS counters
  private long lastStatsTime = System.nanoTime();
  private int frameCount = 0;
  private int updateCount = 0;
  private int currentFPS = 0;
  private int currentUPS = 0;
  private int screenColumns;
  private int screenRows;

  private boolean running = false;
  private float fontPixelWidth;
  private float fontPixelHeight;
  private Screen screen;

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

      // calculate columns and rows based on font size using a rough scaling factor of 0.625 for width and 1.18 for height
      // width scaling factor - could be a configuration parameter for different fonts.
      // Have to keep in mind these factors change depending on the font size and font family.
      fontPixelWidth = font.getSize() * .625f;
      // height scaling factor - same as above
      fontPixelHeight = font.getSize() * 1.18f;

      screenColumns = Math.round(targetWidth / fontPixelWidth);
      screenRows = Math.round(targetHeight / fontPixelHeight);

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
    LOGGER.info("[%s] Starting Render System".formatted(TAG));
    running = true;
  }

  @Override
  public void update() {
    
  }

  @Override
  public void stop() {

  }

  @Override
  public void cleanUp() {

  }
}
