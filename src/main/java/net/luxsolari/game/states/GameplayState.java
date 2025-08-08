package net.luxsolari.game.states;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.Random;
import java.util.logging.Logger;
import net.luxsolari.engine.ecs.Entity;
import net.luxsolari.engine.ecs.EntityPool;
import net.luxsolari.engine.ecs.Layer;
import net.luxsolari.engine.ecs.Position;
import net.luxsolari.engine.manager.InputManager;
import net.luxsolari.engine.manager.RenderManager;
import net.luxsolari.engine.manager.StateMachineManager;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.internal.MasterSubsystem;
import net.luxsolari.game.ecs.Card;
import net.luxsolari.game.ecs.CardArt;
import net.luxsolari.game.ecs.CardSprite;

/** Simple placeholder gameplay state used to demonstrate state transitions. */
public class GameplayState implements LoopableState {

  private static final String TAG = GameplayState.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  private Random random;
  private int cardsCreated = 0;
  private static final int CARD_LAYER = 2;

  @Override
  public void start() {
    LOGGER.info("Gameplay started");
    random = new Random();
    cardsCreated = 0;
  }

  @Override
  public void pause() {
    LOGGER.info("Gameplay paused");
  }

  @Override
  public void resume() {
    LOGGER.info("Gameplay resumed");
  }

  @Override
  public void handleInput() {
    if (!renderReady()) {
      return;
    }
    KeyStroke keyStroke = InputManager.poll();
    if (keyStroke == null) {
      return;
    }
    if (keyStroke.getKeyType() == KeyType.EOF) {
      MasterSubsystem.INSTANCE.stop();
      return;
    }

    if (keyStroke.getKeyType() == KeyType.Character) {
      switch (Character.toUpperCase(keyStroke.getCharacter())) {
        case 'P', 'Q' ->
            // P or Q opens pause menu
            StateMachineManager.push(new PauseState());
        case '1' -> createRandomCardEntity();
        case '2' -> clearCards();
        default -> {}
      }
    }
    if (keyStroke.getKeyType() == KeyType.Escape) {
      // Esc behaves like P: open pause menu
      StateMachineManager.push(new PauseState());
    }
  }

  @Override
  public void update() {
    // game logic placeholder
  }

  @Override
  public void render() {
    RenderManager.clear(RenderManager.UI_LAYER);
    if (!renderReady()) return;
    String[] lines = {" Gameplay state ", "Press P or Q or Esc to pause", "Press 1 to create a card", "Press 2 to clear cards"};
    RenderManager.drawCenteredTextBlock(RenderManager.UI_LAYER, lines, true);
  }

  @Override
  public void end() {
    LOGGER.info("Gameplay ended");
    // In a real game, we might want to clean up entities created in this state.
    // For this demo, we'll let them persist.
    clearCards(); // just for this demo.
  }

  private void clearCards() {
    RenderManager.clear(CARD_LAYER);
    EntityPool entityPool = MasterSubsystem.INSTANCE.getEntityPool();
    entityPool.removeWith(CardSprite.class);
    cardsCreated = 0;
  }

  private void createRandomCardEntity() {
    EntityPool entityPool = MasterSubsystem.INSTANCE.getEntityPool();

    // 1. Create a random card
    Card.Rank rank = Card.Rank.values()[random.nextInt(Card.Rank.values().length)];
    Card.Suit suit = Card.Suit.values()[random.nextInt(Card.Suit.values().length)];
    Card card = new Card(rank, suit);

    LOGGER.warning("Creating card: " + card);

    // 2. Create the entity and its components
    Entity cardEntity = entityPool.create();
    if (card.rank() == Card.Rank.JOKER) {
      cardEntity.add(new CardSprite(CardArt.jokerFace(), CardArt.defaultBack(), true));
    } else {
      cardEntity.add(new CardSprite(CardArt.fromCard(card), CardArt.defaultBack(), true));
    }
    // Position cards in a row
    int cardSpacing = CardArt.CARD_COLS + 2;
    int startX = 5;
    int startY = 15;
    cardEntity.add(new Position(startX + (cardsCreated * cardSpacing), startY + 5));
    cardEntity.add(new Layer(CARD_LAYER));

    cardsCreated++;
  }
}
