package net.luxsolari.engine.systems.internal;

import net.luxsolari.engine.ecs.systems.DisplayListSystem;
import net.luxsolari.engine.exceptions.GameLoopException;
import net.luxsolari.engine.manager.RenderManager;
import net.luxsolari.engine.manager.StateMachineManager;
import net.luxsolari.engine.manager.ViewportManager;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.Subsystem;
import net.luxsolari.game.states.MainMenuState;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Central coordinator of the entire engine, implemented as an enum singleton.
 *
 * <p><strong>Internal API — game code must not reference this type directly.</strong>
 * Use {@link net.luxsolari.engine.manager.MasterManager} instead. This class is an
 * engine-internal implementation detail and may change without notice.
 *
 * <h2>Role in the engine</h2>
 * <p>{@code MasterSubsystem} owns the main game loop and is the root of the engine's
 * lifecycle. Specifically it:
 * <ul>
 *   <li>Spawns and starts the {@link RenderSubsystem}, {@link InputSubsystem}, and
 *       {@link AudioSubsystem} on their own dedicated threads.</li>
 *   <li>Runs the fixed-timestep update loop (see {@link #update()}) on the
 *       <em>calling</em> (main) thread.</li>
 *   <li>Drives the active {@link net.luxsolari.engine.states.LoopableState} through
 *       its per-tick {@code handleInput → update → render} sequence via
 *       {@link net.luxsolari.engine.manager.StateMachineManager}.</li>
 *   <li>Ticks all registered ECS systems through
 *       {@link EntityCoordinator#INSTANCE}.</li>
 * </ul>
 *
 * <h2>Threading model</h2>
 * <pre>
 *   Main thread          ── MasterSubsystem.update() game loop
 *   "Render Subsystem Thread" ── RenderSubsystem.run()
 *   "Input Subsystem Thread"  ── InputSubsystem.run()
 *   "Audio Subsystem Thread"  ── AudioSubsystem.run()
 * </pre>
 *
 * <h2>Update rate</h2>
 * <p>The loop targets {@value #TARGET_UPS} updates per second ({@value #UPDATE_INTERVAL} ns
 * per tick). A fixed-timestep accumulator absorbs wall-clock jitter so game logic always
 * receives a deterministic {@code dt}.
 *
 * @see net.luxsolari.engine.manager.MasterManager
 * @see net.luxsolari.engine.systems.Subsystem
 */
public enum MasterSubsystem implements Subsystem {

  /**
   * Singleton instance — access the subsystem exclusively through this constant.
   */
  INSTANCE;

  private static final String TAG = MasterSubsystem.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  /** One second expressed in nanoseconds, used for the per-second UPS snapshot. */
  private static final int SECOND_IN_NANOS = 1_000_000_000;
  /**
   * Target number of logic updates per second.
   */
  private static final int TARGET_UPS = 8;
  /** Nanoseconds between consecutive logic ticks ({@code 1 000 000 000 / TARGET_UPS} ≈ 125 ms). */
  private static final long UPDATE_INTERVAL =
          TimeUnit.MILLISECONDS.toNanos(1000L / TARGET_UPS);

  // --- loop-statistics state ---
  private long lastStatsTime = System.nanoTime();
  private int updateCount = 0;
  private int currentUps = 0;
  private boolean running = false;

  /**
   * Initializes all engine subsystems and pushes the first game state.
   *
   * <p>Initialization order:
   * <ol>
   *   <li>Marks the master loop as {@link #running} via {@link #start()}.</li>
   *   <li>Spawns and starts the {@link RenderSubsystem}, {@link InputSubsystem}, and
   *       {@link AudioSubsystem} threads.</li>
   *   <li>Pushes {@link net.luxsolari.game.states.MainMenuState} as the first active
   *       state on the {@link net.luxsolari.engine.manager.StateMachineManager} stack.</li>
   *   <li>Registers the {@link net.luxsolari.engine.ecs.systems.DisplayListSystem}
   *       with the {@link EntityCoordinator}.</li>
   * </ol>
   *
   * <p>Called once by the {@link net.luxsolari.engine.systems.Subsystem#run()} default
   * implementation before {@link #update()} begins.
   */
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
    EntityCoordinator.INSTANCE.registerSystem(new DisplayListSystem());
  }

  /**
   * Spawns a daemon thread for the {@link AudioSubsystem} and starts it.
   * The thread runs the full {@link Subsystem#run()} lifecycle autonomously.
   */
  private void startAudioSubsystem() {
    AudioSubsystem audioSystem = AudioSubsystem.INSTANCE;
    Thread audioSystemHandlerThread = new Thread(audioSystem, "Audio Subsystem Thread");
    audioSystemHandlerThread.start();
  }

  /**
   * Spawns a daemon thread for the {@link InputSubsystem} and starts it.
   * The thread runs the full {@link Subsystem#run()} lifecycle autonomously.
   */
  private void startInputSubsystem() {
    InputSubsystem inputSystem = InputSubsystem.INSTANCE;
    Thread inputSystemHandlerThread = new Thread(inputSystem, "Input Subsystem Thread");
    inputSystemHandlerThread.start();
  }

  /**
   * Spawns a daemon thread for the {@link RenderSubsystem} and starts it.
   * The render thread must complete its initialization (signalled via
   * {@link RenderSubsystem#getInitializedFuture()}) before the master loop can begin
   * ticking — see {@link #update()}.
   */
  private void startRenderSubsystem() {
    RenderSubsystem renderSystem = RenderSubsystem.INSTANCE;
    Thread renderSystemHandlerThread = new Thread(renderSystem, "Render Subsystem Thread");
    renderSystemHandlerThread.start();
  }

  /**
   * Marks the master loop as ready to run by setting {@code running = true}.
   *
   * <p>Called by {@link #init()} before the subsystem threads are launched, ensuring
   * the {@link #update()} loop starts in the running state when entered immediately
   * afterward by the {@link Subsystem#run()} default implementation.
   */
  @Override
  public void start() {
    LOGGER.info("[%s] Starting Master Game Handler".formatted(TAG));
    running = true;
  }

  /**
   * Runs the fixed-timestep main game loop on the calling thread until {@link #stop()} is invoked.
   *
   * <h3>Startup barrier</h3>
   * <p>Before entering the loop this method blocks on
   * {@link RenderSubsystem#getInitializedFuture()} to guarantee the terminal window is
   * ready before the first tick attempts to draw anything. If the wait is interrupted the
   * thread's interrupt flag is restored and the method returns without entering the loop.
   *
   * <h3>Fixed-timestep accumulator</h3>
   * <p>Each outer iteration measures elapsed wall-clock time and adds it to
   * {@code updateLag}. The inner {@code while (updateLag >= UPDATE_INTERVAL)} loop then
   * drains that debt one tick at a time, each tick consuming exactly {@link #UPDATE_INTERVAL}
   * nanoseconds of virtual time. This decouples game-logic speed from rendering speed:
   * if a tick takes longer than expected the next outer iteration will simply run more
   * inner ticks to catch up, keeping physics and state transitions deterministic.
   *
   * <h3>Per-tick work (in order)</h3>
   * <ol>
   *   <li>Writes live UPS/tick diagnostics to the terminal via {@link RenderSubsystem}.</li>
   *   <li>Drives the active {@link LoopableState} through
   *       {@code handleInput → update → [handleViewportResize] → render}.</li>
   *   <li>Ticks all registered ECS systems via {@link EntityCoordinator#update(double)},
   *       passing a fixed {@code dt} of {@code UPDATE_INTERVAL / 1e9} seconds.</li>
   * </ol>
   *
   * <h3>CPU yield</h3>
   * <p>A {@code Thread.sleep(1)} at the end of each outer iteration yields the CPU rather
   * than busy-waiting, keeping idle CPU usage near zero between ticks.
   *
   * @throws GameLoopException if an unrecoverable error occurs during a tick, or if the
   *                           thread is interrupted while sleeping
   */
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
              .setBackgroundColor(RenderManager.DEFAULT_BG)
              .setForegroundColor(RenderManager.DEFAULT_FG)
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
            // We poll for viewport resizing, if so, we propagate the signal to the state so it handles it
            if (ViewportManager.INSTANCE.consumeSizeChanged()) {
              active.handleViewportResize();
            }
            active.render();
          }

          // ----- ECS SYSTEMS -----
          double dtSec = UPDATE_INTERVAL / (double) SECOND_IN_NANOS;
          EntityCoordinator.INSTANCE.update(dtSec);

          updateCount++;
          updateLag -= UPDATE_INTERVAL;
        }

        Thread.sleep(1);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOGGER.severe("[%s] Update loop interrupted: %s".formatted(TAG, e.getMessage()));
        throw new GameLoopException("Update loop interrupted", e);
      } catch (GameLoopException e) {
        LOGGER.severe("[%s] Update loop failed: %s".formatted(TAG, e.getMessage()));
        throw e;
      } catch (Exception e) {
        LOGGER.severe("[%s] Update loop failed: %s".formatted(TAG, e.getMessage()));
        throw new GameLoopException("Update loop failed", e);
      }
    }
  }

  /**
   * Signals a graceful shutdown of all subsystems.
   *
   * <p>Stops the {@link RenderSubsystem}, {@link InputSubsystem}, and
   * {@link AudioSubsystem} (in that order) by calling their respective {@code stop()}
   * methods, then clears the master {@code running} flag so the {@link #update()} loop
   * exits on its next iteration.
   *
   * <p>This method is the intended shutdown path. Game code should reach it via
   * {@link net.luxsolari.engine.manager.MasterManager#stop()}.
   */
  @Override
  public void stop() {
    LOGGER.info("[%s] Stopping Master Game Handler".formatted(TAG));
    RenderSubsystem.INSTANCE.stop();
    InputSubsystem.INSTANCE.stop();
    AudioSubsystem.INSTANCE.stop();
    running = false;
  }

  /**
   * Releases engine-level resources after the main loop has exited.
   *
   * <p>Clears the {@link net.luxsolari.engine.manager.StateMachineManager} stack,
   * invoking each state's {@code cleanUp()} so states can release their own resources
   * (terminal screens, component data, etc.) before the JVM exits.
   *
   * <p>Called automatically by the {@link Subsystem#run()} default implementation
   * immediately after {@link #update()} returns.
   */
  @Override
  public void cleanUp() {
    LOGGER.info("[%s] Cleaning up Master Game Handler".formatted(TAG));
    StateMachineManager.clear();
  }
}
