package net.luxsolari.engine.ecs;

import com.googlecode.lanterna.TextCharacter;
import net.luxsolari.engine.viewport.CardSizeTier;
import java.util.Map;

/**
 * Visual component that supports multiple size tiers for scalable display.
 * Automatically selects appropriate visual representation based on available space.
 */
public final class ScalableVisual implements Component {

  private final Map<CardSizeTier, TextCharacter> tierVisuals;
  private final TextCharacter fallback;

  /**
   * Creates a scalable visual with tier-specific characters.
   *
   * @param tierVisuals map of tier to visual character
   * @param fallback fallback character if no tier matches
   */
  public ScalableVisual(Map<CardSizeTier, TextCharacter> tierVisuals, TextCharacter fallback) {
    if (tierVisuals == null || tierVisuals.isEmpty()) {
      throw new IllegalArgumentException("Tier visuals cannot be null or empty");
    }
    this.tierVisuals = Map.copyOf(tierVisuals);
    this.fallback = fallback != null ? fallback : TextCharacter.fromCharacter('?')[0];
  }

  /**
   * Creates a scalable visual with a single character for all tiers.
   *
   * @param character the character to use for all tiers
   */
  public ScalableVisual(TextCharacter character) {
    this.tierVisuals = Map.of(
        CardSizeTier.SMALL, character,
        CardSizeTier.MEDIUM, character,
        CardSizeTier.LARGE, character
    );
    this.fallback = character;
  }

  /**
   * Gets the appropriate visual character for the given tier.
   *
   * @param tier the card size tier
   * @return the visual character for that tier
   */
  public TextCharacter getVisualForTier(CardSizeTier tier) {
    return tierVisuals.getOrDefault(tier, fallback);
  }

  /**
   * Checks if this visual supports the given tier.
   *
   * @param tier the tier to check
   * @return true if the tier is supported
   */
  public boolean supportsTier(CardSizeTier tier) {
    return tierVisuals.containsKey(tier);
  }

  /**
   * Gets all supported tiers.
   *
   * @return set of supported tiers
   */
  public java.util.Set<CardSizeTier> getSupportedTiers() {
    return tierVisuals.keySet();
  }

  /**
   * Creates a builder for constructing scalable visuals.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating ScalableVisual instances.
   */
  public static class Builder {
    private final Map<CardSizeTier, TextCharacter> tierVisuals = new java.util.HashMap<>();
    private TextCharacter fallback;

    /**
     * Adds a visual character for a specific tier.
     *
     * @param tier the tier
     * @param character the character for that tier
     * @return this builder
     */
    public Builder withTier(CardSizeTier tier, TextCharacter character) {
      tierVisuals.put(tier, character);
      return this;
    }

    /**
     * Adds a visual character for a specific tier.
     *
     * @param tier the tier
     * @param character the character for that tier
     * @return this builder
     */
    public Builder withTier(CardSizeTier tier, char character) {
      return withTier(tier, TextCharacter.fromCharacter(character)[0]);
    }

    /**
     * Sets the fallback character.
     *
     * @param character the fallback character
     * @return this builder
     */
    public Builder withFallback(TextCharacter character) {
      this.fallback = character;
      return this;
    }

    /**
     * Sets the fallback character.
     *
     * @param character the fallback character
     * @return this builder
     */
    public Builder withFallback(char character) {
      return withFallback(TextCharacter.fromCharacter(character)[0]);
    }

    /**
     * Builds the scalable visual.
     *
     * @return new ScalableVisual instance
     */
    public ScalableVisual build() {
      return new ScalableVisual(tierVisuals, fallback);
    }
  }
}