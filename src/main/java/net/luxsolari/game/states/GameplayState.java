package net.luxsolari.game.states;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import net.luxsolari.engine.ecs.Entity;
import net.luxsolari.engine.ecs.EntityPool;
import net.luxsolari.engine.ecs.Layer;
import net.luxsolari.engine.ecs.Position;
import net.luxsolari.engine.manager.AudioManager;
import net.luxsolari.engine.manager.InputManager;
import net.luxsolari.engine.manager.RenderManager;
import net.luxsolari.engine.manager.StateMachineManager;
import net.luxsolari.engine.states.LoopableState;
import net.luxsolari.engine.systems.internal.MasterSubsystem;
import net.luxsolari.engine.systems.internal.RenderSubsystem;
import net.luxsolari.engine.viewport.Anchor;
import net.luxsolari.engine.viewport.ViewportManager;
import net.luxsolari.game.display.CardSizeTier;
import net.luxsolari.game.ecs.Card;
import net.luxsolari.game.ecs.CardArt;
import net.luxsolari.game.ecs.CardSprite;

/** Simple placeholder gameplay state used to demonstrate state transitions. */
public class GameplayState implements LoopableState {

  private static final String TAG = GameplayState.class.getSimpleName();
  private static final Logger LOGGER = Logger.getLogger(TAG);

  private Random random;
  private static final int CARD_LAYER = 2;

  @Override
  public void start() {
    LOGGER.info("Gameplay started");
    random = new Random();
    AudioManager.playBGM("menu_theme_2", true);
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
    redrawLayers();
  }

  @Override
  public void end() {
    LOGGER.info("Gameplay ended");
    // In a real game, we might want to clean up entities created in this state.
    // For this demo, we'll let them persist.
    clearCards(); // just for this demo.
    RenderManager.clear(RenderManager.UI_LAYER); // Clear the text UI
    RenderManager.clear(CARD_LAYER);
    AudioManager.stopBGM();
  }

  private void clearCards() {
    RenderManager.clear(CARD_LAYER);
    EntityPool entityPool = MasterSubsystem.INSTANCE.getEntityPool();
    entityPool.removeWith(CardSprite.class);
  }

  private void createRandomCardEntity() {
    EntityPool entityPool = MasterSubsystem.INSTANCE.getEntityPool();

    // 1. Create a random card
    Card.Rank rank = Card.Rank.values()[random.nextInt(Card.Rank.values().length)];
    Card.Suit suit = Card.Suit.values()[random.nextInt(Card.Suit.values().length)];
    Card card = new Card(rank, suit);

    LOGGER.warning("Creating card: " + card);

    // 2. Determine appropriate card size based on available space
    ViewportManager viewport = ViewportManager.INSTANCE;
    int cardCount = entityPool.with(CardSprite.class).size() + 1; // including new card
    CardSizeTier tier = CardSizeTier.getBestFit(viewport.getWidth(), viewport.getHeight(), cardCount);
    LOGGER.info("Card tier: " + tier);

    // 3. Create the entity and its components
    Entity cardEntity = entityPool.create();

    // Compute the initial position using relative coordinates
    CardLayout layout = computeCardLayout(cardCount, tier);
    float relX = layout.getRelativeX(cardCount - 1); // current card index
    float relY = layout.relativeY;
    cardEntity.add(new Position(relX, relY, Anchor.TOP_LEFT));

    // Create card sprite with appropriate tier sizing
    if (card.rank() == Card.Rank.JOKER) {
      cardEntity.add(new CardSprite(CardArt.jokerFace(), CardArt.defaultBack(tier), true));
    } else {
      cardEntity.add(new CardSprite(CardArt.fromCard(card, tier), CardArt.defaultBack(tier), true));
    }
    cardEntity.add(new Layer(CARD_LAYER));
  }

