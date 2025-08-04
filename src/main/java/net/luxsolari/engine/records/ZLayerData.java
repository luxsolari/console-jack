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
  public Map<ZLayerPosition, TextCharacter> contents() {
    return contents;
  }

  public TextCharacter get(ZLayerPosition pos) {
    return contents.get(pos);
  }

  public TextCharacter get(int x, int y) {
    return contents.get(new ZLayerPosition(x, y));
  }

  public int size() {
    return contents.size();
  }

  public boolean isEmpty() {
    return contents.isEmpty();
  }
}
