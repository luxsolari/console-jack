package net.luxsolari;


import net.luxsolari.systems.MasterGameSubsystem;

import java.util.logging.Logger;

public class Main {
  private static final String TAG = Main.class.getSimpleName() + "System";
  private static final Logger LOGGER = Logger.getLogger(TAG);

  public static void main(String[] args) {
    LOGGER.info("[%s] Starting Main".formatted(TAG));

    // Spawn master thread for GameMasterHandler instance
    MasterGameSubsystem masterGameSystem = MasterGameSubsystem.getInstance();
    Thread masterGameHandlerThread = new Thread(masterGameSystem, "MasterHandler");
    masterGameHandlerThread.start();

    try {
      masterGameHandlerThread.join();
    } catch (InterruptedException e) {
      LOGGER.severe("[%s] Error while joining Master Game Handler thread: %s"
          .formatted(TAG, e.getMessage()));
    }
    LOGGER.info("[%s] Exiting Main".formatted(TAG));
  }
}
