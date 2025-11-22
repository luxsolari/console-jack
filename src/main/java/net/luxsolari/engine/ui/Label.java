package net.luxsolari.engine.ui;

import com.googlecode.lanterna.TextColor;
import net.luxsolari.engine.manager.RenderManager;

/**
 * A simple text label widget for displaying static text.
 * Supports custom foreground and background colors.
 */
public class Label extends UIWidget {

  private String text;
  private TextColor foregroundColor;
  private TextColor backgroundColor;

  /**
   * Creates a label with the specified text at the given position.
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @param text the text to display
   */
  public Label(int x, int y, String text) {
    super(x, y, text.length(), 1);
    this.text = text;
    this.foregroundColor = RenderManager.DEFAULT_FG;
    this.backgroundColor = RenderManager.DEFAULT_BG;
  }

  /**
   * Creates a label with the specified text, position, and colors.
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @param text the text to display
   * @param foregroundColor the text color
   * @param backgroundColor the background color
   */
  public Label(int x, int y, String text, TextColor foregroundColor, TextColor backgroundColor) {
    super(x, y, text.length(), 1);
    this.text = text;
    this.foregroundColor = foregroundColor;
    this.backgroundColor = backgroundColor;
  }

  /**
   * Gets the text displayed by this label.
   *
   * @return the label text
   */
  public String getText() {
    return text;
  }

  /**
   * Sets the text displayed by this label.
   * Updates the width to match the new text length.
   *
   * @param text the new text to display
   */
  public void setText(String text) {
    this.text = text;
    setSize(text.length(), 1);
  }

  /**
   * Gets the foreground color.
   *
   * @return the foreground color
   */
  public TextColor getForegroundColor() {
    return foregroundColor;
  }

  /**
   * Sets the foreground color.
   *
   * @param foregroundColor the new foreground color
   */
  public void setForegroundColor(TextColor foregroundColor) {
    this.foregroundColor = foregroundColor;
  }

  /**
   * Gets the background color.
   *
   * @return the background color
   */
  public TextColor getBackgroundColor() {
    return backgroundColor;
  }

  /**
   * Sets the background color.
   *
   * @param backgroundColor the new background color
   */
  public void setBackgroundColor(TextColor backgroundColor) {
    this.backgroundColor = backgroundColor;
  }

  /**
   * Binds the label text to an observable property.
   * The label will automatically update when the property value changes.
   *
   * @param property the observable text property
   * @return a subscription that can be used to cancel the binding
   */
  public Subscription bindText(UIProperty<String> property) {
    return bind(property, (component, value) -> ((Label) component).setText(value));
  }

  /**
   * Binds the label foreground color to an observable property.
   * The label will automatically update when the property value changes.
   *
   * @param property the observable color property
   * @return a subscription that can be used to cancel the binding
   */
  public Subscription bindForegroundColor(UIProperty<TextColor> property) {
    return bind(property, (component, value) -> ((Label) component).setForegroundColor(value));
  }

  /**
   * Binds the label background color to an observable property.
   * The label will automatically update when the property value changes.
   *
   * @param property the observable color property
   * @return a subscription that can be used to cancel the binding
   */
  public Subscription bindBackgroundColor(UIProperty<TextColor> property) {
    return bind(property, (component, value) -> ((Label) component).setBackgroundColor(value));
  }

  @Override
  protected void doRender(int layerIdx) {
    if (text != null && !text.isEmpty()) {
      RenderManager.putString(layerIdx, getX(), getY(), text, foregroundColor, backgroundColor);
    }
  }
}