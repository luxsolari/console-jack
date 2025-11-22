package net.luxsolari.engine.ui;

/**
 * Represents a subscription to an observable property that can be cancelled.
 *
 * <p>Subscriptions are returned when binding UI components to properties and
 * should be unsubscribed when the UI component is disposed to prevent memory leaks.
 *
 * <p>Example usage:
 * <pre>{@code
 * Subscription sub = label.bind(property, Label::setText);
 * // Later, when disposing the label:
 * sub.unsubscribe();
 * }</pre>
 *
 * @see UIProperty
 * @see BindableUIComponent
 */
@FunctionalInterface
public interface Subscription {
    /**
     * Cancels this subscription, preventing future notifications.
     * After calling this method, the associated listener will no longer
     * receive property change notifications.
     *
     * <p>This method is idempotent - calling it multiple times has no effect
     * after the first call.
     */
    void unsubscribe();
}
