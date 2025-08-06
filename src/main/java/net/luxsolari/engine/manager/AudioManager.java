package net.luxsolari.engine.manager;

import java.util.logging.Logger;
import net.luxsolari.engine.systems.internal.AudioSubsystem;

/**
 * Public façade for the internal {@link AudioSubsystem}.
 *
 * <p>External game logic can interact with the audio system via this class, without depending on
 * the internal subsystem implementation. This keeps the subsystem encapsulated while providing a
 * minimal, stateless API surface.
 */
public final class AudioManager {

  private static final String TAG = AudioManager.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  // Utility class - no instances
  private AudioManager() {}

  /** Returns {@code true} if the audio subsystem is initialized and ready. */
  public static boolean ready() {
    return AudioSubsystem.INSTANCE.running();
  }

  /**
   * High-level helper to play a sound effect. For now this is just a stub that logs the request
   * until the audio pipeline is fully implemented.
   *
   * @param soundId identifier of the sound to play (e.g. filename or enum)
   */
  public static void play(String soundId) {
    if (!ready()) {
      LOGGER.warning("[" + TAG + "] Audio subsystem not ready. Ignoring play request for: " + soundId);
      return;
    }
    // TODO: delegate to AudioSubsystem once concrete playback implementation exists
    LOGGER.info("[" + TAG + "] (stub) would play sound: " + soundId);
  }
}
