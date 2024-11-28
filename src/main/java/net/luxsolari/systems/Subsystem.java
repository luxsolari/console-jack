package net.luxsolari.systems;

/**
 * The {@code Subsystem} interface defines the basic lifecycle methods that any subsystem in the game should implement.
 * It extends the {@link Runnable} interface to allow subsystems to be executed in separate threads.
 *
 * <p>Each subsystem should follow a specific lifecycle:
 * <ul>
 *   <li>{@link #init()}: Initialize the subsystem, setting up any necessary resources.</li>
 *   <li>{@link #start()}: Start the subsystem, making it ready to run.</li>
 *   <li>{@link #update()}: Update the subsystem's state. This method is called repeatedly to perform the subsystem's main logic.</li>
 *   <li>{@link #stop()}: Stop the subsystem, halting its operations.</li>
 *   <li>{@link #cleanUp()}: Clean up the subsystem, releasing any resources that were allocated during its lifecycle.</li>
 * </ul>
 *
 * <p>The {@link #run()} method, inherited from {@code Runnable}, provides a default implementation that calls the lifecycle methods in order:
 * <ol>
 *   <li>{@link #init()}</li>
 *   <li>{@link #update()}</li>
 *   <li>{@link #cleanUp()}</li>
 * </ol>
 *
 * <p>Implementing classes should provide concrete implementations for each of these methods to define the specific behavior of the subsystem.
 */
public interface Subsystem extends Runnable {

  /**
   * Initialize the subsystem. This method is called once at the beginning of the subsystem's lifecycle.
   * Implementations should set up any necessary resources or state.
   */
  void init();

  /**
   * Start the subsystem. This method is called to make the subsystem ready to run.
   * Implementations should perform any actions necessary to start the subsystem.
   */
  void start();

  /**
   * Update the subsystem's state. This method is called repeatedly to perform the subsystem's main logic.
   * Implementations should update the subsystem's state and perform any necessary actions.
   */
  void update();

  /**
   * Stop the subsystem. This method is called to halt the subsystem's operations.
   * Implementations should perform any actions necessary to stop the subsystem.
   */
  void stop();

  /**
   * Clean up the subsystem. This method is called once at the end of the subsystem's lifecycle.
   * Implementations should release any resources that were allocated during the subsystem's lifecycle.
   */
  void cleanUp();

  /**
   * Run the subsystem. This method provides a default implementation that calls the lifecycle methods in order:
   * <ol>
   *   <li>{@link #init()}</li>
   *   <li>{@link #update()}</li>
   *   <li>{@link #cleanUp()}</li>
   * </ol>
   */
  @Override
  default void run() {
    this.init();
    this.update();
    this.cleanUp();
  }
}