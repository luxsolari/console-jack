package net.luxsolari.engine.records;

/**
 * Represents a Z-ordered layer in the rendering system. Each layer has a name and an index that
 * determines its rendering order.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public record ZLayer(String name, int index) {
  // No additional methods needed at this time
}
