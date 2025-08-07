package net.luxsolari.engine.systems.internal;

import com.googlecode.lanterna.TextColor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import net.luxsolari.engine.ecs.EcsSystem;
import net.luxsolari.engine.ecs.EntityPool;
import net.luxsolari.engine.ecs.systems.DisplayListSystem;
import net.luxsolari.engine.manager.StateMachineManager;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.Subsystem;
import net.luxsolari.game.states.MainMenuState;

/**
 * Master subsystem implemented as an enum singleton (see {@link #INSTANCE}).
 * It coordinates the main game loop, delegates updates/rendering to other subsystems,
 * and manages the {@link net.luxsolari.engine.manager.StateMachineManager} for state transitions.
 */
public enum MasterSubsystem implements Subsystem {

  INSTANCE;

  private static final String TAG = MasterSubsystem.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  private static final int SECOND_IN_NANOS = 1_000_000_000;
  private static final int TARGET_UPS = 8; // 8 updates per second
  private static final long UPDATE_INTERVAL =
      TimeUnit.MILLISECONDS.toNanos(1000L / TARGET_UPS); // ~125ms per update

  // tracking statistics for FPS and UPS counters
  private long lastStatsTime = System.nanoTime();
  private int updateCount = 0;
  private int currentUps = 0;
  private boolean running = false;

  // --- ECS ---
  private final EntityPool entityPool = new EntityPool();
  private final List<EcsSystem> ecsSystems = new ArrayList<>();

  public EntityPool getEntityPool() {
    return entityPool;
  }

  @Override
  public void init() {
    LOGGER.info("[%s] Initializing Master Game Handler".formatted(TAG));
    this.start();

    startRenderSubsystem();
    startInputSubsystem();
    startAudioSubsystem();

    // Push initial game state (Main Menu)
    StateMachineManager.push(new MainMenuState());

    // --- ECS setup ---
    ecsSystems.add(new DisplayListSystem());
  }

  private void startAudioSubsystem() {
    AudioSubsystem audioSystem = AudioSubsystem.INSTANCE;
    Thread audioSystemHandlerThread = new Thread(audioSystem, "Audio Subsystem Thread");
    audioSystemHandlerThread.start();
  }

  private void startInputSubsystem() {
    InputSubsystem inputSystem = InputSubsystem.INSTANCE;
    Thread inputSystemHandlerThread = new Thread(inputSystem, "Input Subsystem Thread");
    inputSystemHandlerThread.start();
  }

  private void startRenderSubsystem() {
    RenderSubsystem renderSystem = RenderSubsystem.INSTANCE;
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

    try {
      RenderSubsystem.INSTANCE.getInitializedFuture().get();
    } catch (Exception e) {
      LOGGER.severe("Failed to wait for RenderSubsystem: " + e.getMessage());
      Thread.currentThread().interrupt();
      return;
    }

    long previousUpdateTime = System.nanoTime();
    long updateLag = 0;

    while (running) {
      try {
        long currentTime = System.nanoTime();

        if (currentTime - lastStatsTime >= SECOND_IN_NANOS) {
          currentUps = updateCount;
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

          RenderSubsystem.INSTANCE
              .mainScreen()
              .get()
              .newTextGraphics()
              .setBackgroundColor(new TextColor.RGB(40, 55, 40))
              .setForegroundColor(new TextColor.RGB(255, 255, 255))
              .putString(1, 7, "Master Game Subsystem Stats")
              .putString(1, 8, "UPS: %d".formatted(currentUps))
              .putString(1, 9, "Tick Count: %d".formatted(updateCount))
              .putString(
                  1,
                  10,
                  "Update Interval: %dms"
                      .formatted(TimeUnit.NANOSECONDS.toMillis(UPDATE_INTERVAL)));

          // ----- STATE MACHINE -----
          LoopableState active = StateMachineManager.active();
          if (active != null) {
            active.handleInput();
            active.update();
            active.render();
          }

          // ----- ECS SYSTEMS -----
          double dtSec = UPDATE_INTERVAL / (double) SECOND_IN_NANOS;
          ecsSystems.forEach(sys -> sys.update(dtSec, entityPool));

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
    RenderSubsystem.INSTANCE.stop();
    InputSubsystem.INSTANCE.stop();
    AudioSubsystem.INSTANCE.stop();
    running = false;
  }

  @Override
  public void cleanUp() {
    LOGGER.info("[%s] Cleaning up Master Game Handler".formatted(TAG));
    StateMachineManager.clear();
  }
}
