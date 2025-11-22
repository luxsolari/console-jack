package net.luxsolari.engine.ui;

import com.googlecode.lanterna.TextColor;
import java.util.function.Consumer;

/**
 * Static factory for creating common UI modification commands.
 *
 * <p>This class provides convenient methods for creating {@link UICommand} instances
 * that perform typical UI modifications. Commands created by this factory can be
 * enqueued to {@link UICommandQueue} for thread-safe execution on the Master thread.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Update label text from any thread
 * UICommandQueue.INSTANCE.enqueue(
 *     UIUpdateCommands.setText(scoreLabel, "Score: 100")
 * );
 *
 * // Add menu item dynamically
 * UICommandQueue.INSTANCE.enqueue(
 *     UIUpdateCommands.addMenuItem(pauseMenu, "Resume", () -> unpause())
 * );
 *
 * // Batch multiple updates atomically
 * UICommandQueue.INSTANCE.enqueueBatch(List.of(
 *     UIUpdateCommands.setText(label1, "Line 1"),
 *     UIUpdateCommands.setText(label2, "Line 2"),
 *     UIUpdateCommands.setVisible(label3, false)
 * ));
 * }</pre>
 *
 * @see UICommand
 * @see UICommandQueue
 */
public final class UIUpdateCommands {
    private UIUpdateCommands() {
        // Prevent instantiation
    }

    // ========== Label Operations ==========

    /**
     * Creates a command to set label text.
     *
     * @param label the label to modify
     * @param text the new text
     * @return a command that sets the label text
     */
    public static UICommand setText(Label label, String text) {
        return () -> label.setText(text);
    }

    /**
     * Creates a command to set label foreground color.
     *
     * @param label the label to modify
     * @param color the new foreground color
     * @return a command that sets the foreground color
     */
    public static UICommand setForegroundColor(Label label, TextColor color) {
        return () -> label.setForegroundColor(color);
    }

    /**
     * Creates a command to set label background color.
     *
     * @param label the label to modify
     * @param color the new background color
     * @return a command that sets the background color
     */
    public static UICommand setBackgroundColor(Label label, TextColor color) {
        return () -> label.setBackgroundColor(color);
    }

    // ========== Component Operations ==========

    /**
     * Creates a command to set component position.
     *
     * @param component the component to move
     * @param x the new x coordinate
     * @param y the new y coordinate
     * @return a command that sets the component position
     */
    public static UICommand setPosition(UIComponent component, int x, int y) {
        return () -> component.setPosition(x, y);
    }

    /**
     * Creates a command to set component visibility.
     *
     * @param component the component to show/hide
     * @param visible true to show, false to hide
     * @return a command that sets the visibility
     */
    public static UICommand setVisible(UIComponent component, boolean visible) {
        return () -> component.setVisible(visible);
    }

    // ========== Container Operations ==========

    /**
     * Creates a command to add a child to a container.
     *
     * @param container the container to modify
     * @param child the component to add
     * @return a command that adds the child
     */
    public static UICommand addChild(UIContainer container, UIComponent child) {
        return () -> container.addChild(child);
    }

    /**
     * Creates a command to remove a child from a container.
     *
     * @param container the container to modify
     * @param child the component to remove
     * @return a command that removes the child
     */
    public static UICommand removeChild(UIContainer container, UIComponent child) {
        return () -> container.removeChild(child);
    }

    /**
     * Creates a command to remove a child at a specific index.
     *
     * @param container the container to modify
     * @param index the index of the child to remove
     * @return a command that removes the child at the index
     */
    public static UICommand removeChildAt(UIContainer container, int index) {
        return () -> {
            if (index >= 0 && index < container.getChildren().size()) {
                UIComponent child = container.getChildren().get(index);
                container.removeChild(child);
            }
        };
    }

    /**
     * Creates a command to insert a child at a specific index.
     *
     * @param container the container to modify
     * @param index the index at which to insert
     * @param child the component to insert
     * @return a command that inserts the child
     */
    public static UICommand insertChild(UIContainer container, int index, UIComponent child) {
        return () -> container.insertChild(index, child);
    }

    /**
     * Creates a command to clear all children from a container.
     *
     * @param container the container to modify
     * @return a command that clears all children
     */
    public static UICommand clearChildren(UIContainer container) {
        return () -> container.clearChildren();
    }

    // ========== Menu Operations ==========

    /**
     * Creates a command to add a menu item.
     *
     * @param menu the menu to modify
     * @param text the menu item text
     * @param action the action to execute when selected
     * @return a command that adds the menu item
     */
    public static UICommand addMenuItem(Menu menu, String text, MenuAction action) {
        return () -> menu.addItem(text, action);
    }

    /**
     * Creates a command to remove a menu item at a specific index.
     *
     * @param menu the menu to modify
     * @param index the index of the item to remove
     * @return a command that removes the menu item
     */
    public static UICommand removeMenuItem(Menu menu, int index) {
        return () -> menu.removeItem(index);
    }

    /**
     * Creates a command to replace a menu item at a specific index.
     *
     * @param menu the menu to modify
     * @param index the index of the item to replace
     * @param text the new menu item text
     * @param action the new action to execute when selected
     * @return a command that replaces the menu item
     */
    public static UICommand replaceMenuItem(Menu menu, int index, String text, MenuAction action) {
        return () -> menu.replaceItem(index, text, action);
    }

    /**
     * Creates a command to clear all menu items.
     *
     * @param menu the menu to modify
     * @return a command that clears all menu items
     */
    public static UICommand clearMenu(Menu menu) {
        return () -> menu.clearItems();
    }

    // ========== Generic Updater ==========

    /**
     * Creates a command that applies a custom updater function to a component.
     *
     * <p>This is useful for complex updates that don't have a dedicated factory method.
     *
     * <p>Example:
     * <pre>{@code
     * UIUpdateCommands.updateComponent(menu, m -> {
     *     m.clearItems();
     *     m.addItem("New Item 1", action1);
     *     m.addItem("New Item 2", action2);
     * });
     * }</pre>
     *
     * @param <T> the type of component
     * @param component the component to update
     * @param updater the function that performs the update
     * @return a command that applies the updater
     */
    public static <T extends UIComponent> UICommand updateComponent(T component, Consumer<T> updater) {
        return () -> updater.accept(component);
    }
}
