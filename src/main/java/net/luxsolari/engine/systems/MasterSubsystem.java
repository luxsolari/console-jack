package net.luxsolari.engine.systems;

import com.googlecode.lanterna.TextColor;
import net.luxsolari.engine.manager.StateManager;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.game.states.MainMenuState;

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

  // State management
  private final StateManager stateManager = new StateManager();

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

    // Push initial game state (Main Menu)
    stateManager.push(new MainMenuState());
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

          // ----- STATE MACHINE -----
          LoopableState active = stateManager.active();
          if (active != null) {
            active.handleInput();
            active.update();
            active.render();
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
    stateManager.clear();
  }

  /** Exposes the engine-wide StateManager instance. */
  public StateManager stateManager() {
    return stateManager;
  }
}
