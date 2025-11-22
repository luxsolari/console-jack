package net.luxsolari.engine.ui;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe command queue for processing UI modifications on the Master thread.
 *
 * <p>This singleton queue allows any thread to safely enqueue UI modification commands,
 * which are then processed serially by the Master thread during the game loop. This ensures
 * thread-safe UI updates without explicit locking.
 *
 * <p>The queue uses the enum singleton pattern (Effective Java, Item 3) for thread-safe
 * lazy initialization and guaranteed single instance.
 *
 * <p>Example usage:
 * <pre>{@code
 * // From any thread:
 * UICommandQueue.INSTANCE.enqueue(() -> label.setText("Score: 100"));
 *
 * // From Master thread (game loop):
 * UICommandQueue.INSTANCE.processAll(); // Executes all pending commands
 * }</pre>
 *
 * <h2>Threading Model</h2>
 * <ul>
 *   <li><b>Enqueue:</b> Thread-safe, can be called from any thread</li>
 *   <li><b>ProcessAll:</b> MUST be called from Master thread only</li>
 *   <li><b>Queue:</b> Unbounded {@link LinkedBlockingQueue} with FIFO ordering</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * If a command throws an exception during execution, the error is logged and
 * processing continues with the next command. This prevents one failing command
 * from blocking subsequent updates.
 *
 * <h2>Monitoring</h2>
 * Enable debug logging with system property: {@code -Dui.commands.debug=true}
 *
 * @see UICommand
 * @see UIUpdateCommands
 */
public enum UICommandQueue {
    /**
     * The singleton instance.
     */
    INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(UICommandQueue.class.getName());
    private static final boolean DEBUG = Boolean.getBoolean("ui.commands.debug");
    private static final int WARN_THRESHOLD = 1000;

    private final BlockingQueue<UICommand> commandQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger queuedCommands = new AtomicInteger(0);
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);

    /**
     * Enqueues a command for execution on the Master thread.
     *
     * <p>This method is thread-safe and can be called from any thread.
     * The command will be executed during the next {@link #processAll()} call.
     *
     * @param command the command to execute
     * @throws NullPointerException if command is null
     */
    public void enqueue(UICommand command) {
        if (command == null) {
            throw new NullPointerException("Command cannot be null");
        }

        commandQueue.offer(command);
        int size = queuedCommands.incrementAndGet();

        if (DEBUG) {
            LOGGER.fine("Enqueued UI command (queue depth: " + size + ")");
        }

        if (size > WARN_THRESHOLD && size % 1000 == 0) {
            LOGGER.warning("UI command queue depth: " + size);
        }
    }

    /**
     * Enqueues multiple commands as an atomic batch.
     *
     * <p>All commands in the batch will be executed together during the next
     * {@link #processAll()} call, without interleaving commands from other sources.
     *
     * @param commands list of commands to execute together
     * @throws NullPointerException if commands list is null
     */
    public void enqueueBatch(List<UICommand> commands) {
        if (commands == null) {
            throw new NullPointerException("Commands list cannot be null");
        }

        if (commands.isEmpty()) {
            return;
        }

        UICommand batchCommand = () -> {
            for (UICommand cmd : commands) {
                cmd.execute();
            }
        };

        enqueue(batchCommand);
    }

    /**
     * Processes all pending commands on the current thread.
     *
     * <p><b>IMPORTANT:</b> This method MUST be called from the Master thread only,
     * typically during the game loop after state updates and before rendering.
     *
     * <p>Commands are executed in FIFO order. If a command throws an exception,
     * the error is logged and processing continues with the next command.
     *
     * <p>Example integration in game loop:
     * <pre>{@code
     * protected void loop() {
     *     currentState.handleInput();
     *     currentState.update();
     *     UICommandQueue.INSTANCE.processAll(); // Process UI updates
     *     currentState.render();
     * }
     * }</pre>
     */
    public void processAll() {
        int count = queuedCommands.getAndSet(0);

        if (DEBUG && count > 0) {
            LOGGER.fine("Processing " + count + " UI commands");
        }

        for (int i = 0; i < count; i++) {
            UICommand command = commandQueue.poll();
            if (command != null) {
                try {
                    command.execute();
                    totalProcessed.incrementAndGet();
                } catch (Exception e) {
                    totalErrors.incrementAndGet();
                    LOGGER.log(Level.WARNING, "UI command execution failed", e);
                }
            }
        }
    }

    /**
     * Returns the current number of pending commands.
     *
     * <p>This is an approximate count useful for monitoring and debugging.
     *
     * @return the number of commands waiting to be processed
     */
    public int pendingCount() {
        return queuedCommands.get();
    }

    /**
     * Clears all pending commands without executing them.
     *
     * <p>This method is primarily useful for testing and cleanup scenarios.
     * Use with caution as it discards all queued UI updates.
     */
    public void clear() {
        commandQueue.clear();
        queuedCommands.set(0);
        if (DEBUG) {
            LOGGER.fine("UI command queue cleared");
        }
    }

    /**
     * Returns the total number of commands successfully processed since startup.
     *
     * @return the total processed command count
     */
    public long getTotalProcessed() {
        return totalProcessed.get();
    }

    /**
     * Returns the total number of commands that failed during execution.
     *
     * @return the total error count
     */
    public long getTotalErrors() {
        return totalErrors.get();
    }
}
