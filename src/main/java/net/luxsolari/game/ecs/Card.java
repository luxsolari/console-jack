package net.luxsolari.game.ecs;

import net.luxsolari.engine.ecs.Component;

/** Logical identity of a playing card. */
public record Card(Rank rank, Suit suit) implements Component {

  /**
   * Represents the suit of a playing card.
   */
  public enum Suit {
    SPADES('♠'),
    HEARTS('♥'),
    DIAMONDS('♦'),
    CLUBS('♣');

    private final char symbol;

    Suit(char symbol) {
      this.symbol = symbol;
    }

    /**
     * Returns the symbol character for this suit.
     *
     * @return the symbol character
     */
    public char symbol() {
      return symbol;
    }
  }

  /**
   * Represents the rank of a playing card.
   */
  public enum Rank {
    A("A"),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
    SEVEN("7"),
    EIGHT("8"),
    NINE("9"),
    TEN("10"),
    J("J"),
    Q("Q"),
    K("K"),
    JOKER("Joker");

    private final String label;

    Rank(String label) {
      this.label = label;
    }

    /**
     * Returns the label string for this rank.
     *
     * @return the label string
     */
    public String label() {
      return label;
    }
  }
}
