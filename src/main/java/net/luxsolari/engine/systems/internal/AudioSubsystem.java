package net.luxsolari.engine.systems.internal;

import com.adonax.audiocue.AudioCue;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import net.luxsolari.engine.systems.Subsystem;

/**
 * Audio subsystem implemented as an enum singleton (see {@link #INSTANCE}) following Effective Java
 * Item 3. It manages audio initialization, lifecycle control and update logic for all in-game
 * sounds.
 */
public enum AudioSubsystem implements Subsystem {
  INSTANCE;

  private static final String TAG = AudioSubsystem.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private boolean running = false;

  // Dictionary-based asset management
  private final Map<String, AudioCue> loadedBGM = new ConcurrentHashMap<>();
  private final Map<String, AudioCue> loadedSFX = new ConcurrentHashMap<>();

  // Volume controls
  private float masterVolume = 1.0f;
  private float bgmVolume = 1.0f;
  private float sfxVolume = 1.0f;

  // Current BGM tracking
  private AudioCue currentBGM = null;

  public boolean running() {
    return running;
  }

  @Override
  public void init() {
    LOGGER.info("[%s] Initializing Audio Subsystem".formatted(TAG));
    loadAudioAssets();
    this.start();
  }

  @Override
  public void start() {
    LOGGER.info("[%s] Starting Audio Subsystem".formatted(TAG));
    running = true;
  }

