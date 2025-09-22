package net.luxsolari.engine.viewport;

/**
 * Defines the different size tiers for card display based on available terminal space.
 * Each tier represents a different resolution/detail level for card ASCII art.
 */
public enum CardSizeTier {
  /** Small cards for constrained viewports (5x7 characters) */
  SMALL(5, 7),

  /** Medium cards for standard viewports (7x9 characters) */
  MEDIUM(7, 9),

  /** Large cards for expanded viewports (10x13 characters) */
  LARGE(10, 13);

  /** Width of cards in this tier (in terminal columns) */
  public final int width;

  /** Height of cards in this tier (in terminal rows) */
  public final int height;

  CardSizeTier(int width, int height) {
    this.width = width;
    this.height = height;
  }

  /**
   * Gets the appropriate tier based on available screen space.
   *
   * @param availableWidth available terminal width
   * @param availableHeight available terminal height
   * @param cardCount number of cards to display
   * @return the best fitting card tier
   */
  public static CardSizeTier getBestFit(int availableWidth, int availableHeight, int cardCount) {
    if (cardCount == 0) {
      return MEDIUM; // Default when no cards present
    }

    // Calculate space needed for each tier
    int spacing = 1; // Space between cards

    for (CardSizeTier tier : new CardSizeTier[] {LARGE, MEDIUM, SMALL}) {
      int totalWidth = (cardCount * tier.width) + ((cardCount - 1) * spacing);
      int totalHeight = tier.height;

      // Leave some margin for UI elements
      int marginWidth = Math.max(20, availableWidth / 10);
      int marginHeight = Math.max(10, availableHeight / 5);

      if (totalWidth <= (availableWidth - marginWidth)
          && totalHeight <= (availableHeight - marginHeight)) {
        return tier;
      }
    }

    return SMALL; // Fallback to smallest if nothing fits
  }

  /**
   * Gets the next smaller tier, or the same tier if already at minimum.
   *
   * @return smaller tier or this tier if already minimum
   */
  public CardSizeTier smaller() {
    return switch (this) {
      case LARGE -> MEDIUM;
      case MEDIUM -> SMALL;
      case SMALL -> SMALL;
    };
  }

  /**
   * Gets the next larger tier, or the same tier if already at maximum.
   *
   * @return larger tier or this tier if already maximum
   */
  public CardSizeTier larger() {
    return switch (this) {
      case SMALL -> MEDIUM;
      case MEDIUM -> LARGE;
      case LARGE -> LARGE;
    };
  }
}