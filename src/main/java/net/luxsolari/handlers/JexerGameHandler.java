package net.luxsolari.handlers;

public class JexerGameHandler implements SystemHandler {
  private static final String TAG = JexerGameHandler.class.getSimpleName() + "System";

  @Override
  public void init() {
    System.out.println("[%s] Initializing Jexer Game Handler".formatted(TAG));
  }

  @Override
  public void start() {
    System.out.println("[%s] Starting Jexer Game Handler".formatted(TAG));
  }

  @Override
  public void update() {
    System.out.println("[%s] Updating Jexer Game Handler".formatted(TAG));
  }

  @Override
  public void stop() {
    System.out.println("[%s] Stopping Jexer Game Handler".formatted(TAG));
  }

  @Override
  public void cleanUp() {
    System.out.println("[%s] Cleaning up Jexer Game Handler".formatted(TAG));
  }
}
