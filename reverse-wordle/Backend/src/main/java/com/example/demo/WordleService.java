// src/main/java/com/example/demo/WordleService.java
package com.example.demo;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WordleService {

    // Cap the number of white (gray) tiles per row
    private static final int MAX_WHITES = 3;

    private List<String> allWords;
    private List<String> commonWords;
    private String solution;
    private List<Guess> puzzleGuesses;
    private boolean clueUsed = false;

    private final Random rnd = new Random();

    @PostConstruct
    public void loadDictionaries() throws Exception {
        allWords    = load("/allwords.txt");
        commonWords = load("/words.txt");
    }

    private List<String> load(String resource) throws Exception {
        try (var in     = getClass().getResourceAsStream(resource);
             var reader = new BufferedReader(new InputStreamReader(in))) {
            return reader.lines()
                         .map(String::trim)
                         .filter(s -> s.length() == 5)
                         .collect(Collectors.toList());
        }
    }

    /** Starts or restarts a new puzzle */
    public void newGame() {
        solution      = allWords.get(rnd.nextInt(allWords.size()));
        puzzleGuesses = simulateGuesses();
        clueUsed      = false;
    }

    /**
     * Simulate exactly 6 guesses, with the 6th always the solution.
     * Ensure each of the first 5 simulated rows has at most MAX_WHITES white tiles.
     */
    private List<Guess> simulateGuesses() {
        List<Guess> out  = new ArrayList<>();
        Set<String> used = new HashSet<>();

        // Generate first 5 rows under the white-cap constraint
        while (out.size() < 5) {
            String guess = commonWords.get(rnd.nextInt(commonWords.size()));
            if (guess.equals(solution) || !used.add(guess)) {
                continue;  // skip duplicates or the actual solution
            }

            Guess eval = evaluate(guess, solution);
            long whiteCount = eval.colors.stream()
                                         .filter(c -> c.equals("white"))
                                         .count();

            if (whiteCount <= MAX_WHITES) {
                out.add(eval);
            }
            // otherwise drop and try again
        }

        // Final row is the true solution (all blue)
        out.add(evaluate(solution, solution));
        return out;
    }

    /** Returns just the colors for the entire 6×5 board */
    public List<List<String>> getBoardColors() {
        return puzzleGuesses.stream()
                            .map(g -> new ArrayList<>(g.colors))
                            .collect(Collectors.toList());
    }

    /** Validates a user’s reconstruction guess for row idx */
    public boolean validateRow(int idx, String guess) {
        if (idx < 0 || idx >= puzzleGuesses.size()) return false;
        return puzzleGuesses.get(idx).word.equalsIgnoreCase(guess);
    }

    /** Count occurrences in all 6 guess words */
    public int countLetter(char c) {
        if (puzzleGuesses == null) newGame();
        int total = 0;
        for (var g : puzzleGuesses) {
            for (char ch : g.word.toCharArray()) {
                if (ch == c) total++;
            }
        }
        return total;
    }

    /** One-time clue reveal of a full row */
    public Guess revealClue(int idx) {
        if (clueUsed) return null;
        clueUsed = true;
        if (idx < 0 || idx >= puzzleGuesses.size()) return null;
        return puzzleGuesses.get(idx);
    }

    /** Scratch-box evaluation */
    public Guess scratch(String guess) {
        if (!allWords.contains(guess))
            throw new IllegalArgumentException("Not in dictionary");
        return evaluate(guess, solution);
    }

    /** Validate a single letter at (row,col) */
    public boolean validateLetter(int row, int col, char letter) {
        if (puzzleGuesses == null) newGame();
        String target = puzzleGuesses.get(row).word;
        return Character.toLowerCase(letter) == target.charAt(col);
    }

    /** Phantom-Scan: which letters A–Z appear in any guess */
    public Map<String, Boolean> phantomScan() {
        if (puzzleGuesses == null) newGame();
        Map<String, Boolean> map = new TreeMap<>();
        for (char c = 'A'; c <= 'Z'; c++) {
            map.put(String.valueOf(c), false);
        }
        for (Guess g : puzzleGuesses) {
            for (char ch : g.word.toUpperCase().toCharArray()) {
                map.put(String.valueOf(ch), true);
            }
        }
        return map;
    }

    /** Core evaluator identical to Wordle rules */
    private Guess evaluate(String guess, String sol) {
        guess = guess.toLowerCase();
        sol   = sol.toLowerCase();
        List<String> colors = new ArrayList<>(
            List.of("white","white","white","white","white")
        );
        boolean[] matched = new boolean[5];

        // Blues
        for (int i = 0; i < 5; i++) {
            if (guess.charAt(i) == sol.charAt(i)) {
                colors.set(i, "blue");
                matched[i] = true;
            }
        }

        // Oranges
        for (int i = 0; i < 5; i++) {
            if (!matched[i]) {
                for (int j = 0; j < 5; j++) {
                    if (!matched[j] && guess.charAt(i) == sol.charAt(j)) {
                        colors.set(i, "orange");
                        matched[j] = true;
                        break;
                    }
                }
            }
        }

        return new Guess(guess, colors);
    }

    /** DTO for a guess + colors */
    public static class Guess {
        public String        word;
        public List<String> colors;

        public Guess(String w, List<String> c) {
            this.word   = w;
            this.colors = c;
        }
    }
}
