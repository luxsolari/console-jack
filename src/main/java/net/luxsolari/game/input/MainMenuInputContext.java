package net.luxsolari.game.input;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.Map;
import net.luxsolari.engine.input.InputCommand;
import net.luxsolari.engine.input.InputContext;
import net.luxsolari.engine.input.KeyBinding;

/**
 * Input context for the main menu state.
 */
public class MainMenuInputContext implements InputContext {

  private static final Map<KeyBinding, InputCommand> BINDINGS = Map.ofEntries(
      // Navigation
      Map.entry(KeyBinding.of(KeyType.ArrowUp), InputCommand.NAVIGATE_UP),
      Map.entry(KeyBinding.of(KeyType.ArrowDown), InputCommand.NAVIGATE_DOWN),
      Map.entry(KeyBinding.of(KeyType.Home), InputCommand.NAVIGATE_FIRST),
      Map.entry(KeyBinding.of(KeyType.End), InputCommand.NAVIGATE_LAST),

      // Actions
      Map.entry(KeyBinding.fromKeyStroke(new KeyStroke(KeyType.Enter)), InputCommand.CONFIRM),
      Map.entry(KeyBinding.of(KeyType.EOF), InputCommand.QUIT),

      // Shortcuts
      Map.entry(KeyBinding.of('Q', true, false, false), InputCommand.QUIT),  // Ctrl+Q
      Map.entry(KeyBinding.of('Q'), InputCommand.QUIT)  // Q to quit
  );

  @Override
  public Map<KeyBinding, InputCommand> getBindings() {
    return BINDINGS;
  }

  @Override
  public String getName() {
    return "MainMenu";
  }
}
