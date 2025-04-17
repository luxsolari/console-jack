package net.luxsolari.engine.utils;

import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import java.util.Map;
import net.luxsolari.engine.systems.ZLayer;
import net.luxsolari.engine.systems.ZLayerData;
import net.luxsolari.engine.systems.ZLayerPosition;

/**
 * Helper class for rendering playing cards on the terminal display.
 * This is demo code for testing the rendering system and will be removed in future versions.
 */
public class CardRenderer {
  // Map of layers for rendering
  private final Map<ZLayer, ZLayerData> layers;

  /**
   * Creates a new CardRenderer with access to the rendering layers.
   *
   * @param layers The rendering layers map from RenderSubsystem
   */
  public CardRenderer(Map<ZLayer, ZLayerData> layers) {
    this.layers = layers;
  }

  /**
   * Draws a playing card to a specific layer at the specified position.
   *
   * @param layerIndex The index of the layer to draw to
   * @param x The x-coordinate of the top-left corner of the card
   * @param y The y-coordinate of the top-left corner of the card
   * @param width The width of the card
   * @param height The height of the card
   * @param cardValueIndex The index of the card value (0-13 for A,2-10,J, Q, K, JOKER)
   * @param suitIndex The index of the card suit (0-3 for hearts, diamonds, clubs,spades)
   */
  public void drawRandomCard(
      int layerIndex, int x, int y, int width, int height, int cardValueIndex, int suitIndex) {

    // random value for the card (A, 2, 3, 4, 5, 6, 7, 8, 9, 10, J, Q, K, JOKER)
    String[] cardValues = {
      "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "JOKER"
    };
    final String cardValue = cardValues[cardValueIndex];

    // random suit for the card (hearts, diamonds, clubs, spades), represented by wide Unicode
    // characters
    String[] suits = {"♥", "♦", "♣", "♠"};
    String[] suitNames = {"HEARTS", "DIAMONDS", "CLUBS", "SPADES"};
    final String suit = suits[suitIndex];

    // draw card frame using special border characters and color the card red if it's a heart or
    // diamond, white otherwise
    final TextColor white = TextColor.ANSI.WHITE;
    final TextColor black = TextColor.ANSI.BLACK;
    final TextColor red = TextColor.ANSI.RED;
    final TextColor green = TextColor.ANSI.GREEN;
    final TextColor yellow = TextColor.ANSI.YELLOW;
    final TextColor magenta = TextColor.ANSI.MAGENTA;
    final TextColor suitColor = (suitIndex < 2) ? red : black;
    final TextColor borderColor =
        (cardValueIndex == 0 || cardValueIndex == 13) ? yellow : suitColor;

    // Get the layer to draw to
    ZLayer zlayer = new ZLayer("Layer %d".formatted(layerIndex), layerIndex);
    ZLayerData layer = layers.get(zlayer);
    Map<ZLayerPosition, TextCharacter> layerContents = layer.contents();

    // I still fail to comprehend why the author of the lanterna library decided to use a 2D array
    // for the TextCharacter class...
    final TextCharacter topLeftCorner = TextCharacter.fromCharacter('┌', borderColor, white)[0];
    final TextCharacter topRightCorner = TextCharacter.fromCharacter('┐', borderColor, white)[0];
    final TextCharacter bottomLeftCorner = TextCharacter.fromCharacter('└', borderColor, white)[0];
    final TextCharacter bottomRightCorner = TextCharacter.fromCharacter('┘', borderColor, white)[0];
    final TextCharacter horizontalBorder = TextCharacter.fromCharacter('─', borderColor, white)[0];
    final TextCharacter verticalBorder = TextCharacter.fromCharacter('│', borderColor, white)[0];
    final TextCharacter blank = TextCharacter.fromCharacter(' ', white, white)[0];

    // Draw to layer instead of screen
    // draw the top border of the card at a specified position (x, y) using special border characters.
    layerContents.put(new ZLayerPosition(x, y), topLeftCorner);
    layerContents.put(new ZLayerPosition(x + width, y), topRightCorner);
    for (int i = 1; i < width; i++) {
      layerContents.put(new ZLayerPosition(x + i, y), horizontalBorder);
    }

    // draw the bottom border of the card at a specified position (x, y) using special border characters.
    layerContents.put(new ZLayerPosition(x, y + height), bottomLeftCorner);
    layerContents.put(new ZLayerPosition(x + width, y + height), bottomRightCorner);
    for (int i = 1; i < width; i++) {
      layerContents.put(new ZLayerPosition(x + i, y + height), horizontalBorder);
    }

    // draw the left border of the card at a specified position (x, y) using special border characters.
    for (int i = 1; i < height; i++) {
      layerContents.put(new ZLayerPosition(x, y + i), verticalBorder);
    }

    // draw the right border of the card at a specified position (x, y) using special border characters.
    for (int i = 1; i < height; i++) {
      layerContents.put(new ZLayerPosition(x + width, y + i), verticalBorder);
    }

    // clear the inside of the card
    for (int i = 1; i < width; i++) {
      for (int j = 1; j < height; j++) {
        layerContents.put(new ZLayerPosition(x + i, y + j), blank);
      }
    }

    // For single characters like card values and suits, use the first character
    char cardValueChar = cardValue.charAt(0);
    char suitChar = suit.charAt(0);

    // draw card value and suit in the center of the card
    // Special handling for "10" in the top left
    if (!cardValue.equals("10")) {
      layerContents.put(
          new ZLayerPosition(x + 1, y + 1),
          TextCharacter.fromCharacter(cardValueChar, suitColor, white)[0]);
      layerContents.put(
          new ZLayerPosition(x + 1, y + 2),
          TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
    } else {
      // Handle "10" especially for the top left
      layerContents.put(
          new ZLayerPosition(x + 1, y + 1), TextCharacter.fromCharacter('1', suitColor, white)[0]);
      layerContents.put(
          new ZLayerPosition(x + 2, y + 1), TextCharacter.fromCharacter('0', suitColor, white)[0]);
      layerContents.put(
          new ZLayerPosition(x + 1, y + 2),
          TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
    }

    // Special handling for "10" which is two characters
    if (!cardValue.equals("10")) {
      layerContents.put(
          new ZLayerPosition(x + width - 1, y + height - 2),
          TextCharacter.fromCharacter(cardValueChar, suitColor, white)[0]);
      layerContents.put(
          new ZLayerPosition(x + width - 1, y + height - 1),
          TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
    } else {
      // Handle "10" especially since it's two characters
      layerContents.put(
          new ZLayerPosition(x + width - 1, y + height - 2),
          TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      layerContents.put(
          new ZLayerPosition(x + width - 2, y + height - 1),
          TextCharacter.fromCharacter('1', suitColor, white)[0]);
      layerContents.put(
          new ZLayerPosition(x + width - 1, y + height - 1),
          TextCharacter.fromCharacter('0', suitColor, white)[0]);
    }

    // draw card corners
    layerContents.put(
        new ZLayerPosition(x + 2, y + 2), TextCharacter.fromCharacter('┌', suitColor, white)[0]);
    layerContents.put(
        new ZLayerPosition(x + 8, y + 2), TextCharacter.fromCharacter('┐', suitColor, white)[0]);
    layerContents.put(
        new ZLayerPosition(x + 2, y + 6), TextCharacter.fromCharacter('└', suitColor, white)[0]);
    layerContents.put(
        new ZLayerPosition(x + 8, y + 6), TextCharacter.fromCharacter('┘', suitColor, white)[0]);

    // Add the suit symbols for each card value
    switch (cardValue) {
      case "A" -> {
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "2" -> {
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "3" -> {
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "4" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "5" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "6" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "7" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "8" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 2),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 6),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "9" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "10" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 2),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 6),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
      case "J" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 6),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 2),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
      }
      case "Q" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 4, y + 2),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 6, y + 2),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 6),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
      }
      case "K" -> {
        layerContents.put(
            new ZLayerPosition(x + 3, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 4, y + 2),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 6, y + 2),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 1),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 6),
            TextCharacter.fromCharacter(suitChar, TextColor.ANSI.YELLOW, white)[0]);
      }
      case "JOKER" -> {
        // Special rendering for Joker card with a colorful pattern
        // Draw a jester hat pattern
        layerContents.put(
            new ZLayerPosition(x + 3, y + 2), TextCharacter.fromCharacter('▲', magenta, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 4, y + 2), TextCharacter.fromCharacter('▲', red, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 2), TextCharacter.fromCharacter('▲', yellow, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 6, y + 2), TextCharacter.fromCharacter('▲', green, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 2), TextCharacter.fromCharacter('▲', magenta, white)[0]);

        // Draw a joker face
        layerContents.put(
            new ZLayerPosition(x + 4, y + 3), TextCharacter.fromCharacter('(', white, black)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 3), TextCharacter.fromCharacter('^', yellow, black)[0]);
        layerContents.put(
            new ZLayerPosition(x + 6, y + 3), TextCharacter.fromCharacter(')', white, black)[0]);

        // Draw a smile
        layerContents.put(
            new ZLayerPosition(x + 4, y + 4), TextCharacter.fromCharacter('\\', white, black)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4), TextCharacter.fromCharacter('_', white, black)[0]);
        layerContents.put(
            new ZLayerPosition(x + 6, y + 4), TextCharacter.fromCharacter('/', white, black)[0]);

        // Draw joker text
        layerContents.put(
            new ZLayerPosition(x + 3, y + 5), TextCharacter.fromCharacter('J', magenta, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 4, y + 5), TextCharacter.fromCharacter('O', red, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 5, y + 5), TextCharacter.fromCharacter('K', yellow, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 6, y + 5), TextCharacter.fromCharacter('E', green, white)[0]);
        layerContents.put(
            new ZLayerPosition(x + 7, y + 5), TextCharacter.fromCharacter('R', magenta, white)[0]);
      }
      default -> {
        // Handle any unexpected card values by drawing a simple pattern
        layerContents.put(
            new ZLayerPosition(x + 5, y + 4),
            TextCharacter.fromCharacter(suitChar, suitColor, white)[0]);
      }
    }
  }
}
