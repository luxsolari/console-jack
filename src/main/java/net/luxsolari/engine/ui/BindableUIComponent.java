package net.luxsolari.engine.ui;

/**
 * Interface for UI components that support reactive property binding.
 *
 * <p>Components implementing this interface can bind to {@link UIProperty} instances,
 * automatically updating themselves when property values change. This enables a reactive,
 * declarative style of UI programming.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create observable property
 * UIProperty<String> statusText = new UIProperty<>("Ready");
 *
 * // Bind label to property
 * Label statusLabel = new Label(10, 10, "");
 * Subscription sub = statusLabel.bind(statusText, Label::setText);
 *
 * // Update from any thread - label updates automatically
 * statusText.setValue("Processing...");
 *
 * // Clean up when done
 * sub.unsubscribe();
 * // OR
 * statusLabel.unbindAll();
 * }</pre>
 *
 * <h2>Implementation Requirements</h2>
 * Implementations should:
 * <ul>
 *   <li>Apply the initial property value immediately via {@link UICommandQueue}</li>
 *   <li>Subscribe to property changes and update the component accordingly</li>
 *   <li>Track subscriptions for cleanup in {@link #unbindAll()}</li>
 *   <li>Support multiple concurrent bindings to different properties</li>
 * </ul>
 *
 * <h2>Memory Management</h2>
 * Always call {@link #unbindAll()} when disposing a component to prevent memory leaks.
 * Subscriptions maintain strong references to listeners, so failing to unsubscribe
 * will prevent the component from being garbage collected.
 *
 * @see UIProperty
 * @see PropertyBinder
 * @see Subscription
 * @see UIWidget
 */
public interface BindableUIComponent extends UIComponent {
    /**
     * Binds this component to a property using a custom binder function.
     *
     * <p>The binder function is called:
     * <ul>
     *   <li>Immediately with the current property value (via UICommandQueue)</li>
     *   <li>Whenever the property value changes in the future</li>
     * </ul>
     *
     * <p>The binding is executed on the Master thread via {@link UICommandQueue},
     * ensuring thread-safe UI updates.
     *
     * @param <T> the type of property value
     * @param property the observable property to bind to
     * @param binder the function that applies property values to this component
     * @return a subscription that can be used to cancel the binding
     * @throws NullPointerException if property or binder is null
     */
    <T> Subscription bind(UIProperty<T> property, PropertyBinder<? super UIComponent, T> binder);

    /**
     * Removes all property bindings for this component.
     *
     * <p>This method should be called when disposing a component to prevent memory leaks.
     * After calling this method, the component will no longer receive property change
     * notifications from any previously bound properties.
     *
     * <p>This method is idempotent - calling it multiple times is safe.
     */
    void unbindAll();
}
