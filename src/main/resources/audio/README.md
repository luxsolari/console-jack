# Audio Assets

This directory contains all audio assets for Console Jack, organized into BGM (background music) and SFX (sound effects).

## Directory Structure

```
audio/
├── bgm/                    # Background Music
│   ├── casino_downtown.wav     # Downtown casino ambient music
│   ├── casino_upscale.wav      # Upscale casino theme
│   ├── casino_elite.wav        # Elite casino theme
│   └── menu_theme.wav          # Main menu music
└── sfx/                    # Sound Effects
    ├── card_deal.wav           # Card dealing sound
    ├── card_shuffle.wav        # Card shuffling sound
    ├── card_flip.wav           # Card flipping sound
    ├── chip_place_small.wav    # Small chip placement
    ├── chip_place_medium.wav   # Medium chip placement
    ├── chip_place_large.wav    # Large chip placement
    ├── win_small.wav           # Small win celebration
    ├── win_big.wav             # Big win celebration
    ├── win_blackjack.wav       # Blackjack win sound
    ├── lose.wav                # Loss sound
    └── button_click.wav        # UI button click

```

## Audio Format Requirements

- **Format**: WAV (recommended) or OGG
- **Sample Rate**: 44.1kHz
- **Bit Depth**: 16-bit
- **Channels**: Stereo (2 channels)
- **Encoding**: PCM (for WAV)

## Asset Guidelines

### BGM (Background Music)
- **Loop-friendly**: Should seamlessly loop without noticeable gaps
- **Length**: 1-3 minutes per track to avoid repetition fatigue
- **Volume**: Should be ambient, not overpowering SFX
- **Style**: Match casino atmosphere - jazzy, sophisticated, ambient

### SFX (Sound Effects)
- **Short Duration**: Most SFX should be under 2 seconds
- **Clear Impact**: Distinct, recognizable sounds
- **Consistent Volume**: Similar perceived loudness across all SFX
- **No Clipping**: Clean audio without distortion

## Usage in Code

Assets are loaded automatically at startup and accessed via string IDs:

```java
// BGM Control
AudioManager.playBGM("casino_downtown", true);  // Loop casino music
AudioManager.stopBGM();

// SFX Control
AudioManager.playSFX("card_deal");              // Play card sound
AudioManager.playSFX("chip_place_large", 0.8f, 0.0f); // Custom volume/pan

// Volume Control
AudioManager.setMasterVolume(0.8f);             // Overall volume
AudioManager.setBGMVolume(0.6f);                // BGM specific
AudioManager.setSFXVolume(0.9f);                // SFX specific
```

## Asset Sources

Audio assets should be sourced from:
- **Royalty-free sites**: Freesound.org, Zapsplat, Adobe Stock Audio
- **Creative Commons**: Ensure proper licensing
- **Generated**: Tools like Audacity, LMMS for simple effects

## Notes

- Missing audio files will be logged as warnings but won't crash the game
- The system supports concurrent SFX playback (multiple sounds simultaneously)
- BGM will seamlessly switch when changing casino venues
- All audio processing happens on a dedicated thread for optimal performance
