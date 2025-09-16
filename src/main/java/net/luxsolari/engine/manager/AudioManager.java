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
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public final class AudioManager {

  private static final String TAG = AudioManager.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  // Utility class - no instances
  private AudioManager() {}

  /** Returns {@code true} if the audio subsystem is initialized and ready. */
  public static boolean ready() {
    return AudioSubsystem.INSTANCE.running();
  }

  // BGM Controls

  /**
   * Plays background music by ID.
   *
   * @param bgmId identifier of the BGM to play
   * @param loop whether to loop the BGM continuously
   */
  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  public static void playBGM(String bgmId, boolean loop) {
    if (!ready()) {
      LOGGER.warning("[" + TAG 
          + "] Audio subsystem not ready. Ignoring BGM play request for: " + bgmId);
      return;
    }
    AudioSubsystem.INSTANCE.playBGM(bgmId, loop);
  }

  /**
   * Stops currently playing background music.
   */
  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  public static void stopBGM() {
    if (!ready()) {
      LOGGER.warning("[" + TAG + "] Audio subsystem not ready. Ignoring BGM stop request.");
      return;
    }
    AudioSubsystem.INSTANCE.stopBGM();
  }

  // SFX Controls

  /**
   * Plays a sound effect by ID with default volume and pan settings.
   *
   * @param sfxId identifier of the SFX to play
   */
  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  public static void playSFX(String sfxId) {
    if (!ready()) {
      LOGGER.warning("[" + TAG 
          + "] Audio subsystem not ready. Ignoring SFX play request for: " + sfxId);
      return;
    }
    AudioSubsystem.INSTANCE.playSFX(sfxId);
  }

  /**
   * Plays a sound effect by ID with custom volume and pan.
   *
   * @param sfxId identifier of the SFX to play
   * @param volume volume level (0.0f to 1.0f)
   * @param pan stereo panning (-1.0f = left, 0.0f = center, 1.0f = right)
   */
  public static void playSFX(String sfxId, float volume, float pan) {
    if (!ready()) {
      LOGGER.warning("[" + TAG 
          + "] Audio subsystem not ready. Ignoring SFX play request for: " + sfxId);
      return;
    }
    AudioSubsystem.INSTANCE.playSFX(sfxId, volume, pan);
  }

  // Volume Controls

  /**
   * Sets the master volume level affecting all audio.
   *
   * @param volume volume level (0.0f to 1.0f)
   */
  public static void setMasterVolume(float volume) {
    if (!ready()) {
      LOGGER.warning("[" + TAG + "] Audio subsystem not ready. Ignoring master volume change.");
      return;
    }
    AudioSubsystem.INSTANCE.setMasterVolume(volume);
  }

  /**
   * Sets the BGM volume level.
   *
   * @param volume volume level (0.0f to 1.0f)
   */
  public static void setBGMVolume(float volume) {
    if (!ready()) {
      LOGGER.warning("[" + TAG + "] Audio subsystem not ready. Ignoring BGM volume change.");
      return;
    }
    AudioSubsystem.INSTANCE.setBGMVolume(volume);
  }

  /**
   * Sets the SFX volume level.
   *
   * @param volume volume level (0.0f to 1.0f)
   */
  public static void setSFXVolume(float volume) {
    if (!ready()) {
      LOGGER.warning("[" + TAG + "] Audio subsystem not ready. Ignoring SFX volume change.");
      return;
    }
    AudioSubsystem.INSTANCE.setSFXVolume(volume);
  }

  // Volume Getters

  /**
   * Gets the current master volume level.
   *
   * @return volume level (0.0f to 1.0f)
   */
  public static float getMasterVolume() {
    if (!ready()) {
      return 0.0f;
    }
    return AudioSubsystem.INSTANCE.getMasterVolume();
  }

  /**
   * Gets the current BGM volume level.
   *
   * @return volume level (0.0f to 1.0f)
   */
  public static float getBGMVolume() {
    if (!ready()) {
      return 0.0f;
    }
    return AudioSubsystem.INSTANCE.getBGMVolume();
  }

  /**
   * Gets the current SFX volume level.
   *
   * @return volume level (0.0f to 1.0f)
   */
  public static float getSFXVolume() {
    if (!ready()) {
      return 0.0f;
    }
    return AudioSubsystem.INSTANCE.getSFXVolume();
  }
}
