package net.luxsolari.game.ecs;

import net.luxsolari.game.display.CardSizeTier;

/** ASCII art templates and constants for card rendering. */
public final class CardArt {

  public static final int CARD_ROWS = 7;
  public static final int CARD_COLS = 7;

  private static final String[] ACE_SPADES_FACE = {
    "┌─────┐", "A     │", "♠     │", "│  ♠  │", "│     ♠", "│     A", "└─────┘"
  };

  private static final String[] ACE_HEARTS_FACE = {
    "┌─────┐", "A     │", "♥     │", "│  ♥  │", "│     ♥", "│     A", "└─────┘"
  };

  private static final String[] ACE_DIAMONDS_FACE = {
    "┌─────┐", "A     │", "♦     │", "│  ♦  │", "│     ♦", "│     A", "└─────┘"
  };

  private static final String[] ACE_CLUBS_FACE = {
    "┌─────┐", "A     │", "♣     │", "│  ♣  │", "│     ♣", "│     A", "└─────┘"
  };

  private static final String[] JOKER_FACE = {
    "┌─────┐",
    "JOK   │",
    "│     │",
    "│     │",
    "│     │",
    "│   JOK",
    "└─────┘"
  };

  private static final String[] DEFAULT_BACK = {
    "┌─────┐", "│░░░░░│", "│░░░░░│", "│░░░░░│", "│░░░░░│", "│░░░░░│", "└─────┘"
  };

  /**
   * Returns the ASCII art face for the Ace of Spades.
   *
   * @return a String array representing the Ace of Spades face.
   */
  public static String[] aceOfSpadesFace() {
    return ACE_SPADES_FACE;
  }

  /**
   * Returns the ASCII art face for the Ace of Hearts.
   *
   * @return a String array representing the Ace of Hearts face.
   */
  public static String[] aceOfHeartsFace() {
    return ACE_HEARTS_FACE;
  }

  /**
   * Returns the ASCII art face for the Ace of Diamonds.
   *
   * @return a String array representing the Ace of Diamonds face.
   */
  public static String[] aceOfDiamondsFace() {
    return ACE_DIAMONDS_FACE;
  }

  /**
   * Returns the ASCII art face for the Ace of Clubs.
   *
   * @return a String array representing the Ace of Clubs face.
   */
  public static String[] aceOfClubsFace() {
    return ACE_CLUBS_FACE;
  }

  /**
   * Returns the ASCII art face for the Joker card.
   *
   * @return a String array representing the Joker face.
   */
  public static String[] jokerFace() {
    return JOKER_FACE;
  }

  /**
   * Returns the ASCII art for the default card back.
   *
   * @return a String array representing the default card back.
   */
  public static String[] defaultBack() {
    return DEFAULT_BACK;
  }

  private CardArt() {}

  /**
   * Generates a standard card face sprite based on the provided card's rank and suit.
   *
   * @param card The card to generate a sprite for.
   * @return A string array representing the card's face art.
   */
  public static String[] fromCard(Card card) {
    return fromCard(card, CardSizeTier.MEDIUM);
  }

  /**
   * Generates a card face sprite based on the provided card and size tier.
   *
   * @param card The card to generate a sprite for.
   * @param tier The size tier to generate art for.
   * @return A string array representing the card's face art.
   */
  public static String[] fromCard(Card card, CardSizeTier tier) {
    String rankLabel = card.rank().label();
    char suitSymbol = card.suit().symbol();

    return switch (tier) {
      case SMALL -> generateSmallCard(rankLabel, suitSymbol);
      case MEDIUM -> generateMediumCard(rankLabel, suitSymbol);
      case LARGE -> generateLargeCard(rankLabel, suitSymbol);
    };
  }

  /**
   * Generates the default card back for the specified tier.
   *
   * @param tier The size tier to generate art for.
   * @return A string array representing the card back.
   */
  public static String[] defaultBack(CardSizeTier tier) {
    return switch (tier) {
      case SMALL -> generateSmallBack();
      case MEDIUM -> DEFAULT_BACK;
      case LARGE -> generateLargeBack();
    };
  }

  private static String[] generateSmallCard(String rank, char suit) {
    // 5x7 card for SMALL tier
    return new String[] {
      "┌───┐",
      String.format("│%-2s │", rank.length() > 1 ? rank.substring(0, 1) : rank),
      "│ " + suit + " │",
      String.format("│ %2s│", rank.length() > 1 ? rank.substring(0, 1) : rank),
      "└───┘"
    };
  }

  private static String[] generateMediumCard(String rank, char suit) {
    // 7x9 card for MEDIUM tier (existing logic)
    return new String[] {
      "┌─────┐",
      String.format("%-3s   │", rank),
      "│     │",
      String.format("│  %c  │", suit),
      "│     │",
      String.format("│   %3s", rank),
      "└─────┘"
    };
  }

  private static String[] generateLargeCard(String rank, char suit) {
    // 10x13 card for LARGE tier
    return new String[] {
      "┌────────┐",
      String.format("│%-3s     │", rank),
      "│        │",
      String.format("│   %c    │", suit),
      "│        │",
      "│        │",
      String.format("│    %c   │", suit),
      "│        │",
      String.format("│     %3s│", rank),
      "└────────┘"
    };
  }

  private static String[] generateSmallBack() {
    return new String[] {
      "┌───┐",
      "│░░░│",
      "│░░░│",
      "│░░░│",
      "└───┘"
    };
  }

  private static String[] generateLargeBack() {
    return new String[] {
      "┌────────┐",
      "│░░░░░░░░│",
      "│░░░░░░░░│",
      "│░░░░░░░░│",
      "│░░░░░░░░│",
      "│░░░░░░░░│",
      "│░░░░░░░░│",
      "│░░░░░░░░│",
      "│░░░░░░░░│",
      "└────────┘"
    };
  }
}
