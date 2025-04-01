package edu.guilford;

public class Player {
    private String name;
    private Hand hand;
    private int lives;

    public Player(String name) {
        this.name = name;
        this.hand = new Hand();
        this.lives = 3;
    }

    public String getName() {
        return name;
    }

    public Hand getHand() {
        return hand;
    }

    public int getLives() {
        return lives;
    }

    public void loseLife() {
        lives--;
    }

    @Override
    public String toString() {
        return name + " (" + lives + " lives): " + hand;
    }
}