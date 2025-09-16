package net.luxsolari.game.ecs;

import net.luxsolari.engine.ecs.Component;

/**
 * Holds a multi-cell sprite for a playing card. Stores both face-up and back art. Rows and columns
 * must be equal length as enforced in the ctor.
 */
public final class CardSprite implements Component {

  private final String[] faceUp;
  private final String[] back;
  private boolean showFaceUp;

  /**
   * Creates a new card sprite with face-up and back representations.
   *
   * @param faceUp the face-up sprite lines
   * @param back the back sprite lines
   * @param faceUpInitial whether to initially show face-up
   */
  public CardSprite(String[] faceUp, String[] back, boolean faceUpInitial) {
    if (faceUp.length != back.length) {
      throw new IllegalArgumentException("Sprites must have same height");
    }
    int w = faceUp[0].length();
    for (String row : faceUp) {
      if (row.length() != w) {
        throw new IllegalArgumentException("Sprite rows must be equal length");
      }
    }
    for (String row : back) {
      if (row.length() != w) {
        throw new IllegalArgumentException("Sprite rows must have same width");
      }
    }
    this.faceUp = faceUp;
    this.back = back;
    this.showFaceUp = faceUpInitial;
  }

  /**
   * Returns the number of rows in the sprite.
   *
   * @return the row count
   */
  public int rows() {
    return faceUp.length;
  }

  /**
   * Returns the number of columns in the sprite.
   *
   * @return the column count
   */
  public int cols() {
    return faceUp[0].length();
  }

  /**
   * Returns the current sprite representation.
   *
   * @return the sprite lines to display
   */
  public String[] current() {
    return showFaceUp ? faceUp : back;
  }

  public boolean isFaceUp() {
    return showFaceUp;
  }

  /**
   * Sets whether to show the face-up representation.
   *
   * @param up true to show face-up, false to show back
   */
  public void faceUp(boolean up) {
    this.showFaceUp = up;
  }
}
