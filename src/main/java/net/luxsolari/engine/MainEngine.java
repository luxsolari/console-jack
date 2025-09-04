package net.luxsolari.engine;

import net.luxsolari.engine.systems.internal.MasterSubsystem;

import java.util.logging.LogManager;
import java.util.logging.Logger;

public class MainEngine {
    private static final String VERSION = "0.0.1";
    private static final String NAME = "Solari Engine v" + VERSION;
    private static final String TAG = MainEngine.class.getSimpleName();
    private static final Logger LOGGER = Logger.getLogger(TAG);
    private static final String LOGGING_CONFIG = "logging.properties";

    public static void bootstrap(String[] args) {
        LOGGER.info(NAME);
        // Load logging configuration
        try (var is = MainEngine.class.getClassLoader().getResourceAsStream(LOGGING_CONFIG)) {
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
            } else {
                LOGGER.warning("Could not find logging configuration file");
            }
        } catch (Exception e) {
            LOGGER.warning("Could not load logging configuration file");
        }

        LOGGER.info("[%s] Starting Main".formatted(TAG));
        // Start Master Subsystem
        MasterSubsystem.INSTANCE.run();

        LOGGER.info("[%s] Exiting Main".formatted(TAG));
    }
}
