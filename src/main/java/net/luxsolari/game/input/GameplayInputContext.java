package net.luxsolari.game.input;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.Map;
import net.luxsolari.engine.input.InputCommand;
import net.luxsolari.engine.input.InputContext;
import net.luxsolari.engine.input.KeyBinding;

/**
 * Input context for the gameplay state.
 */
public class GameplayInputContext implements InputContext {

  private static final Map<KeyBinding, InputCommand> BINDINGS = Map.ofEntries(
      // Game control
      Map.entry(KeyBinding.of('P'), InputCommand.PAUSE),
      Map.entry(KeyBinding.of('Q'), InputCommand.PAUSE),
      Map.entry(KeyBinding.of(KeyType.Escape), InputCommand.PAUSE),
      Map.entry(KeyBinding.of(KeyType.EOF), InputCommand.QUIT),
      Map.entry(KeyBinding.fromKeyStroke(new KeyStroke(KeyType.Enter)), InputCommand.CONFIRM),

      // Blackjack actions
      Map.entry(KeyBinding.of('H'), InputCommand.HIT),
      Map.entry(KeyBinding.of('S'), InputCommand.STAND),
      Map.entry(KeyBinding.of('D'), InputCommand.DOUBLE_DOWN),
      Map.entry(KeyBinding.of('X'), InputCommand.SPLIT),
      Map.entry(KeyBinding.of('R'), InputCommand.SURRENDER),
      Map.entry(KeyBinding.of(' '), InputCommand.HIT),  // Enter = Hit

      // Debug commands
      Map.entry(KeyBinding.of('1'), InputCommand.DEBUG_CREATE_CARD),
      Map.entry(KeyBinding.of('2'), InputCommand.DEBUG_CLEAR_CARDS),
      Map.entry(KeyBinding.of('`'), InputCommand.DEBUG_TOGGLE),

      // Audio controls
      Map.entry(KeyBinding.of('M'), InputCommand.TOGGLE_SOUND),
      Map.entry(KeyBinding.of('+'), InputCommand.VOLUME_UP),
      Map.entry(KeyBinding.of('-'), InputCommand.VOLUME_DOWN),
      Map.entry(KeyBinding.of('='), InputCommand.VOLUME_UP),  // = key without shift

      // Fullscreen
      Map.entry(KeyBinding.of('F', false, true, false), InputCommand.TOGGLE_FULLSCREEN)  // Alt+F
  );

  @Override
  public Map<KeyBinding, InputCommand> getBindings() {
    return BINDINGS;
  }

  @Override
  public String getName() {
    return "Gameplay";
  }
}
