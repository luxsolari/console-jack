package net.luxsolari.game;

import net.luxsolari.engine.MainEngine;

/**
 * Main application class that serves as the entry point for the application. Initializes and
 * manages the MasterGameSubsystem.
 */
public class Main {
  /**
   * Main entry point for the application. Initializes and starts the MasterGameSubsystem.
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    MainEngine.bootstrap(args);
  }
}
