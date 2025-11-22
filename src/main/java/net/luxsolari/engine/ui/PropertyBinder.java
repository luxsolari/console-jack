package net.luxsolari.engine.ui;

/**
 * Functional interface for applying property values to UI components.
 *
 * <p>A PropertyBinder defines how a property value should be applied to a component.
 * It is used in conjunction with {@link UIProperty} to create reactive bindings.
 *
 * <p>Example usage:
 * <pre>{@code
 * PropertyBinder<Label, String> binder = Label::setText;
 * label.bind(textProperty, binder);
 *
 * // Or using lambda:
 * label.bind(scoreProperty, (lbl, value) -> lbl.setText("Score: " + value));
 * }</pre>
 *
 * @param <C> the type of UI component
 * @param <T> the type of property value
 * @see UIProperty
 * @see BindableUIComponent
 */
@FunctionalInterface
public interface PropertyBinder<C, T> {
    /**
     * Applies a property value to a component.
     *
     * @param component the component to modify
     * @param value the property value to apply
     */
    void applyValue(C component, T value);
}
