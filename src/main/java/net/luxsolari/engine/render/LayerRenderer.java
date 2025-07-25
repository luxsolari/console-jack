package net.luxsolari.engine.render;

import com.googlecode.lanterna.Symbols;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import java.util.concurrent.ConcurrentHashMap;
import net.luxsolari.engine.systems.RenderSubsystem;
import net.luxsolari.engine.records.ZLayer;
import net.luxsolari.engine.records.ZLayerData;
import net.luxsolari.engine.records.ZLayerPosition;

/**
 * Convenience facade for pushing glyphs into {@link RenderSubsystem} layers. All methods are static
 * & stateless; nothing to construct. The class also offers a few niceties such as rainbow/gradient
 * strings and box drawing helpers so that individual game states remain free of boiler-plate
 * rendering code.
 */
public final class LayerRenderer {

  // ---------- configuration / theme ----------
  public static final TextColor DEFAULT_FG = TextColor.ANSI.WHITE;
  public static final TextColor DEFAULT_BG = new TextColor.RGB(40, 55, 40);

  private static final TextColor[] RAINBOW = {
      TextColor.ANSI.RED,
      TextColor.ANSI.YELLOW,
      TextColor.ANSI.GREEN,
      TextColor.ANSI.CYAN,
      TextColor.ANSI.BLUE,
      TextColor.ANSI.MAGENTA
  };

  private LayerRenderer() {}

  /* ======================================================================== */
  /*                               LAYER HELPERS                              */
  /* ======================================================================== */

  /** Clears every glyph from the chosen layer. */
  public static void clear(int layerIdx) {
    ZLayerData layer = getLayer(layerIdx);
    if (layer != null) {
      layer.contents().clear();
    }
  }

  /* ======================================================================== */
  /*                              GLYPH HELPERS                               */
  /* ======================================================================== */

  /** Quick helper for monocolor strings (defaults to {@link #DEFAULT_FG}/{@link #DEFAULT_BG}). */
  public static void putString(int layerIdx, int x, int y, String text) {
    putString(layerIdx, x, y, text, DEFAULT_FG, DEFAULT_BG);
  }

  /** Puts a monocolor string. */
  public static void putString(
      int layerIdx, int x, int y, String text, TextColor fg, TextColor bg) {
    ZLayerData layer = getLayer(layerIdx);
    if (layer == null) return;
    for (int i = 0; i < text.length(); i++) {
      putChar(layerIdx, x + i, y, text.charAt(i), fg, bg);
    }
  }

  /** Shortcut: specify only foreground, keep default background. */
  public static void putStringCustomFg(
      int layerIdx, int x, int y, String text, TextColor fg) {
    putString(layerIdx, x, y, text, fg, DEFAULT_BG);
  }

  /** Shortcut: use completely default colours. */
  public static void putStringDefault(int layerIdx, int x, int y, String text) {
    putString(layerIdx, x, y, text, DEFAULT_FG, DEFAULT_BG);
  }

  /**
   * Rainbow string – cycles through {@link #RAINBOW} colours per character.
   *
   * <p>The background colour defaults to {@link #DEFAULT_BG}.
   */
  public static void putStringRainbow(int layerIdx, int x, int y, String text) {
    ZLayerData layer = getLayer(layerIdx);
    if (layer == null) return;
    for (int i = 0; i < text.length(); i++) {
      TextColor fg = RAINBOW[i % RAINBOW.length];
      putChar(layerIdx, x + i, y, text.charAt(i), fg, DEFAULT_BG);
    }
  }

  /**
   * Linear foreground gradient between two colours.
   *
   * @param from inclusive first colour
   * @param to inclusive last colour
   */
  public static void putStringGradient(
      int layerIdx, int x, int y, String text, TextColor.RGB from, TextColor.RGB to) {

    ZLayerData layer = getLayer(layerIdx);
    if (layer == null) return;
    int len = text.length();
    for (int i = 0; i < len; i++) {
      double t = (double) i / Math.max(len - 1, 1); // [0,1]
      int r = (int) (from.getRed() + t * (to.getRed() - from.getRed()));
      int g = (int) (from.getGreen() + t * (to.getGreen() - from.getGreen()));
      int b = (int) (from.getBlue() + t * (to.getBlue() - from.getBlue()));
      putChar(layerIdx, x + i, y, text.charAt(i), new TextColor.RGB(r, g, b), DEFAULT_BG);
    }
  }

  /** Draws a single glyph (foreground/background fully configurable). */
  public static void putChar(
      int layerIdx, int x, int y, char ch, TextColor fg, TextColor bg) {
    ZLayerData layer = getLayer(layerIdx);
    if (layer == null) return;
    layer.contents().put(new ZLayerPosition(x, y), new TextCharacter(ch, fg, bg));
  }

  /** Shortcut: specify only foreground. */
  public static void putChar(int layerIdx, int x, int y, char ch, TextColor fg) {
    putChar(layerIdx, x, y, ch, fg, DEFAULT_BG);
  }

  /** Shortcut: default colours. */
  public static void putChar(int layerIdx, int x, int y, char ch) {
    putChar(layerIdx, x, y, ch, DEFAULT_FG, DEFAULT_BG);
  }

  /* ======================================================================== */
  /*                               BOX HELPERS                                */
  /* ======================================================================== */

  /**
   * Draws a rectangular box (inclusive coordinates).
   *
   * <p>Uses Lanterna {@link Symbols} single-line box glyphs. Caller must satisfy {@code x1≤x2} &
   * {@code y1≤y2}.
   */
  public static void drawBox(
      int layerIdx, int x1, int y1, int x2, int y2, TextColor fg, TextColor bg) {

    ZLayerData layer = getLayer(layerIdx);
    if (layer == null) return;

    // top & bottom horizontal lines
    for (int x = x1 + 1; x < x2; x++) {
      putChar(layerIdx, x, y1, Symbols.SINGLE_LINE_HORIZONTAL, fg, bg);
      putChar(layerIdx, x, y2, Symbols.SINGLE_LINE_HORIZONTAL, fg, bg);
    }

    // left & right vertical lines
    for (int y = y1 + 1; y < y2; y++) {
      putChar(layerIdx, x1, y, Symbols.SINGLE_LINE_VERTICAL, fg, bg);
      putChar(layerIdx, x2, y, Symbols.SINGLE_LINE_VERTICAL, fg, bg);
    }

    // corners
    putChar(layerIdx, x1, y1, Symbols.SINGLE_LINE_TOP_LEFT_CORNER, fg, bg);
    putChar(layerIdx, x2, y1, Symbols.SINGLE_LINE_TOP_RIGHT_CORNER, fg, bg);
    putChar(layerIdx, x1, y2, Symbols.SINGLE_LINE_BOTTOM_LEFT_CORNER, fg, bg);
    putChar(layerIdx, x2, y2, Symbols.SINGLE_LINE_BOTTOM_RIGHT_CORNER, fg, bg);
  }

  /* ======================================================================== */
  /*                               INTERNALS                                  */
  /* ======================================================================== */

  private static ZLayerData getLayer(int idx) {
    RenderSubsystem rs = RenderSubsystem.getInstance();
    if (rs.getLayers() == null) {
      // RenderSubsystem not yet initialized; caller should safely ignore drawing for now
      return null;
    }
    ZLayer zlayer = new ZLayer("Layer " + idx, idx);
    return rs.getLayers().computeIfAbsent(
        zlayer, l -> new ZLayerData(new ConcurrentHashMap<>()));
  }
}
