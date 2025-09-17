package net.luxsolari.engine.ui;

import com.googlecode.lanterna.input.KeyStroke;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for UI components that can contain other components.
 * Provides functionality for managing child components and handling focus navigation.
 */
public abstract class UIContainer extends UIWidget implements InputHandler {

  protected final List<UIComponent> children = new ArrayList<>();
  protected int focusedIndex = -1;

  /**
   * Creates a container with the specified bounds.
   *
   * @param bounds the initial bounds of the container
   */
  protected UIContainer(UIBounds bounds) {
    super(bounds);
  }

  /**
   * Creates a container at the specified position with given dimensions.
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @param width the width
   * @param height the height
   */
  protected UIContainer(int x, int y, int width, int height) {
    super(x, y, width, height);
  }

  /**
   * Adds a child component to this container.
   *
   * @param child the component to add
   * @return this container for method chaining
   */
  public UIContainer addChild(UIComponent child) {
    children.add(child);
    return this;
  }

  /**
   * Removes a child component from this container.
   *
   * @param child the component to remove
   * @return true if the component was removed, false if not found
   */
  public boolean removeChild(UIComponent child) {
    int index = children.indexOf(child);
    if (index >= 0) {
      children.remove(index);
      if (focusedIndex >= index && focusedIndex > 0) {
        focusedIndex--;
      } else if (focusedIndex >= children.size()) {
        focusedIndex = children.size() - 1;
      }
      return true;
    }
    return false;
  }

  /**
   * Gets all child components.
   *
   * @return a copy of the children list
   */
  public List<UIComponent> getChildren() {
    return new ArrayList<>(children);
  }

  /**
   * Gets the currently focused child component.
   *
   * @return the focused component, or null if no component is focused
   */
  public UIComponent getFocusedChild() {
    if (focusedIndex >= 0 && focusedIndex < children.size()) {
      return children.get(focusedIndex);
    }
    return null;
  }

  /**
   * Moves focus to the next focusable child component.
   *
   * @return true if focus was moved, false if no next focusable component exists
   */
  protected boolean focusNext() {
    List<UIComponent> focusableChildren = getFocusableChildren();
    if (focusableChildren.isEmpty()) {
      return false;
    }

    int currentFocusableIndex = getCurrentFocusableIndex(focusableChildren);
    int nextIndex = (currentFocusableIndex + 1) % focusableChildren.size();

    return setFocusToChild(focusableChildren.get(nextIndex));
  }

  /**
   * Moves focus to the previous focusable child component.
   *
   * @return true if focus was moved, false if no previous focusable component exists
   */
  protected boolean focusPrevious() {
    List<UIComponent> focusableChildren = getFocusableChildren();
    if (focusableChildren.isEmpty()) {
      return false;
    }

    int currentFocusableIndex = getCurrentFocusableIndex(focusableChildren);
    int prevIndex = (currentFocusableIndex - 1 + focusableChildren.size()) % focusableChildren.size();

    return setFocusToChild(focusableChildren.get(prevIndex));
  }

  /**
   * Sets focus to a specific child component.
   *
   * @param child the component to focus
   * @return true if focus was set, false if the component is not a child or not focusable
   */
  protected boolean setFocusToChild(UIComponent child) {
    int index = children.indexOf(child);
    if (index < 0) {
      return false;
    }

    // Unfocus current focused component
    if (focusedIndex >= 0 && focusedIndex < children.size()) {
      UIComponent currentFocused = children.get(focusedIndex);
      if (currentFocused instanceof Focusable focusable) {
        focusable.unfocus();
      }
    }

    // Focus new component
    focusedIndex = index;
    if (child instanceof Focusable focusable && focusable.canFocus()) {
      focusable.focus();
      return true;
    }

    return false;
  }

  private List<UIComponent> getFocusableChildren() {
    return children.stream()
        .filter(child -> child instanceof Focusable focusable && focusable.canFocus())
        .toList();
  }

  private int getCurrentFocusableIndex(List<UIComponent> focusableChildren) {
    if (focusedIndex >= 0 && focusedIndex < children.size()) {
      UIComponent currentFocused = children.get(focusedIndex);
      return focusableChildren.indexOf(currentFocused);
    }
    return -1;
  }

  @Override
  public void render(int layerIdx) {
    if (!visible) {
      return;
    }

    doRender(layerIdx);

    // Render all children
    for (UIComponent child : children) {
      child.render(layerIdx);
    }
  }

  @Override
  public boolean handleInput(KeyStroke keyStroke) {
    // First try to delegate to focused child if it's an input handler
    UIComponent focused = getFocusedChild();
    if (focused instanceof InputHandler handler) {
      if (handler.handleInput(keyStroke)) {
        return true;
      }
    }

    // If child didn't handle it, try container-specific input handling
    return handleContainerInput(keyStroke);
  }

  /**
   * Handles input specific to this container.
   * Subclasses should override this to implement their own input handling logic.
   *
   * @param keyStroke the keyboard input to handle
   * @return true if the input was consumed, false otherwise
   */
  protected abstract boolean handleContainerInput(KeyStroke keyStroke);
}