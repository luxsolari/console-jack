package net.luxsolari.game.entity;

import net.luxsolari.engine.systems.RenderSubsystem;

/**
 * Represents a playing card in the game. Extends the base Entity class with card-specific
 * properties and behaviors.
 */
public class Card extends Entity {
  /** Enum representing the four suits of a standard playing card deck. */
  public enum Suit {
    HEARTS,
    DIAMONDS,
    CLUBS,
    SPADES
  }

  /** Enum representing the thirteen ranks of a standard playing card deck. */
  public enum Rank {
    ACE,
    TWO,
    THREE,
    FOUR,
    FIVE,
    SIX,
    SEVEN,
    EIGHT,
    NINE,
    TEN,
    JACK,
    QUEEN,
    KING
  }

  private final Suit suit;
  private final Rank rank;
  private boolean faceUp;
  private static final int CARD_LAYER = 1; // Default layer for cards

  /**
   * Creates a new Card with the specified properties.
   *
   * @param x The x-coordinate of the card
   * @param y The y-coordinate of the card
   * @param width The width of the card
   * @param height The height of the card
   * @param suit The suit of the card
   * @param rank The rank of the card
   */
  public Card(float x, float y, float width, float height, Suit suit, Rank rank) {
    super(x, y, width, height);
    this.suit = suit;
    this.rank = rank;
    this.faceUp = false;
  }

  /**
   * Creates a new Card with the specified properties.
   *
   * @param position The position of the card
   * @param width The width of the card
   * @param height The height of the card
   * @param suit The suit of the card
   * @param rank The rank of the card
   */
  public Card(EntityPosition position, float width, float height, Suit suit, Rank rank) {
    super(position, width, height);
    this.suit = suit;
    this.rank = rank;
    this.faceUp = false;
  }

  @Override
  public void update(float deltaTime) {
    // Card-specific update logic can be added here
  }

  @Override
  public void render() {
    // Use the RenderSubsystem to render this card
    RenderSubsystem.getInstance().renderCard(CARD_LAYER, this);
  }

  /** Flips the card face up or face down. */
  public void flip() {
    faceUp = !faceUp;
  }

  /**
   * Returns whether the card is face up.
   *
   * @return true if the card is face up, false otherwise
   */
  public boolean isFaceUp() {
    return faceUp;
  }

  /**
   * Returns the suit of the card.
   *
   * @return the card's suit
   */
  public Suit getSuit() {
    return suit;
  }

  /**
   * Returns the rank of the card.
   *
   * @return the card's rank
   */
  public Rank getRank() {
    return rank;
  }
}