  @Override
  public void update() {
    try {
      // Block and wait until the RenderSubsystem is fully initialized.
      RenderSubsystem.INSTANCE.getInitializedFuture().get();
    } catch (Exception e) {
      LOGGER.severe("[%s] Failed to wait for RenderSubsystem: %s".formatted(TAG, e.getMessage()));
      Thread.currentThread().interrupt(); // Preserve the interrupted status
      return; // Exit if we can't initialize
    }

    while (running) {
      // TODO Add audio update logic here.
      try {
        Thread.sleep(16); // ~60fps, prevents busy-wait
      } catch (InterruptedException ie) {
        LOGGER.warning("[%s] Audio update loop interrupted: %s".formatted(TAG, ie.getMessage()));
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  @Override
  public void stop() {
    LOGGER.info("[%s] Stopping Audio Subsystem".formatted(TAG));
    running = false;
  }

  @Override
  public void cleanUp() {
    LOGGER.info("[%s] Cleaning Up Audio Subsystem".formatted(TAG));
    // Stop current BGM
    stopBGM();
    // Close all loaded audio resources
    for (AudioCue bgm : loadedBGM.values()) {
      bgm.close();
    }
    for (AudioCue sfx : loadedSFX.values()) {
      sfx.close();
    }
    loadedBGM.clear();
    loadedSFX.clear();
  }

  /** Returns true if the subsystem is running and the RenderSubsystem screen is available. */
  public boolean ready() {
    return running && RenderSubsystem.INSTANCE.ready();
  }

  /** Loads all audio assets into dictionaries for fast access during gameplay. */
  private void loadAudioAssets() {
    LOGGER.info("[%s] Loading audio assets...".formatted(TAG));

    // Load BGM assets
    loadBGMAsset("casino_downtown", "/audio/bgm/casino_downtown.wav");
    loadBGMAsset("casino_upscale", "/audio/bgm/casino_upscale.wav");
    loadBGMAsset("casino_elite", "/audio/bgm/casino_elite.wav");
    loadBGMAsset("menu_theme", "/audio/bgm/menu_theme.wav");
    loadBGMAsset("menu_theme_2", "/audio/bgm/menu_theme_2.wav");

    // Load SFX assets (with max concurrent play counts)
    loadSFXAsset("card_deal", "/audio/sfx/card_deal.wav", 4);
    loadSFXAsset("card_shuffle", "/audio/sfx/card_shuffle.wav", 2);
    loadSFXAsset("card_flip", "/audio/sfx/card_flip.wav", 3);
    loadSFXAsset("chip_place_small", "/audio/sfx/chip_place_small.wav", 6);
    loadSFXAsset("chip_place_medium", "/audio/sfx/chip_place_medium.wav", 6);
    loadSFXAsset("chip_place_large", "/audio/sfx/chip_place_large.wav", 6);
    loadSFXAsset("win_small", "/audio/sfx/win_small.wav", 2);
    loadSFXAsset("win_big", "/audio/sfx/win_big.wav", 2);
    loadSFXAsset("win_blackjack", "/audio/sfx/win_blackjack.wav", 1);
    loadSFXAsset("lose", "/audio/sfx/lose.wav", 2);
    loadSFXAsset("button_click", "/audio/sfx/button_click.wav", 4);

    LOGGER.info(
        "[%s] Audio asset loading completed. BGM: %d, SFX: %d"
            .formatted(TAG, loadedBGM.size(), loadedSFX.size()));
  }

  /** Loads a BGM asset into the BGM dictionary. */
  private void loadBGMAsset(String id, String resourcePath) {
    try {
      URL audioURL = getClass().getResource(resourcePath);
      if (audioURL != null) {
        AudioCue bgm = AudioCue.makeStereoCue(audioURL, 1);
        bgm.open();
        loadedBGM.put(id, bgm);
        LOGGER.fine("[%s] Loaded BGM: %s".formatted(TAG, id));
      } else {
        LOGGER.warning("[%s] BGM resource not found: %s".formatted(TAG, resourcePath));
      }
    } catch (Exception e) {
      LOGGER.warning("[%s] Failed to load BGM '%s': %s".formatted(TAG, id, e.getMessage()));
    }
  }

  /** Loads an SFX asset into the SFX dictionary. */
  private void loadSFXAsset(String id, String resourcePath, int maxConcurrent) {
    try {
      URL audioURL = getClass().getResource(resourcePath);
      if (audioURL != null) {
        AudioCue sfx = AudioCue.makeStereoCue(audioURL, maxConcurrent);
        sfx.open();
        loadedSFX.put(id, sfx);
        LOGGER.fine("[%s] Loaded SFX: %s (max concurrent: %d)".formatted(TAG, id, maxConcurrent));
      } else {
        LOGGER.warning("[%s] SFX resource not found: %s".formatted(TAG, resourcePath));
      }
    } catch (Exception e) {
      LOGGER.warning("[%s] Failed to load SFX '%s': %s".formatted(TAG, id, e.getMessage()));
    }
  }

  /** Plays background music by ID. */
  public synchronized void playBGM(String bgmId, boolean loop) {
    AudioCue bgm = loadedBGM.get(bgmId);
    if (bgm == null) {
      LOGGER.warning("[%s] BGM not found: %s".formatted(TAG, bgmId));
      return;
    }

    // Stop current BGM if different
    if (currentBGM != null && currentBGM != bgm) {
      currentBGM.stop(0);
      currentBGM.releaseInstance(0);
    }

    currentBGM = bgm;
    float effectiveVolume = masterVolume * bgmVolume;

    if (loop) {
      currentBGM.play(effectiveVolume, 0.0f, 1.0f, -1); // -1 = infinite loop
    } else {
      currentBGM.play(effectiveVolume, 0.0f, 1.0f, 0);
    }

    LOGGER.info(
        "[%s] Playing BGM: %s (loop: %b, volume: %.2f)"
            .formatted(TAG, bgmId, loop, effectiveVolume));
  }

  /** Stops currently playing background music. */
  public synchronized void stopBGM() {
    if (currentBGM != null) {
      currentBGM.stop(0);
      currentBGM.releaseInstance(0);
      currentBGM = null;
      LOGGER.info("[%s] Stopped BGM".formatted(TAG));
    }
  }

  /** Plays a sound effect by ID. */
  public void playSFX(String sfxId) {
    playSFX(sfxId, sfxVolume, 0.0f); // Default volume and center pan
  }

  /** Plays a sound effect by ID with custom volume and pan. */
  public void playSFX(String sfxId, float volume, float pan) {
    AudioCue sfx = loadedSFX.get(sfxId);
    if (sfx == null) {
      LOGGER.warning("[%s] SFX not found: %s".formatted(TAG, sfxId));
      return;
    }

    float effectiveVolume = masterVolume * volume;
    sfx.play(effectiveVolume, pan, 1.0f, 0); // No looping for SFX
    LOGGER.fine(
        "[%s] Playing SFX: %s (volume: %.2f, pan: %.2f)"
            .formatted(TAG, sfxId, effectiveVolume, pan));
  }

  /** Sets the master volume level (0.0f to 1.0f). */
  public void setMasterVolume(float volume) {
    this.masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
    LOGGER.info("[%s] Master volume set to: %.2f".formatted(TAG, this.masterVolume));
  }

  /** Sets the BGM volume level (0.0f to 1.0f). */
  public void setBGMVolume(float volume) {
    this.bgmVolume = Math.max(0.0f, Math.min(1.0f, volume));
    LOGGER.info("[%s] BGM volume set to: %.2f".formatted(TAG, this.bgmVolume));
  }

  /** Sets the SFX volume level (0.0f to 1.0f). */
  public void setSFXVolume(float volume) {
    this.sfxVolume = Math.max(0.0f, Math.min(1.0f, volume));
    LOGGER.info("[%s] SFX volume set to: %.2f".formatted(TAG, this.sfxVolume));
  }

  // Volume getters
  public float getMasterVolume() {
    return masterVolume;
  }

  public float getBGMVolume() {
    return bgmVolume;
  }

  public float getSFXVolume() {
    return sfxVolume;
  }
}
