// Main.java
import java.util.*;

import com.example.demo.Evil_Wordle;

public class Main {
    public static void main(String[] args) {
        // Initialize the game
        Evil_Wordle game = new Evil_Wordle();
        String targetWord = game.selectWord();
        System.out.println("Target Word (for testing): " + targetWord);
    }
}