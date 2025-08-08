package net.luxsolari.game;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
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
    // Load logging configuration
    try (InputStream is = Main.class.getClassLoader().getResourceAsStream("logging.properties")) {
      if (is != null) {
        LogManager.getLogManager().readConfiguration(is);
      } else {
        LOGGER.warning("Could not find logging.properties file. Using default logging settings.");
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Could not read logging.properties file", e);
    }

    LOGGER.info("[%s] Starting Main".formatted(TAG));

    // Spawn master thread for the GameMasterHandler instance
    MasterSubsystem masterGameSystem = MasterSubsystem.INSTANCE;
    masterGameSystem.run();

    LOGGER.info("[%s] Exiting Main".formatted(TAG));
  }
}
