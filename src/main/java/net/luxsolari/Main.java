package net.luxsolari;


import net.luxsolari.handlers.MasterGameHandler;
import net.luxsolari.handlers.RenderSystemHandler;

import java.util.logging.Logger;

public class Main {
  private static final String TAG = Main.class.getSimpleName() + "System";
  private static final Logger LOGGER = Logger.getLogger(TAG);

  public static void main(String[] args) {
    LOGGER.info("[%s] Starting Main".formatted(TAG));

    // Spawn master thread for GameMasterHandler instance
//    MasterGameHandler masterGameHandler = MasterGameHandler.getInstance();
//    Thread masterGameHandlerThread = new Thread(masterGameHandler, "MasterHandler");
//    masterGameHandlerThread.start();
    RenderSystemHandler renderSystemHandler = RenderSystemHandler.getInstance();
    Thread renderSystemHandlerThread = new Thread(renderSystemHandler, "RenderHandler");
    renderSystemHandlerThread.start();

    try {
      // Wait for master thread to finish before exiting.
      //masterGameHandlerThread.join();
      renderSystemHandlerThread.join();
    } catch (InterruptedException e) {
      LOGGER.severe("[%s] Error while joining Master Game Handler thread: %s"
          .formatted(TAG, e.getMessage()));
    }
    LOGGER.info("[%s] Exiting Main".formatted(TAG));
  }
}