package net.luxsolari.engine.records;

import com.googlecode.lanterna.TextCharacter;
import java.util.Map;

/**
 * Represents the data content of a Z-ordered layer. This class stores the text characters and their
 * positions within a layer.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public record ZLayerData(Map<ZLayerPosition, TextCharacter> contents) {
  // No additional methods needed at this time
}
