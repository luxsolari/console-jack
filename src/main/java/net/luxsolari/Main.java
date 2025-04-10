package net.luxsolari;

import java.util.logging.Logger;
import net.luxsolari.systems.MasterGameSubsystem;

/**
 * Main application class that serves as the entry point for the application. Initializes and
 * manages the MasterGameSubsystem.
 */
public class Main {
  private static final String TAG = Main.class.getSimpleName() + "System";
  private static final Logger LOGGER = Logger.getLogger(TAG);

  /**
   * Main entry point for the application. Initializes and starts the MasterGameSubsystem.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    LOGGER.info("[%s] Starting Main".formatted(TAG));

    // Spawn master thread for GameMasterHandler instance
    MasterGameSubsystem masterGameSystem = MasterGameSubsystem.getInstance();
    Thread masterGameHandlerThread = new Thread(masterGameSystem, "MasterHandler");
    masterGameHandlerThread.start();

    try {
      masterGameHandlerThread.join();
    } catch (InterruptedException e) {
      LOGGER.severe(
          "[%s] Error while joining Master Game Handler thread: %s".formatted(TAG, e.getMessage()));
    }
    LOGGER.info("[%s] Exiting Main".formatted(TAG));
  }
}
