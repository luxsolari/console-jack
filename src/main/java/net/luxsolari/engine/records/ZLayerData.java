package net.luxsolari.engine.records;

import com.googlecode.lanterna.TextCharacter;
import java.util.Map;

/**
 * Represents the data content of a Z-ordered layer. This class stores the text characters and their
 * positions within a layer.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public record ZLayerData(Map<ZLayerPosition, TextCharacter> contents) {
  // Getters
  /**
   * Returns the map of positions to text characters in this layer.
   *
   * @return the map of contents.
   */
  public Map<ZLayerPosition, TextCharacter> contents() {
    return contents;
  }

  /**
   * Retrieves a character at a specific position.
   *
   * @param pos The position to retrieve the character from.
   * @return The character at the given position, or null if none exists.
   */
  public TextCharacter get(ZLayerPosition pos) {
    return contents.get(pos);
  }

  /**
   * Retrieves a character at specific coordinates.
   *
   * @param x The x-coordinate.
   * @param y The y-coordinate.
   * @return The character at the given coordinates, or null if none exists.
   */
  public TextCharacter get(int x, int y) {
    return contents.get(new ZLayerPosition(x, y));
  }

  /**
   * Returns the number of characters in this layer.
   *
   * @return The size of the layer contents.
   */
  public int size() {
    return contents.size();
  }

  /**
   * Checks if this layer is empty.
   *
   * @return true if the layer has no characters, false otherwise.
   */
  public boolean isEmpty() {
    return contents.isEmpty();
  }
}
