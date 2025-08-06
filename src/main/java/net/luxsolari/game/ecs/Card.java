package net.luxsolari.game.ecs;

import net.luxsolari.engine.ecs.Component;

/** Logical identity of a playing card. */
public record Card(Rank rank, Suit suit) implements Component {

  public enum Suit {
    SPADES('♠'),
    HEARTS('♥'),
    DIAMONDS('♦'),
    CLUBS('♣');

    private final char symbol;

    Suit(char symbol) {
      this.symbol = symbol;
    }

    public char symbol() {
      return symbol;
    }
  }

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
    K("K");

    private final String label;

    Rank(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }
}
