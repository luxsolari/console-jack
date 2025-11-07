package net.luxsolari.engine.manager;

import java.util.logging.Logger;
import net.luxsolari.engine.viewport.Anchor;

/**
 * Viewport manager implemented as a singleton to handle coordinate transformation from relative
 * positions (0.0-1.0) to absolute screen coordinates.
 */
public final class ViewportManager {
  public static final ViewportManager INSTANCE = new ViewportManager();

  private static final String TAG = ViewportManager.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  // Reference resolution (1280x720px window with 20pt font)
  private static final int REF_WIDTH = 98;
  private static final int REF_HEIGHT = 30;

  // Minimum supported terminal size
  private static final int MIN_WIDTH = REF_WIDTH;
  private static final int MIN_HEIGHT = REF_HEIGHT;

  private volatile int currentWidth = REF_WIDTH;
  private volatile int currentHeight = REF_HEIGHT;
  private volatile boolean meetsMinimum = true;
  private volatile boolean sizeChanged = false;

  /**
   * Updates the current viewport size. Should be called when terminal is resized.
   *
   * @param width new terminal width in columns
   * @param height new terminal height in rows
   */
  public void updateSize(int width, int height) {
    if (this.currentWidth != width || this.currentHeight != height) {
      this.currentWidth = width;
      this.currentHeight = height;
      this.meetsMinimum = (width >= MIN_WIDTH && height >= MIN_HEIGHT);
      this.sizeChanged = true;

      LOGGER.info(
          "[%s] Viewport updated: %dx%d (meets minimum: %s)"
              .formatted(TAG, width, height, meetsMinimum));
    }
  }

  /**
   * Converts relative X coordinate to absolute screen coordinate.
   *
   * @param relX relative X coordinate (0.0-1.0)
   * @param anchor positioning anchor for offset calculation
   * @return absolute screen X coordinate
   */
  public int toScreenX(float relX, Anchor anchor) {
    int baseX = Math.round(relX * currentWidth);
    // Apply anchor offset - anchor provides the offset factor for centering/alignment
    return Math.max(0, Math.min(currentWidth - 1, baseX));
  }

  /**
   * Converts relative Y coordinate to absolute screen coordinate.
   *
   * @param relY relative Y coordinate (0.0-1.0)
   * @param anchor positioning anchor for offset calculation
   * @return absolute screen Y coordinate
   */
  public int toScreenY(float relY, Anchor anchor) {
    int baseY = Math.round(relY * currentHeight);
    // Apply anchor offset - anchor provides the offset factor for centering/alignment
    return Math.max(0, Math.min(currentHeight - 1, baseY));
  }

  /**
   * Converts relative X coordinate to absolute screen coordinate with element width consideration.
   *
   * @param relX relative X coordinate (0.0-1.0)
   * @param anchor positioning anchor
   * @param elementWidth width of the element being positioned
   * @return absolute screen X coordinate adjusted for anchor
   */
  public int toScreenX(float relX, Anchor anchor, int elementWidth) {
    int baseX = Math.round(relX * currentWidth);
    int offsetX = Math.round(anchor.xOffset * elementWidth);
    return Math.max(0, Math.min(currentWidth - elementWidth, baseX - offsetX));
  }

  /**
   * Converts relative Y coordinate to absolute screen coordinate with element height consideration.
   *
   * @param relY relative Y coordinate (0.0-1.0)
   * @param anchor positioning anchor
   * @param elementHeight height of the element being positioned
   * @return absolute screen Y coordinate adjusted for anchor
   */
  public int toScreenY(float relY, Anchor anchor, int elementHeight) {
    int baseY = Math.round(relY * currentHeight);
    int offsetY = Math.round(anchor.yOffset * elementHeight);
    return Math.max(0, Math.min(currentHeight - elementHeight, baseY - offsetY));
  }

  /**
   * Checks if the current terminal size meets the minimum requirements.
   *
   * @return true if terminal size is adequate for game display
   */
  public boolean meetsMinimumSize() {
    return meetsMinimum;
  }

  /**
   * Gets the current viewport width in columns.
   *
   * @return current width
   */
  public int getWidth() {
    return currentWidth;
  }

  /**
   * Gets the current viewport height in rows.
   *
   * @return current height
   */
  public int getHeight() {
    return currentHeight;
  }

  /**
   * Gets the current scale factor relative to reference resolution.
   *
   * @return scale factor
   */
  public float getScale() {
    float scaleX = (float) currentWidth / REF_WIDTH;
    float scaleY = (float) currentHeight / REF_HEIGHT;
    return Math.min(scaleX, scaleY);
  }

  /**
   * Checks if the viewport size has changed since last check and clears the flag.
   *
   * @return true if size changed since last call
   */
  public boolean consumeSizeChanged() {
    boolean changed = sizeChanged;
    sizeChanged = false;
    return changed;
  }

  /**
   * Gets minimum required dimensions.
   *
   * @return array with [minWidth, minHeight]
   */
  public int[] getMinimumSize() {
    return new int[] {MIN_WIDTH, MIN_HEIGHT};
  }
}
