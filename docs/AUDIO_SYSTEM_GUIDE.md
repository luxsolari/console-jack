# Console Jack - Audio System Guide

A comprehensive guide to the audio playback system for background music and sound effects in Console Jack.

## Table of Contents

- [Overview](#overview)
- [Audio Manager API](#audiomanager-api)
- [Background Music (BGM)](#background-music-bgm)
- [Sound Effects (SFX)](#sound-effects-sfx)
- [Volume Control](#volume-control)
- [Audio Assets](#audio-assets)
- [Best Practices](#best-practices)
- [Real-World Examples](#real-world-examples)
- [Troubleshooting](#troubleshooting)

---

## Overview

Console Jack's audio system provides background music and sound effects using the **AudioCue** library. The system runs on a dedicated audio thread and manages audio assets through a dictionary-based approach for fast access.

### Key Features

- **Dual-Layer Audio**: Separate control for BGM (background music) and SFX (sound effects)
- **Thread-Safe**: Dedicated audio thread prevents blocking
- **Volume Control**: Master, BGM, and SFX volume controls
- **Asset Management**: Pre-loaded assets identified by string IDs
- **Simple API**: Static facade via `AudioManager`
- **Concurrent Playback**: Multiple SFX can play simultaneously

### Audio Architecture

```
┌───────────────────────────────────────────────────────┐
│              Game State (Main Thread)                  │
│  - Calls AudioManager.playBGM(), playSFX()           │
└───────────────────┬───────────────────────────────────┘
                    ↓
┌───────────────────────────────────────────────────────┐
│            AudioManager (Static Facade)               │
│  - playBGM(), stopBGM()                              │
│  - playSFX()                                         │
│  - setMasterVolume(), etc.                           │
└───────────────────┬───────────────────────────────────┘
                    ↓
┌───────────────────────────────────────────────────────┐
│          AudioSubsystem (Audio Thread)                │
│  - Manages AudioCue instances                        │
│  - Controls playback and volume                      │
│  - Runs at ~60fps update rate                        │
└───────────────────┬───────────────────────────────────┘
                    ↓
┌───────────────────────────────────────────────────────┐
│             AudioCue Library                          │
│  - Loads WAV files (44.1kHz, 16-bit, stereo)       │
│  - Handles concurrent playback                       │
└───────────────────┬───────────────────────────────────┘
                    ↓
┌───────────────────────────────────────────────────────┐
│            Java Sound API                             │
│  - System audio output                               │
└───────────────────────────────────────────────────────┘
```

---

## AudioManager API

The `AudioManager` provides a simple, static API for all audio operations.

### Checking Audio Readiness

```java
// Check if audio subsystem is initialized and ready
boolean isReady = AudioManager.ready();

if (isReady) {
  // Safe to play audio
  AudioManager.playBGM("menu_theme", true);
}
```

**Note**: AudioManager methods automatically check readiness and log warnings if not ready.

---

## Background Music (BGM)

Background music is designed for looping audio that plays continuously during states.

### Playing BGM

```java
// Play BGM with looping
AudioManager.playBGM("menu_theme", true);  // Loops continuously

// Play BGM once (no loop)
AudioManager.playBGM("menu_theme", false);
```

**Parameters**:
- `bgmId`: String identifier for the BGM asset
- `loop`: Whether to loop the music continuously

### Stopping BGM

```java
// Stop currently playing BGM
AudioManager.stopBGM();
```

### BGM State Lifecycle

```java
public class MainMenuState implements LoopableState {

  @Override
  public void start() {
    // Start BGM when state begins
    AudioManager.playBGM("menu_theme", true);
  }

  @Override
  public void pause() {
    // Optional: stop BGM when paused
    AudioManager.stopBGM();
  }

  @Override
  public void resume() {
    // Restart BGM when resumed
    AudioManager.playBGM("menu_theme", true);
  }

  @Override
  public void end() {
    // Always stop BGM when state ends
    AudioManager.stopBGM();
  }
}
```

### Available BGM Tracks

```java
"casino_downtown"   // Downtown casino ambient music
"casino_upscale"    // Upscale casino theme
"casino_elite"      // Elite casino theme
"menu_theme"        // Main menu music
"menu_theme_2"      // Alternate menu music
```

---

## Sound Effects (SFX)

Sound effects are short audio clips for game events and user interactions.

### Playing SFX

```java
// Play SFX with default volume and pan
AudioManager.playSFX("card_deal");

// Play SFX with custom volume and pan
AudioManager.playSFX("card_deal",
    0.8f,   // Volume (0.0-1.0)
    0.0f    // Pan (-1.0 = left, 0.0 = center, 1.0 = right)
);
```

### Parameters

**Volume** (0.0f - 1.0f):
- `0.0f` = Silent
- `0.5f` = Half volume
- `1.0f` = Full volume

**Pan** (-1.0f - 1.0f):
- `-1.0f` = Hard left
- `0.0f` = Center (default)
- `1.0f` = Hard right

### Concurrent Playback

SFX supports concurrent playback (multiple sounds playing simultaneously):

```java
// These can all play at the same time
AudioManager.playSFX("card_deal");
AudioManager.playSFX("chip_place_small");
AudioManager.playSFX("button_click");
```

**Polyphony**: Each SFX has a maximum concurrent playback count (defined when loaded).

### Available SFX

```java
// Card sounds
"card_deal"         // Card dealing sound
"card_shuffle"      // Card shuffling sound
"card_flip"         // Card flipping sound

// Chip sounds
"chip_place_small"  // Small chip placement
"chip_place_medium" // Medium chip placement
"chip_place_large"  // Large chip placement

// Win/Loss sounds
"win_small"         // Small win celebration
"win_big"           // Big win celebration
"win_blackjack"     // Blackjack win sound
"lose"              // Loss sound

// UI sounds
"button_click"      // UI button click
```

---

## Volume Control

The audio system provides three-tier volume control: Master, BGM, and SFX.

### Volume Hierarchy

```
Master Volume (affects everything)
  ├── BGM Volume (affects only background music)
  └── SFX Volume (affects only sound effects)

Final Volume = Master × (BGM or SFX)
```

### Setting Volumes

```java
// Master volume (affects all audio)
AudioManager.setMasterVolume(0.8f);  // 80% volume

// BGM volume (affects only background music)
AudioManager.setBGMVolume(0.6f);     // 60% volume

// SFX volume (affects only sound effects)
AudioManager.setSFXVolume(0.9f);     // 90% volume
```

### Getting Volumes

```java
// Get current volumes
float master = AudioManager.getMasterVolume();  // Returns 0.0-1.0
float bgm = AudioManager.getBGMVolume();
float sfx = AudioManager.getSFXVolume();
```

### Volume Calculation Example

```
Master Volume: 0.8
BGM Volume: 0.6
SFX Volume: 0.9

Actual BGM playback volume: 0.8 × 0.6 = 0.48 (48%)
Actual SFX playback volume: 0.8 × 0.9 = 0.72 (72%)
```

### Volume Control in Settings

```java
// Example: Options menu with volume sliders
public class OptionsState implements LoopableState {

  private float masterVolume = AudioManager.getMasterVolume();
  private float bgmVolume = AudioManager.getBGMVolume();
  private float sfxVolume = AudioManager.getSFXVolume();

  private void adjustMasterVolume(float delta) {
    masterVolume = Math.max(0.0f, Math.min(1.0f, masterVolume + delta));
    AudioManager.setMasterVolume(masterVolume);

    // Play test sound
    AudioManager.playSFX("button_click");
  }

  private void adjustBGMVolume(float delta) {
    bgmVolume = Math.max(0.0f, Math.min(1.0f, bgmVolume + delta));
    AudioManager.setBGMVolume(bgmVolume);
  }

  private void adjustSFXVolume(float delta) {
    sfxVolume = Math.max(0.0f, Math.min(1.0f, sfxVolume + delta));
    AudioManager.setSFXVolume(sfxVolume);

    // Play test sound
    AudioManager.playSFX("button_click");
  }
}
```

---

## Audio Assets

Audio assets are stored in the resources directory and loaded at startup.

### Directory Structure

```
src/main/resources/audio/
├── bgm/                        # Background Music
│   ├── casino_downtown.wav
│   ├── casino_upscale.wav
│   ├── casino_elite.wav
│   ├── menu_theme.wav
│   └── menu_theme_2.wav
└── sfx/                        # Sound Effects
    ├── card_deal.wav
    ├── card_shuffle.wav
    ├── card_flip.wav
    ├── chip_place_small.wav
    ├── chip_place_medium.wav
    ├── chip_place_large.wav
    ├── win_small.wav
    ├── win_big.wav
    ├── win_blackjack.wav
    ├── lose.wav
    └── button_click.wav
```

### Audio Format Requirements

**AudioCue** has strict format requirements:

- **Format**: WAV files only
- **Sample Rate**: 44.1 kHz
- **Bit Depth**: 16-bit
- **Channels**: Stereo (2 channels)
- **Encoding**: PCM

**Converting Audio**:
```bash
# Using ffmpeg to convert to required format
ffmpeg -i input.mp3 -ar 44100 -ac 2 -sample_fmt s16 output.wav
```

### Asset Loading

Audio assets are loaded automatically at startup in `AudioSubsystem`:

```java
// BGM loading (single instance, can loop)
loadBGMAsset("menu_theme", "/audio/bgm/menu_theme.wav");

// SFX loading (with polyphony count for concurrent playback)
loadSFXAsset("card_deal", "/audio/sfx/card_deal.wav", 4);  // Max 4 concurrent
```

### Missing Assets

If an audio file is missing:
- A warning is logged
- Game continues without that audio
- Calls to play missing audio are silently ignored

---

## Best Practices

### 1. Always Stop BGM in end()

```java
@Override
public void end() {
  AudioManager.stopBGM();  // ✅ Prevent BGM overlap
  // ... other cleanup
}
```

### 2. Use Appropriate SFX Volume

```java
// ✅ Good: Subtle UI sounds
AudioManager.playSFX("button_click", 0.5f, 0.0f);

// ❌ Bad: All SFX at full volume
AudioManager.playSFX("button_click");  // Might be too loud
```

### 3. Don't Play SFX Too Frequently

```java
// ✅ Good: Rate-limit SFX
private long lastSoundTime = 0;
private static final long SOUND_COOLDOWN = 100; // ms

void playDealSound() {
  long now = System.currentTimeMillis();
  if (now - lastSoundTime > SOUND_COOLDOWN) {
    AudioManager.playSFX("card_deal");
    lastSoundTime = now;
  }
}

// ❌ Bad: Spam sounds every frame
void update() {
  AudioManager.playSFX("card_deal");  // Called 8 times per second!
}
```

### 4. Match Music to Game State

```java
// ✅ Good: Different music for different areas
switch (currentCasino) {
  case DOWNTOWN -> AudioManager.playBGM("casino_downtown", true);
  case UPSCALE -> AudioManager.playBGM("casino_upscale", true);
  case ELITE -> AudioManager.playBGM("casino_elite", true);
}
```

### 5. Provide Audio Options

```java
// ✅ Good: Let players control audio
public class OptionsState {
  private void toggleAudio() {
    if (AudioManager.getMasterVolume() > 0) {
      AudioManager.setMasterVolume(0.0f);  // Mute
    } else {
      AudioManager.setMasterVolume(1.0f);  // Unmute
    }
  }
}
```

### 6. Use Pan for Spatial Audio

```java
// ✅ Good: Pan based on position
float cardX = 0.25f;  // Card at 25% from left
float pan = (cardX - 0.5f) * 2.0f;  // Convert to -1.0 to 1.0
AudioManager.playSFX("card_deal", 1.0f, pan);
```

### 7. Test Without Audio

```java
// ✅ Good: Don't assume audio is available
if (AudioManager.ready()) {
  AudioManager.playSFX("card_deal");
}

// Game should work even if audio fails to initialize
```

### 8. Clean Up in Finally Blocks

```java
@Override
public void end() {
  try {
    // ... other cleanup
  } finally {
    AudioManager.stopBGM();  // ✅ Always execute
  }
}
```

---

## Real-World Examples

### Example 1: Main Menu Audio

```java
public class MainMenuState implements LoopableState {

  @Override
  public void start() {
    LOGGER.info("Main menu started");

    // Start menu music
    AudioManager.playBGM("menu_theme", true);

    // Initialize menu...
  }

  @Override
  public void pause() {
    LOGGER.info("Main menu paused");
    // Stop music when paused by overlay
    AudioManager.stopBGM();
  }

  @Override
  public void resume() {
    LOGGER.info("Main menu resumed");
    // Restart music when resumed
    AudioManager.playBGM("menu_theme", true);
  }

  @Override
  public void end() {
    LOGGER.info("Main menu ended");
    // Always stop music when leaving state
    AudioManager.stopBGM();
  }

  private void onMenuItemSelect() {
    // Play click sound
    AudioManager.playSFX("button_click");
  }
}
```

### Example 2: Gameplay Audio

```java
public class GameplayState implements LoopableState {

  @Override
  public void start() {
    // Start gameplay music
    AudioManager.playBGM("casino_downtown", true);
  }

  private void dealCard() {
    // Play deal sound
    AudioManager.playSFX("card_deal");

    // Deal card logic...
  }

  private void flipCard() {
    // Play flip sound with slight left pan
    AudioManager.playSFX("card_flip", 0.8f, -0.2f);

    // Flip card logic...
  }

  private void onPlayerWin(int amount) {
    // Play appropriate win sound based on amount
    if (amount > 1000) {
      AudioManager.playSFX("win_big");
    } else if (amount > 0) {
      AudioManager.playSFX("win_small");
    }
  }

  private void onBlackjack() {
    // Special sound for blackjack
    AudioManager.playSFX("win_blackjack");
  }

  @Override
  public void end() {
    AudioManager.stopBGM();
  }
}
```

### Example 3: Volume Settings Menu

```java
public class AudioSettingsState implements LoopableState {

  private float masterVolume;
  private float bgmVolume;
  private float sfxVolume;

  @Override
  public void start() {
    // Load current volumes
    masterVolume = AudioManager.getMasterVolume();
    bgmVolume = AudioManager.getBGMVolume();
    sfxVolume = AudioManager.getSFXVolume();

    // Create volume control UI...
  }

  @Override
  public void handleInput() {
    // Handle volume adjustments
    InputResult input = InputManager.pollCommand();
    if (input == null || input.command() == null) {
      return;
    }

    switch (input.command()) {
      case VOLUME_UP -> adjustMasterVolume(0.1f);
      case VOLUME_DOWN -> adjustMasterVolume(-0.1f);
      case TOGGLE_SOUND -> toggleMute();
    }
  }

  private void adjustMasterVolume(float delta) {
    masterVolume = Math.max(0.0f, Math.min(1.0f, masterVolume + delta));
    AudioManager.setMasterVolume(masterVolume);

    // Play test sound
    AudioManager.playSFX("button_click", 0.7f, 0.0f);
  }

  private void adjustBGMVolume(float delta) {
    bgmVolume = Math.max(0.0f, Math.min(1.0f, bgmVolume + delta));
    AudioManager.setBGMVolume(bgmVolume);
  }

  private void adjustSFXVolume(float delta) {
    sfxVolume = Math.max(0.0f, Math.min(1.0f, sfxVolume + delta));
    AudioManager.setSFXVolume(sfxVolume);

    // Play test sound
    AudioManager.playSFX("button_click", 1.0f, 0.0f);
  }

  private void toggleMute() {
    if (masterVolume > 0.0f) {
      // Mute
      AudioManager.setMasterVolume(0.0f);
    } else {
      // Unmute to previous volume
      AudioManager.setMasterVolume(masterVolume);
    }
  }

  @Override
  public void render() {
    // Render volume sliders
    renderVolumeBar("Master", masterVolume);
    renderVolumeBar("Music", bgmVolume);
    renderVolumeBar("SFX", sfxVolume);
  }

  private void renderVolumeBar(String label, float volume) {
    // Render visual volume slider...
  }
}
```

### Example 4: Betting Sounds

```java
private void placeBet(int amount) {
  // Play appropriate chip sound based on bet amount
  String chipSound;

  if (amount < 10) {
    chipSound = "chip_place_small";
  } else if (amount < 100) {
    chipSound = "chip_place_medium";
  } else {
    chipSound = "chip_place_large";
  }

  AudioManager.playSFX(chipSound, 0.8f, 0.0f);

  // Place bet logic...
}
```

---

## Troubleshooting

### No Audio Playing

**Symptom**: No sound at all

**Solutions**:
1. Check `AudioManager.ready()` returns true
2. Verify audio files exist in `src/main/resources/audio/`
3. Check system audio is not muted
4. Look for warnings in logs about missing audio files
5. Verify audio files are in correct format (WAV, 44.1kHz, 16-bit, stereo)

### Audio Cuts Off Early

**Symptom**: Sounds get cut off before finishing

**Solutions**:
1. Check if BGM is being stopped too early
2. Verify state isn't ending before sound finishes
3. Don't call `stopBGM()` immediately after `playBGM()`
4. For SFX, check polyphony count isn't exceeded

### Audio Overlapping

**Symptom**: Multiple BGM tracks playing simultaneously

**Solutions**:
1. Always call `AudioManager.stopBGM()` before playing new BGM:
```java
@Override
public void start() {
  AudioManager.stopBGM();                      // ✅ Stop previous
  AudioManager.playBGM("menu_theme", true);   // Then play new
}
```

2. Ensure `end()` method stops BGM:
```java
@Override
public void end() {
  AudioManager.stopBGM();  // ✅ Always stop
}
```

### Volume Not Working

**Symptom**: Volume changes have no effect

**Solutions**:
1. Check audio is actually playing
2. Verify values are in 0.0-1.0 range
3. Remember: Final volume = Master × (BGM or SFX)
4. Call volume setters before playing audio

### Audio Format Errors

**Symptom**: Errors loading audio files

**Solutions**:
1. Verify WAV format (not MP3, OGG, etc.)
2. Check sample rate is exactly 44.1 kHz
3. Ensure stereo (not mono)
4. Verify 16-bit depth

**Convert with ffmpeg**:
```bash
ffmpeg -i input.mp3 -ar 44100 -ac 2 -sample_fmt s16 output.wav
```

### Performance Issues

**Symptom**: Game lags when playing audio

**Solutions**:
1. Don't load large files (keep SFX under 5 seconds)
2. Reduce polyphony count for SFX
3. Don't play too many SFX simultaneously
4. Ensure audio files are optimized

---

## Additional Resources

- **Architecture Documentation**: See `ARCHITECTURE.md` for subsystem overview
- **State Machine Guide**: See `STATE_MACHINE_GUIDE.md` for state lifecycle audio management
- **Developer Guide**: See `DEVELOPER_GUIDE.md` for development workflow
- **Audio Assets**: See `docs/audio/README.md` for asset organization

**Source Code References**:
- AudioManager: `src/main/java/net/luxsolari/engine/manager/AudioManager.java`
- AudioSubsystem: `src/main/java/net/luxsolari/engine/systems/internal/AudioSubsystem.java`
- Audio assets: `src/main/resources/audio/`

**AudioCue Library**: https://github.com/philfrei/AudioCue

---

*Last Updated: 2025*
*For Console Jack - Terminal-based Blackjack Game*
