package edu.guilford;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

public class ThirtyOneGame {
    private List<Player> players;
    private Deck deck;
    private Stack<Card> stockpile;
    private Queue<Card> discardPile;
    private Random random;
    private Player currentPlayer;
    private boolean roundInProgress;
    private boolean knockOccurred;
    private Player knocker;

    public ThirtyOneGame(int numPlayers) {
        if (numPlayers < 2 || numPlayers > 16) {
            throw new IllegalArgumentException("Number of players must be between 2 and 16");
        }

        random = new Random();
        players = new ArrayList<>();
        deck = new Deck();

        // Initialize players
        for (int i = 0; i < numPlayers; i++) {
            players.add(new Player("Player " + (i + 1)));
        }
    }

    public void startNewRound() {
        // Reset round state
        roundInProgress = true;
        knockOccurred = false;
        knocker = null;

        // Prepare deck and piles
        deck.clear();
        deck.build();
        deck.shuffle();

        stockpile = new Stack<>();
        discardPile = new LinkedList<>();

        // Move all cards from deck to stockpile
        while (deck.size() > 0) {
            stockpile.push(deck.deal());
        }

        // Deal 3 cards to each player
        for (Player player : players) {
            player.getHand().reset();
            for (int i = 0; i < 3; i++) {
                player.getHand().addCard(stockpile.pop());
            }
        }

        // Start discard pile
        discardPile.add(stockpile.pop());

        // Set first player
        currentPlayer = players.get(0);
    }

    public void playRound() {
        startNewRound();

        // First pass (no knocking)
        for (Player player : players) {
            currentPlayer = player;
            playTurn(false);
        }

        // Successive passes until knock or 31
        while (roundInProgress) {
            for (Player player : players) {
                if (!roundInProgress)
                    break;

                currentPlayer = player;
                if (player.getHand().getTotalValue() == 31) {
                    endRoundWithThirtyOne(player);
                    break;
                }

                // Player can choose to knock or play
                if (!knockOccurred && shouldKnock(player)) {
                    knockOccurred = true;
                    knocker = player;
                    System.out.println(player.getName() + " knocks!");
                } else {
                    playTurn(knockOccurred);
                }
            }
        }

        // Evaluate round results
        evaluateRound();
    }

    private void playTurn(boolean afterKnock) {
        // Simple AI: 50% chance to take from discard, otherwise from stock
        boolean takeFromDiscard = random.nextBoolean() && !discardPile.isEmpty();
        Card drawnCard;

        if (takeFromDiscard) {
            drawnCard = discardPile.poll();
            System.out.println(currentPlayer.getName() + " takes from discard: " + drawnCard);
        } else {
            drawnCard = stockpile.pop();
            System.out.println(currentPlayer.getName() + " takes from stockpile: " + drawnCard);
        }

        // Add to hand
        currentPlayer.getHand().addCard(drawnCard);

        // Decide which card to discard (simple strategy: discard lowest value card not
        // matching strongest suit)
        Card toDiscard = chooseCardToDiscard(currentPlayer.getHand());
        currentPlayer.getHand().removeCard(toDiscard);
        discardPile.add(toDiscard);

        System.out.println(currentPlayer.getName() + " discards: " + toDiscard);
        System.out.println(currentPlayer.getName() + "'s hand: " + currentPlayer.getHand());
        System.out.println("Current value: " + currentPlayer.getHand().getTotalValue());

        // Check for 31
        if (currentPlayer.getHand().getTotalValue() == 31) {
            endRoundWithThirtyOne(currentPlayer);
        }

        // Check if stockpile is empty
        if (stockpile.isEmpty()) {
            // Move discard pile to stockpile (except top card)
            Card topDiscard = discardPile.poll();
            while (!discardPile.isEmpty()) {
                stockpile.push(discardPile.poll());
            }
            discardPile.add(topDiscard);
            System.out.println("Stockpile exhausted - reshuffling discard pile (except top card)");
        }
    }

