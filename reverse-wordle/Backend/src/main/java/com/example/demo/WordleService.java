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
    private boolean[][] revealed; // [row][col] tracks correctly-identified cells
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
    /** Starts or restarts a new puzzle */
    public void newGame() {
        solution      = allWords.get(rnd.nextInt(allWords.size()));
        puzzleGuesses = simulateGuesses();
        clueUsed      = false;
        revealed      = new boolean[6][5];
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
    /** Row indices eligible for a clue: not the last row, not already fully solved */
    public List<Integer> eligibleClueRows() {
        if (puzzleGuesses == null) newGame();
        List<Integer> out = new ArrayList<>();
        for (int r = 0; r < puzzleGuesses.size() - 1; r++) {
            if (!isRowFullySolved(r)) out.add(r);
        }
        return out;
    }



    // ⚠️ DEV/TEST ONLY — remove before shipping to real players.
    public String debugSolution() {
        if (puzzleGuesses == null) newGame();
        return solution;
    }
    /** One-time clue reveal of a full row */
    /** One-time clue reveal of a full row. Row must not be the final (solution) row
     *  and must not already be fully revealed by the player. */
    public Guess revealClue(int idx) {
        if (clueUsed) return null;
        if (idx < 0 || idx >= puzzleGuesses.size()) return null;
        if (idx == puzzleGuesses.size() - 1) return null; // never the last row
        if (isRowFullySolved(idx)) return null;            // never an already-solved row

        clueUsed = true;
        return puzzleGuesses.get(idx);
    }

    private boolean isRowFullySolved(int row) {
        for (int c = 0; c < 5; c++) {
            if (!revealed[row][c]) return false;
        }
        return true;
    }

    /** Scratch-box evaluation */
    public Guess scratch(String guess) {
        if (!allWords.contains(guess))
            throw new IllegalArgumentException("Not in dictionary");
        return evaluate(guess, solution);
    }

    /** Validate a single letter at (row,col) */
    /** Validate a single letter at (row,col). Marks the cell revealed on success. */
    public boolean validateLetter(int row, int col, char letter) {
        if (puzzleGuesses == null) newGame();
        String target = puzzleGuesses.get(row).word;
        boolean correct = Character.toLowerCase(letter) == target.charAt(col);
        if (correct) {
            revealed[row][col] = true;
        }
        return correct;
    }

    /** Phantom-Scan: which letters A–Z appear in any guess */
    /** True once every blue/orange tile on the board has been correctly revealed */
    public boolean isPhaseTwo() {
        if (puzzleGuesses == null) newGame();
        List<List<String>> colors = getBoardColors();
        for (int r = 0; r < colors.size(); r++) {
            for (int c = 0; c < colors.get(r).size(); c++) {
                String color = colors.get(r).get(c);
                if ((color.equals("blue") || color.equals("orange")) && !revealed[r][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * New Phantom Scan (Phase 2): remaining occurrence counts of each letter
     * still hidden in unrevealed white/gray tiles. Letters with zero remaining
     * occurrences are omitted.
     */
    public Map<String, Integer> phantomRemaining() {
        if (puzzleGuesses == null) newGame();
        Map<String, Integer> counts = new TreeMap<>();
        List<List<String>> colors = getBoardColors();
        for (int r = 0; r < puzzleGuesses.size(); r++) {
            String word = puzzleGuesses.get(r).word;
            for (int c = 0; c < word.length(); c++) {
                String color = colors.get(r).get(c);
                if (color.equals("white") && !revealed[r][c]) {
                    String letter = String.valueOf(Character.toUpperCase(word.charAt(c)));
                    counts.merge(letter, 1, Integer::sum);
                }
            }
        }
        return counts;
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
