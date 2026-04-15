package net.luxsolari.engine.manager;

import net.luxsolari.engine.systems.internal.MasterSubsystem;

/**
 * <strong>Public API boundary for master game-loop lifecycle operations.</strong>
 *
 * <p>Game code must use this class exclusively when it needs to control the engine's
 * top-level lifecycle (e.g. requesting a clean shutdown). Direct access to
 * {@link MasterSubsystem} from outside the engine package is strongly discouraged —
 * that type is an internal implementation detail and may change without notice.
 *
 * <p>This is a stateless static-utility façade: it holds no data of its own and simply
 * delegates every call to the {@link MasterSubsystem#INSTANCE} enum singleton. Keeping
 * game code behind this boundary means the underlying subsystem can be refactored freely
 * without touching any game-side call sites.
 *
 * <p><strong>Typical usage:</strong>
 * <pre>{@code
 * // Request a graceful engine shutdown from anywhere in game code:
 * MasterManager.stop();
 * }</pre>
 *
 * @see MasterSubsystem
 */
public final class MasterManager {

    /**
     * Not instantiable — all methods are static.
     */
    private MasterManager() {
    }

    /**
     * Requests a graceful shutdown of the entire engine.
     *
     * <p>Delegates to {@link MasterSubsystem#stop()}, which signals the Render, Input,
     * and Audio subsystems to stop before setting the master running flag to
     * {@code false}. The game loop will exit on its next iteration after this call
     * returns.
     */
    public static void stop() {
        MasterSubsystem.INSTANCE.stop();
    }
}