    private Card chooseCardToDiscard(Hand hand) {
        // Simple strategy: discard the card that contributes least to the hand's value
        int maxSuitValue = 0;
        Card.Suit strongestSuit = null;

        // Find strongest suit in hand
        for (Card.Suit suit : Card.Suit.values()) {
            int suitValue = 0;
            for (int i = 0; i < hand.size(); i++) {
                Card card = hand.getCard(i);
                if (card.getSuit() == suit) {
                    suitValue += getCardValue(card);
                }
            }
            if (suitValue > maxSuitValue) {
                maxSuitValue = suitValue;
                strongestSuit = suit;
            }
        }

        // Find card not in strongest suit, or lowest value in strongest suit
        Card toDiscard = null;
        int minValue = Integer.MAX_VALUE;

        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.getCard(i);
            int value = getCardValue(card);

            if (card.getSuit() != strongestSuit) {
                // Prefer to discard cards not in strongest suit
                if (toDiscard == null || value < getCardValue(toDiscard)) {
                    toDiscard = card;
                }
            } else if (toDiscard == null || value < minValue) {
                // Otherwise discard lowest value in strongest suit
                minValue = value;
                toDiscard = card;
            }
        }

        return toDiscard;
    }

    private int getCardValue(Card card) {
        switch (card.getRank()) {
            case ACE:
                return 11;
            case TWO:
                return 2;
            case THREE:
                return 3;
            case FOUR:
                return 4;
            case FIVE:
                return 5;
            case SIX:
                return 6;
            case SEVEN:
                return 7;
            case EIGHT:
                return 8;
            case NINE:
                return 9;
            default:
                return 10; // TEN, JACK, QUEEN, KING
        }
    }

    private boolean shouldKnock(Player player) {
        // Simple AI: knock if hand value is 25 or higher
        return player.getHand().getTotalValue() >= 25 && random.nextDouble() < 0.7;
    }

    private void endRoundWithThirtyOne(Player player) {
        System.out.println(player.getName() + " has 31! Round ends.");
        roundInProgress = false;
    }

    private void evaluateRound() {
        if (knockOccurred) {
            // Find player(s) with lowest score
            int minScore = Integer.MAX_VALUE;
            List<Player> losers = new ArrayList<>();

            for (Player player : players) {
                int score = player.getHand().getTotalValue();
                if (score < minScore) {
                    minScore = score;
                    losers.clear();
                    losers.add(player);
                } else if (score == minScore) {
                    losers.add(player);
                }
            }

            // Apply life loss
            for (Player loser : losers) {
                if (loser == knocker) {
                    loser.loseLife();
                    loser.loseLife(); // Knocker loses 2 lives if they have lowest
                    System.out.println(loser.getName() + " knocked but had lowest score! Loses 2 lives.");
                } else {
                    loser.loseLife();
                    System.out.println(loser.getName() + " has lowest score (" + minScore + "). Loses 1 life.");
                }
            }
        } else {
            // All players except the one with 31 lose a life
            for (Player player : players) {
                if (player.getHand().getTotalValue() != 31) {
                    player.loseLife();
                    System.out.println(player.getName() + " loses 1 life.");
                }
            }
        }

        // Remove players with 0 lives
        players.removeIf(player -> player.getLives() <= 0);

        // Print current status
        System.out.println("\nCurrent status:");
        for (Player player : players) {
            System.out.println(player.getName() + ": " + player.getLives() + " lives");
        }
    }

    public void playGame() {
        System.out.println("Starting Thirty-One with " + players.size() + " players!");

        while (players.size() > 1) {
            System.out.println("\n=== Starting new round ===");
            playRound();
        }

        if (players.size() == 1) {
            System.out.println("\n" + players.get(0).getName() + " wins the game!");
        } else {
            System.out.println("\nAll players lost in the same round. Game ends in a tie!");
        }
    }

    public static void main(String[] args) {
        ThirtyOneGame game = new ThirtyOneGame(4); // Play with 4 players
        game.playGame();
    }
}
