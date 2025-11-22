package net.luxsolari.engine.ui;

/**
 * Functional interface representing a command that modifies UI components.
 * Commands are executed on the Master thread by the {@link UICommandQueue}.
 *
 * <p>This interface supports thread-safe UI modifications by allowing any thread
 * to create and enqueue commands, which are then executed serially on the Master
 * thread during the game loop.
 *
 * <p>Example usage:
 * <pre>{@code
 * UICommand command = () -> label.setText("New Text");
 * UICommandQueue.INSTANCE.enqueue(command);
 * }</pre>
 *
 * @see UICommandQueue
 * @see UIUpdateCommands
 */
@FunctionalInterface
public interface UICommand {
    /**
     * Executes this UI command.
     * This method is called by the UICommandQueue on the Master thread.
     *
     * @throws Exception if the command execution fails
     */
    void execute() throws Exception;
}