  private void redrawLayers() {
    if (!renderReady()) {
      return;
    }

    RenderManager.clear(CARD_LAYER);

    // Reposition centered text
    RenderManager.clear(RenderManager.UI_LAYER);
    String[] lines = {
      " Gameplay state ",
      "Press P or Q or Esc to pause",
      "Press 1 to create a card",
      "Press 2 to clear cards"
    };
    RenderManager.drawCenteredTextBlock(RenderManager.UI_LAYER, lines, true);

    // Reposition cards using relative coordinates with tier-aware sizing
    EntityPool entityPool = MasterSubsystem.INSTANCE.getEntityPool();
    List<Entity> cardEntities = entityPool.with(CardSprite.class, Position.class);

    if (!cardEntities.isEmpty()) {
      ViewportManager viewport = ViewportManager.INSTANCE;
      CardSizeTier tier = CardSizeTier.getBestFit(viewport.getWidth(), viewport.getHeight(), cardEntities.size());
      CardLayout layout = computeCardLayout(cardEntities.size(), tier);

      for (int i = 0; i < cardEntities.size(); i++) {
        Entity cardEntity = cardEntities.get(i);
        float relX = layout.getRelativeX(i);
        cardEntity.add(new Position(relX, layout.relativeY, Anchor.TOP_LEFT));
      }
    }
  }

  // Small helper for consistent layout calculations between creation and redraw
  private CardLayout computeCardLayout(int cardCount, CardSizeTier tier) {
    if (cardCount <= 0) {
      return new CardLayout(0.5f, 0.65f, 0.0f, 0, tier); // Center position when no cards
    }

    // Cards are positioned horizontally across the lower portion of the screen
    // Starting at 65% down the screen (0.65 relative Y)
    float relativeY = 0.65f;

    // Calculate relative spacing based on card count and tier
    // Account for actual card width from tier
    ViewportManager viewport = ViewportManager.INSTANCE;
    int availableWidth = viewport.getWidth();
    int cardWidth = tier.width;
    int spacing = 1;

    // Calculate total width needed
    int totalCardWidth = cardCount * cardWidth;
    int totalSpacing = Math.max(0, cardCount - 1) * spacing;
    int totalWidth = totalCardWidth + totalSpacing;

    // Calculate relative positions
    float totalWidthFactor = Math.min(0.8f, (float) totalWidth / availableWidth);
    float startX = (1.0f - totalWidthFactor) / 2.0f; // Center the card group

    return new CardLayout(startX, relativeY, totalWidthFactor, cardCount, tier);
  }

  private static class CardLayout {
    final float startRelativeX;
    final float relativeY;
    final float totalWidthFactor;
    final int cardCount;
    final CardSizeTier tier;

    CardLayout(float startRelativeX, float relativeY, float totalWidthFactor, int cardCount, CardSizeTier tier) {
      this.startRelativeX = startRelativeX;
      this.relativeY = relativeY;
      this.totalWidthFactor = totalWidthFactor;
      this.cardCount = cardCount;
      this.tier = tier;
    }

    /**
     * Gets the relative X position for the card at the given index.
     *
     * @param cardIndex the index of the card (0-based)
     * @return relative X coordinate (0.0-1.0)
     */
    float getRelativeX(int cardIndex) {
      if (cardCount <= 1) {
        return 0.5f; // Center single card
      }

      // Distribute cards evenly across the allocated width, accounting for actual card width
      ViewportManager viewport = ViewportManager.INSTANCE;
      int availableWidth = viewport.getWidth();
      int cardWidth = tier.width;
      int spacing = 1;

      // Calculate spacing between cards in relative terms
      float cardWidthRel = (float) cardWidth / availableWidth;
      float spacingRel = (float) spacing / availableWidth;

      // Position cards with proper spacing
      float totalSpacing = (cardCount - 1) * spacingRel;
      float totalCardsWidth = cardCount * cardWidthRel;
      float totalLayout = totalCardsWidth + totalSpacing;

      float startPos = (1.0f - totalLayout) / 2.0f;
      return startPos + cardIndex * (cardWidthRel + spacingRel);
    }
  }

}
