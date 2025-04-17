package net.luxsolari.engine.systems;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MasterSubsystem implements Subsystem {

  private static MasterSubsystem INSTANCE;
  private static final String TAG = MasterSubsystem.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  private static final int SECOND_IN_NANOS = 1_000_000_000;
  private static final int TARGET_UPS = 8; // 8 updates per second
  private static final long UPDATE_INTERVAL =
      TimeUnit.MILLISECONDS.toNanos(1000L / TARGET_UPS); // ~125ms per update

  // tracking statistics for FPS and UPS counters
  private long lastStatsTime = java.lang.System.nanoTime();
  private int updateCount = 0;
  private int currentUPS = 0;

  private boolean running = false;

  private MasterSubsystem() {}

  public static MasterSubsystem getInstance() {
    if (INSTANCE == null) {
      LOGGER.info("[%s] Creating new Master Game Handler instance".formatted(TAG));
      INSTANCE = new MasterSubsystem();
    }
    return INSTANCE;
  }

  @Override
  public void init() {
    LOGGER.info("[%s] Initializing Master Game Handler".formatted(TAG));
    this.start();

    RenderSubsystem renderSystem = RenderSubsystem.getInstance();
    Thread renderSystemHandlerThread = new Thread(renderSystem, "Render Subsystem Thread");
    renderSystemHandlerThread.start();
  }

  @Override
  public void start() {
    LOGGER.info("[%s] Starting Master Game Handler".formatted(TAG));
    running = true;
  }

  @Override
  @SuppressWarnings("BusyWait")
  public void update() {

    long previousUpdateTime = java.lang.System.nanoTime();
    long updateLag = 0;

    while (running) {
      try {
        long currentTime = java.lang.System.nanoTime();

        if (currentTime - lastStatsTime >= SECOND_IN_NANOS) {
          currentUPS = updateCount;
          updateCount = 0;
          lastStatsTime = currentTime;
        }

        // calculate elapsed time since last update and render
        long elapsedTime = currentTime - previousUpdateTime;

        // update lag is the time that has passed since the last update, and we need to keep track
        // of it
        // so we can update the game logic at a fixed rate, even if the rendering is slower or
        // faster
        updateLag += elapsedTime;
        previousUpdateTime = currentTime;

        // update game logic at fixed rate
        while (running && (updateLag >= UPDATE_INTERVAL)) {

          if (RenderSubsystem.getInstance().running()
              && RenderSubsystem.getInstance().mainScreen().get() != null) {
            RenderSubsystem.getInstance()
                .mainScreen()
                .get()
                .newTextGraphics()
                .setBackgroundColor(new TextColor.RGB(40, 55, 40))
                .setForegroundColor(new TextColor.RGB(255, 255, 255))
                .putString(1, 7, "Master Game Subsystem Stats")
                .putString(1, 8, "UPS: %d".formatted(currentUPS))
                .putString(1, 9, "Tick Count: %d".formatted(updateCount))
                .putString(
                    1,
                    10,
                    "Update Interval: %dms"
                        .formatted(TimeUnit.NANOSECONDS.toMillis(UPDATE_INTERVAL)));
          }

          if (RenderSubsystem.getInstance().running()
              && RenderSubsystem.getInstance().mainScreen().get() != null) {
            try {
              KeyStroke keyStroke = RenderSubsystem.getInstance().mainScreen().get().pollInput();
              if (keyStroke != null) {
                if ((keyStroke.getKeyType() == KeyType.Character
                        && keyStroke.getCharacter().toString().equalsIgnoreCase("Q"))
                    || keyStroke.getKeyType() == KeyType.EOF) {
                  this.stop();
                }
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }

          updateCount++;
          updateLag -= UPDATE_INTERVAL;
        }

        Thread.sleep(1);
        // Thread.yield();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void stop() {
    LOGGER.info("[%s] Stopping Master Game Handler".formatted(TAG));
    RenderSubsystem.getInstance().stop();
    running = false;
  }

  @Override
  public void cleanUp() {
    LOGGER.info("[%s] Cleaning up Master Game Handler".formatted(TAG));
  }
}
