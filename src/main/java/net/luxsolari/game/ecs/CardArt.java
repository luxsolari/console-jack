package net.luxsolari.game.ecs;

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

  public static String[] aceOfSpadesFace() {
    return ACE_SPADES_FACE;
  }

  public static String[] aceOfHeartsFace() {
    return ACE_HEARTS_FACE;
  }

  public static String[] aceOfDiamondsFace() {
    return ACE_DIAMONDS_FACE;
  }

  public static String[] aceOfClubsFace() {
    return ACE_CLUBS_FACE;
  }

  public static String[] jokerFace() {
    return JOKER_FACE;
  }

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
    String rankLabel = card.rank().label();
    char suitSymbol = card.suit().symbol();

    // Basic template for a card face
    return new String[] {
      "┌─────┐",
      String.format("%-3s   │", rankLabel),
      "│     │",
      String.format("│  %c  │", suitSymbol),
      "│     │",
      String.format("│   %3s", rankLabel),
      "└─────┘"
    };
  }
}
