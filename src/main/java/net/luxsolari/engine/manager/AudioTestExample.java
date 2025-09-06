package net.luxsolari.engine.manager;

import java.util.logging.Logger;

/**
 * Example usage and test class for the AudioManager system.
 * This demonstrates how to use the audio system in game code.
 */
public class AudioTestExample {
    private static final Logger LOGGER = Logger.getLogger(AudioTestExample.class.getSimpleName());

    /**
     * Example method showing how to use the audio system in game scenarios.
     */
    public static void demonstrateAudioUsage() {
        LOGGER.info("=== Audio System Usage Examples ===");

        // Check if audio system is ready
        if (!AudioManager.ready()) {
            LOGGER.warning("Audio system not ready - audio calls will be ignored");
            return;
        }

        // Example 1: Playing BGM for different casino venues
        LOGGER.info("1. Playing BGM for downtown casino...");
        AudioManager.playBGM("casino_downtown", true);

        // Example 2: Playing various SFX
        LOGGER.info("2. Playing card game SFX...");
        AudioManager.playSFX("card_deal");
        AudioManager.playSFX("card_shuffle");
        AudioManager.playSFX("card_flip");

        // Example 3: Playing chip sounds with different volumes
        LOGGER.info("3. Playing chip sounds...");
        AudioManager.playSFX("chip_place_small", 0.6f, -0.3f);  // Quieter, slightly left
        AudioManager.playSFX("chip_place_medium", 0.8f, 0.0f);  // Normal volume, center
        AudioManager.playSFX("chip_place_large", 1.0f, 0.3f);   // Full volume, slightly right

        // Example 4: Volume control
        LOGGER.info("4. Adjusting volumes...");
        AudioManager.setMasterVolume(0.8f);  // 80% overall volume
        AudioManager.setBGMVolume(0.6f);     // 60% BGM volume
        AudioManager.setSFXVolume(0.9f);     // 90% SFX volume

        // Example 5: Switching BGM (simulating venue change)
        LOGGER.info("5. Switching to upscale casino BGM...");
        AudioManager.playBGM("casino_upscale", true);

        // Example 6: Win/lose sounds
        LOGGER.info("6. Playing game outcome sounds...");
        AudioManager.playSFX("win_big");
        // AudioManager.playSFX("lose");  // Commented out for demo

        // Example 7: UI interactions
        LOGGER.info("7. Playing UI sounds...");
        AudioManager.playSFX("button_click");

        LOGGER.info("=== Audio demonstration completed ===");
    }

    /**
     * Example method for casino venue-specific audio management.
     */
    public static void switchCasinoVenue(String venueType) {
        switch (venueType.toLowerCase()) {
            case "downtown" -> {
                LOGGER.info("Switching to downtown casino audio theme");
                AudioManager.playBGM("casino_downtown", true);
            }
            case "upscale" -> {
                LOGGER.info("Switching to upscale casino audio theme");
                AudioManager.playBGM("casino_upscale", true);
            }
            case "elite" -> {
                LOGGER.info("Switching to elite casino audio theme");
                AudioManager.playBGM("casino_elite", true);
            }
            case "menu" -> {
                LOGGER.info("Switching to menu audio theme");
                AudioManager.playBGM("menu_theme", true);
            }
            default -> {
                LOGGER.warning("Unknown venue type: " + venueType);
                AudioManager.stopBGM();
            }
        }
    }

    /**
     * Example method for blackjack game audio events.
     */
    public static void playBlackjackGameSounds() {
        // Game start
        AudioManager.playSFX("card_shuffle");

        // Deal cards
        AudioManager.playSFX("card_deal");
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        AudioManager.playSFX("card_deal");

        // Player actions
        AudioManager.playSFX("card_flip");  // Player checks cards
        AudioManager.playSFX("chip_place_medium");  // Player bets

        // Game outcome
        AudioManager.playSFX("win_blackjack");  // Player wins!
    }

    /**
     * Gets current volume settings as a formatted string.
     */
    public static String getVolumeStatus() {
        if (!AudioManager.ready()) {
            return "Audio system not ready";
        }

        return String.format("Audio Volumes - Master: %.1f%%, BGM: %.1f%%, SFX: %.1f%%",
            AudioManager.getMasterVolume() * 100,
            AudioManager.getBGMVolume() * 100,
            AudioManager.getSFXVolume() * 100);
    }
}
