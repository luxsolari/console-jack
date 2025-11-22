package net.luxsolari.engine.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for atomic UI components (widgets).
 * Provides common functionality for position, size, and visibility management.
 * Supports reactive property binding via {@link BindableUIComponent}.
 */
public abstract class UIWidget implements BindableUIComponent {

  protected UIBounds bounds;
  protected boolean visible = true;
  private final List<Subscription> subscriptions = new ArrayList<>();

  /**
   * Creates a widget with the specified bounds.
   *
   * @param bounds the initial bounds of the widget
   */
  protected UIWidget(UIBounds bounds) {
    this.bounds = bounds;
  }

  /**
   * Creates a widget at the specified position with given dimensions.
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @param width the width
   * @param height the height
   */
  protected UIWidget(int x, int y, int width, int height) {
    this.bounds = new UIBounds(x, y, width, height);
  }

  @Override
  public UIBounds getBounds() {
    return bounds;
  }

  @Override
  public void setPosition(int x, int y) {
    this.bounds = bounds.withPosition(x, y);
  }

  @Override
  public int getX() {
    return bounds.x();
  }

  @Override
  public int getY() {
    return bounds.y();
  }

  @Override
  public int getWidth() {
    return bounds.width();
  }

  @Override
  public int getHeight() {
    return bounds.height();
  }

  @Override
  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  @Override
  public boolean isVisible() {
    return visible;
  }

  /**
   * Sets the size of this widget.
   *
   * @param width the new width
   * @param height the new height
   */
  protected void setSize(int width, int height) {
    this.bounds = bounds.withSize(width, height);
  }

  /**
   * Abstract method for subclasses to implement their specific rendering logic.
   * This method is called by render() when the component is visible.
   *
   * @param layerIdx the Z-layer index to render on
   */
  protected abstract void doRender(int layerIdx);

  @Override
  public void render(int layerIdx) {
    if (visible) {
      doRender(layerIdx);
    }
  }

  @Override
  public <T> Subscription bind(UIProperty<T> property, PropertyBinder<? super UIComponent, T> binder) {
    if (property == null || binder == null) {
      throw new NullPointerException("Property and binder cannot be null");
    }

    // Apply initial value via command queue
    UICommandQueue.INSTANCE.enqueue(() -> binder.applyValue(this, property.getValue()));

    // Subscribe to future changes
    Subscription sub = property.addListener((oldValue, newValue) -> {
      binder.applyValue(this, newValue);
    });

    subscriptions.add(sub);
    return sub;
  }

  @Override
  public void unbindAll() {
    subscriptions.forEach(Subscription::unsubscribe);
    subscriptions.clear();
  }
}