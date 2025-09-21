package net.luxsolari.engine.ui;

import com.googlecode.lanterna.input.KeyStroke;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for UI components that can contain other components.
 * Provides functionality for managing child components and handling focus navigation.
 */
public abstract class UIContainer extends UIWidget implements InputHandler {

  private static final int MAX_RENDER_DEPTH = 50; // Prevent stack overflow
  protected final List<UIComponent> children = new ArrayList<>();
  protected int focusedIndex = -1;
  
  // Cache for focusable children to avoid repeated list creation
  private List<UIComponent> focusableChildrenCache = null;
  private boolean focusableCacheDirty = true;

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
    focusableCacheDirty = true; // Invalidate cache
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
      // Properly validate and adjust focus index
      validateAndAdjustFocusIndex();
      focusableCacheDirty = true; // Invalidate cache
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
    validateAndAdjustFocusIndex();
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
   * Invalidates the focusable children cache.
   * Call this method when a child's focusable status might have changed.
   */
  protected void invalidateFocusableCache() {
    focusableCacheDirty = true;
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
    // Return cached result if available and valid
    if (!focusableCacheDirty && focusableChildrenCache != null) {
      return focusableChildrenCache;
    }
    
    // Generate and cache the result
    focusableChildrenCache = children.stream()
        .filter(child -> child instanceof Focusable focusable && focusable.canFocus())
        .toList();
    focusableCacheDirty = false;
    
    return focusableChildrenCache;
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
    render(layerIdx, 0);
  }

  /**
   * Renders the container with depth tracking to prevent infinite recursion.
   *
   * @param layerIdx the layer index to render on
   * @param depth the current rendering depth
   */
  protected void render(int layerIdx, int depth) {
    if (!visible || depth >= MAX_RENDER_DEPTH) {
      if (depth >= MAX_RENDER_DEPTH) {
        System.err.println("Warning: Maximum render depth exceeded for " + getClass().getSimpleName());
      }
      return;
    }

    doRender(layerIdx);

    // Render all children with incremented depth
    for (UIComponent child : children) {
      if (child instanceof UIContainer container) {
        container.render(layerIdx, depth + 1);
      } else {
        child.render(layerIdx);
      }
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
   * Validates and adjusts the focus index to ensure it's within valid bounds.
   * This prevents index out of bounds exceptions and maintains focus consistency.
   */
  private void validateAndAdjustFocusIndex() {
    if (children.isEmpty()) {
      focusedIndex = -1;
      invalidateFocusableCache();
      return;
    }

    // Clamp focus index to valid range
    if (focusedIndex >= children.size()) {
      focusedIndex = children.size() - 1;
    } else if (focusedIndex < -1) {
      focusedIndex = -1;
    }

    // If focused index points to a non-focusable component, find the next focusable one
    if (focusedIndex >= 0 && focusedIndex < children.size()) {
      UIComponent focused = children.get(focusedIndex);
      if (!(focused instanceof Focusable focusable) || !focusable.canFocus()) {
        // Try to find next focusable component
        boolean foundFocusable = false;
        for (int i = 0; i < children.size(); i++) {
          UIComponent child = children.get(i);
          if (child instanceof Focusable f && f.canFocus()) {
            focusedIndex = i;
            foundFocusable = true;
            break;
          }
        }
        if (!foundFocusable) {
          focusedIndex = -1;
        }
      }
    }
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