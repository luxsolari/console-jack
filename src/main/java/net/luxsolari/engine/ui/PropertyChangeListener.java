package net.luxsolari.engine.ui;

/**
 * Functional interface for observing property value changes.
 *
 * <p>Listeners are notified when a {@link UIProperty} value changes.
 * The notification is delivered via the {@link UICommandQueue}, ensuring
 * thread-safe execution on the Master thread.
 *
 * <p>Example usage:
 * <pre>{@code
 * UIProperty<Integer> score = new UIProperty<>(0);
 * score.addListener((oldValue, newValue) -> {
 *     System.out.println("Score changed from " + oldValue + " to " + newValue);
 * });
 * }</pre>
 *
 * @param <T> the type of property value
 * @see UIProperty
 */
@FunctionalInterface
public interface PropertyChangeListener<T> {
    /**
     * Called when a property value changes.
     *
     * <p>This method is executed on the Master thread via the UICommandQueue.
     *
     * @param oldValue the previous property value
     * @param newValue the new property value
     */
    void onPropertyChange(T oldValue, T newValue);
}
