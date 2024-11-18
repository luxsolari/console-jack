package net.luxsolari.handlers;

public interface SystemHandler extends Runnable {
  void init();

  void start();

  void update();

  void stop();

  void cleanUp();

  @Override
  default void run() {
    this.init();
    this.update();
    this.cleanUp();
  }
}
