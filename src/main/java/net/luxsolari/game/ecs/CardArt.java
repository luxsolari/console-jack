package net.luxsolari.game.ecs;

/** ASCII art templates and constants for card rendering. */
public final class CardArt {

  public static final int CARD_ROWS = 7;
  public static final int CARD_COLS = 7;

  private static final String[] ACE_SPADES_FACE = {
    "┌─────┐", "A     │", "♠     │", "│  ♠  │", "│     ♠", "│     A", "└─────┘"
  };

  private static final String[] DEFAULT_BACK = {
    "┌─────┐", "│░░░░░│", "│░░░░░│", "│░░░░░│", "│░░░░░│", "│░░░░░│", "└─────┘"
  };

  public static String[] aceOfSpadesFace() {
    return ACE_SPADES_FACE;
  }

  public static String[] defaultBack() {
    return DEFAULT_BACK;
  }

  private CardArt() {}
}
