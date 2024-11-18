package net.luxsolari.handlers;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.TerminalFactory;
import net.luxsolari.exceptions.ResourceCleanupException;
import net.luxsolari.exceptions.ResourceInitializationException;

import java.io.IOException;
import java.util.logging.Logger;

public class MasterGameHandler implements SystemHandler {

    private static final String TAG = MasterGameHandler.class.getSimpleName();
    private static final Logger LOGGER = Logger.getLogger(TAG);
    private static MasterGameHandler INSTANCE;
    private boolean running = false;

    private Terminal terminal;

    private MasterGameHandler() {}

    public static MasterGameHandler getInstance() {
        if (INSTANCE == null) {
            LOGGER.info("[%s] Creating new Master Game Handler instance".formatted(TAG));
            INSTANCE = new MasterGameHandler();
        }
        return INSTANCE;
    }

    @Override
    public void init() throws ResourceInitializationException {
        LOGGER.info("[%s] Initializing Master Game Handler".formatted(TAG));
        TerminalFactory terminalFactory = new DefaultTerminalFactory();
        try {
            this.terminal = terminalFactory.createTerminal();
            this.terminal.enterPrivateMode();
            this.terminal.setCursorVisible(false);
            this.terminal.clearScreen();
        } catch (IOException e) {
            LOGGER.severe("[%s] Error while initializing Master Game Handler: %s"
                    .formatted(TAG, e.getMessage()));
            throw new ResourceInitializationException("Error while initializing Master Game Handler", e);
        }
        this.start();
    }

    @Override
    public void start() {
        LOGGER.info("[%s] Starting Master Game Handler".formatted(TAG));
        running = true;
    }

    @Override
    public void update() {
        LOGGER.info("[%s] Updating Master Game Handler".formatted(TAG));

        while (running) {
            LOGGER.info("[%s] Running Master Game Handler".formatted(TAG));
            try {
                this.terminal.flush();

                // I know what you're thinking, but since this is scaffolding code, it's going to get replaced
                // with actual game logic in the future. Don't worry about it.
                Thread.sleep(2000); // 0.5 FPS (2000/1000 = 2 seconds)

                // poll for key presses
                KeyStroke keyStroke = this.terminal.pollInput();
                if (keyStroke != null) {
                    if (keyStroke.getKeyType() == KeyType.Character &&
                            keyStroke.getCharacter().toString().equalsIgnoreCase("q")) {
                        this.running = false;
                    }
                }

            } catch (InterruptedException | IOException e) {
                this.running = false;
                LOGGER.severe("[%s] Error while running Master Game Handler: %s"
                        .formatted(TAG, e.getMessage()));
            }
        }

        this.stop();
    }

    @Override
    public void stop() {
        LOGGER.info("[%s] Stopping Master Game Handler".formatted(TAG));
        try {
            this.terminal.exitPrivateMode();
            this.terminal.close();
            running = false;
        } catch (IOException e) {
            LOGGER.severe("[%s] Error while stopping Master Game Handler: %s"
                    .formatted(TAG, e.getMessage()));
            throw new ResourceCleanupException("Error while stopping Master Game Handler", e);
        }
    }

    @Override
    public void cleanUp() {
        LOGGER.info("[%s] Cleaning up Master Game Handler".formatted(TAG));
    }
}
