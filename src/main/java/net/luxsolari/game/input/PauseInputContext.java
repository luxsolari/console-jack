package net.luxsolari.game.input;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.Map;
import net.luxsolari.engine.input.InputCommand;
import net.luxsolari.engine.input.InputContext;
import net.luxsolari.engine.input.KeyBinding;

/**
 * Input context for the pause menu state.
 */
public class PauseInputContext implements InputContext {

  private static final Map<KeyBinding, InputCommand> BINDINGS = Map.ofEntries(
      // Navigation
      Map.entry(KeyBinding.of(KeyType.ArrowUp), InputCommand.NAVIGATE_UP),
      Map.entry(KeyBinding.of(KeyType.ArrowDown), InputCommand.NAVIGATE_DOWN),
      Map.entry(KeyBinding.of(KeyType.Home), InputCommand.NAVIGATE_FIRST),
      Map.entry(KeyBinding.of(KeyType.End), InputCommand.NAVIGATE_LAST),

      // Actions
      Map.entry(KeyBinding.fromKeyStroke(new KeyStroke(KeyType.Enter)), InputCommand.CONFIRM),
      Map.entry(KeyBinding.of(KeyType.Escape), InputCommand.RESUME),
      Map.entry(KeyBinding.of(KeyType.EOF), InputCommand.QUIT),

      // Quick shortcuts
      Map.entry(KeyBinding.of('P'), InputCommand.RESUME),
      Map.entry(KeyBinding.of('R'), InputCommand.RESUME),
      Map.entry(KeyBinding.of('Q'), InputCommand.BACK)  // Quit to main menu
  );

  @Override
  public Map<KeyBinding, InputCommand> getBindings() {
    return BINDINGS;
  }

  @Override
  public String getName() {
    return "Pause";
  }
}
