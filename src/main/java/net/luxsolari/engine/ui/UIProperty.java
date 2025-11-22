package net.luxsolari.engine.ui;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observable property container that notifies listeners of value changes.
 *
 * <p>UIProperty provides a reactive data binding mechanism for UI components.
 * When the property value changes, all registered listeners are notified via
 * the {@link UICommandQueue}, ensuring thread-safe updates on the Master thread.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create observable property
 * UIProperty<String> playerName = new UIProperty<>("Player 1");
 * UIProperty<Integer> score = new UIProperty<>(0);
 *
 * // Bind to UI components
 * nameLabel.bind(playerName, Label::setText);
 * scoreLabel.bind(score, (lbl, val) -> lbl.setText("Score: " + val));
 *
 * // Update from any thread - UI automatically updates
 * score.setValue(100);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li>{@link #getValue()} - Thread-safe (volatile read)</li>
 *   <li>{@link #setValue(Object)} - Thread-safe (synchronized)</li>
 *   <li>{@link #addListener(PropertyChangeListener)} - Thread-safe (CopyOnWriteArrayList)</li>
 *   <li>Listener notifications - Delivered via UICommandQueue on Master thread</li>
 * </ul>
 *
 * <h2>Memory Management</h2>
 * Listeners maintain strong references to prevent premature garbage collection.
 * Use {@link Subscription#unsubscribe()} to remove listeners and prevent memory leaks.
 *
 * @param <T> the type of property value
 * @see PropertyChangeListener
 * @see BindableUIComponent
 * @see UICommandQueue
 */
public class UIProperty<T> {
    private volatile T value;
    private final List<PropertyChangeListener<T>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Creates a new property with the specified initial value.
     *
     * @param initialValue the initial value (may be null)
     */
    public UIProperty(T initialValue) {
        this.value = initialValue;
    }

    /**
     * Returns the current property value.
     *
     * <p>This method is thread-safe and can be called from any thread.
     *
     * @return the current value (may be null)
     */
    public T getValue() {
        return value;
    }

    /**
     * Sets a new property value and notifies all listeners.
     *
     * <p>This method is thread-safe and can be called from any thread.
     * Listeners are notified via {@link UICommandQueue} regardless of whether
     * the new value differs from the old value (no equals() check).
     *
     * <p>Listener notifications are enqueued as commands and executed on the
     * Master thread during the next {@link UICommandQueue#processAll()} call.
     *
     * @param newValue the new value (may be null)
     */
    public synchronized void setValue(T newValue) {
        T oldValue = this.value;
        this.value = newValue;
        notifyListeners(oldValue, newValue);
    }

    /**
     * Adds a listener to be notified of property changes.
     *
     * <p>This method is thread-safe and can be called from any thread.
     * The listener will NOT be notified of the current value immediately;
     * it will only receive notifications for future changes.
     *
     * <p>To apply the current value immediately, use:
     * <pre>{@code
     * Subscription sub = property.addListener(listener);
     * UICommandQueue.INSTANCE.enqueue(() -> listener.onPropertyChange(null, property.getValue()));
     * }</pre>
     *
     * Or better yet, use {@link BindableUIComponent#bind(UIProperty, PropertyBinder)}
     * which handles initial value application automatically.
     *
     * @param listener the listener to add
     * @return a subscription that can be used to remove the listener
     * @throws NullPointerException if listener is null
     * @see BindableUIComponent#bind(UIProperty, PropertyBinder)
     */
    public Subscription addListener(PropertyChangeListener<T> listener) {
        if (listener == null) {
            throw new NullPointerException("Listener cannot be null");
        }
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /**
     * Notifies all registered listeners of a property change.
     *
     * <p>Notifications are delivered via {@link UICommandQueue} to ensure
     * thread-safe execution on the Master thread.
     *
     * @param oldValue the previous value
     * @param newValue the new value
     */
    private void notifyListeners(T oldValue, T newValue) {
        for (PropertyChangeListener<T> listener : listeners) {
            UICommand command = () -> listener.onPropertyChange(oldValue, newValue);
            UICommandQueue.INSTANCE.enqueue(command);
        }
    }

    /**
     * Returns the number of registered listeners.
     *
     * <p>This method is useful for debugging and monitoring.
     *
     * @return the listener count
     */
    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * Returns a string representation of this property.
     *
     * @return a string showing the current value and listener count
     */
    @Override
    public String toString() {
        return "UIProperty{value=" + value + ", listeners=" + listeners.size() + "}";
    }

    /**
     * Returns the hash code of the current value.
     *
     * @return the value's hash code, or 0 if value is null
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    /**
     * Compares this property to another object.
     *
     * <p>Two properties are equal if they have the same value (compared using
     * {@link Objects#equals(Object, Object)}). Listeners are not considered
     * in equality comparison.
     *
     * @param obj the object to compare
     * @return true if the objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof UIProperty<?> other)) {
            return false;
        }
        return Objects.equals(this.value, other.value);
    }
}
