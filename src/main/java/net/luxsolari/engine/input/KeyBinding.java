package net.luxsolari.engine.input;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.Objects;

/**
 * Immutable record representing a key binding with modifiers.
 * Used as a map key for resolving keystrokes to commands.
 */
public record KeyBinding(
    KeyType keyType,
    Character character,
    boolean ctrlPressed,
    boolean altPressed,
    boolean shiftPressed) {

  /**
   * Creates a KeyBinding from a Lanterna KeyStroke.
   */
  public static KeyBinding fromKeyStroke(KeyStroke keyStroke) {
    if (keyStroke == null) {
      return null;
    }
    return new KeyBinding(
        keyStroke.getKeyType(),
        keyStroke.getCharacter(),
        keyStroke.isCtrlDown(),
        keyStroke.isAltDown(),
        keyStroke.isShiftDown());
  }

  /**
   * Creates a KeyBinding for a character key without modifiers.
   */
  public static KeyBinding of(char character) {
    return new KeyBinding(KeyType.Character, character, false, false, false);
  }

  /**
   * Creates a KeyBinding for a character key with modifiers.
   */
  public static KeyBinding of(char character, boolean ctrl, boolean alt, boolean shift) {
    return new KeyBinding(KeyType.Character, character, ctrl, alt, shift);
  }

  /**
   * Creates a KeyBinding for a special key (non-character) without modifiers.
   */
  public static KeyBinding of(KeyType keyType) {
    return new KeyBinding(keyType, null, false, false, false);
  }

  /**
   * Creates a KeyBinding for a special key with modifiers.
   */
  public static KeyBinding of(KeyType keyType, boolean ctrl, boolean alt, boolean shift) {
    return new KeyBinding(keyType, null, ctrl, alt, shift);
  }

  /**
   * Canonical constructor - normalizes character keys to uppercase.
   */
  public KeyBinding {
    if (keyType == KeyType.Character && character != null) {
      character = Character.toUpperCase(character);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (ctrlPressed) {
      sb.append("Ctrl+");
    }
    if (altPressed) {
      sb.append("Alt+");
    }
    if (shiftPressed) {
      sb.append("Shift+");
    }
    if (keyType == KeyType.Character && character != null) {
      sb.append(character);
    } else {
      sb.append(keyType);
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    KeyBinding that = (KeyBinding) o;
    return ctrlPressed == that.ctrlPressed
        && altPressed == that.altPressed
        && shiftPressed == that.shiftPressed
        && keyType == that.keyType
        && Objects.equals(character, that.character);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keyType, character, ctrlPressed, altPressed, shiftPressed);
  }
}
