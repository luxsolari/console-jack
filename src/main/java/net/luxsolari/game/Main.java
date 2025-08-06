package net.luxsolari.game;

import java.util.logging.Logger;
import net.luxsolari.engine.systems.internal.MasterSubsystem;

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
    MasterSubsystem masterGameSystem = MasterSubsystem.INSTANCE;
    masterGameSystem.run();

    LOGGER.info("[%s] Exiting Main".formatted(TAG));
  }
}
