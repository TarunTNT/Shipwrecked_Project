// src/main/java/com/example/demo/GameController.java
package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GameController {

    @Autowired
    private WordleService svc;

    /** Initialize or restart a puzzle session */
    @GetMapping("/new")
    public void newGame() {
        svc.newGame();
    }

    /** Get the 6×5 color grid (no letters) */
    @GetMapping("/board")
    public List<List<String>> board() {
        return svc.getBoardColors();
    }

    /** Validate a user’s reconstruction guess for row idx */
    @PostMapping("/validate")
    public Map<String, Object> validate(@RequestBody Map<String, Object> body) {
        int row = (int) body.get("row");
        String guess = ((String) body.get("guess")).toLowerCase();
        boolean correct = svc.validateRow(row, guess);
        return Map.of("correct", correct);
    }

    /** Count how many times letter appears across all guesses */
    @GetMapping("/count")
    public Map<String, Object> count(@RequestParam char letter) {
        int cnt = svc.countLetter(Character.toLowerCase(letter));
        return Map.of("letter", letter, "count", cnt);
    }

    /** Reveal one clue row (word + colors), one-time use */
    @GetMapping("/reveal")
    public ResponseEntity<?> reveal(@RequestParam int idx) {
        var clue = svc.revealClue(idx);
        if (clue == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(clue);
    }

    /** Scratch-box: get color feedback on any valid word */
    @PostMapping("/scratch")
    public ResponseEntity<?> scratch(@RequestBody Map<String, String> body) {
        try {
            var res = svc.scratch(body.get("guess").toLowerCase());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Validate a single letter at (row, col) */
    @PostMapping("/validate-letter")
    public Map<String, Object> validateLetter(@RequestBody Map<String, Object> body) {
        int row = (int) body.get("row");
        int col = (int) body.get("col");
        String letter = ((String) body.get("letter")).toLowerCase();
        boolean correct = svc.validateLetter(row, col, letter.charAt(0));
        return Map.of("correct", correct);
    }

    /** One-time Phantom Scan: which letters A–Z appear anywhere in the 6 guesses */
    @GetMapping("/phantom-scan")
    public Map<String, Boolean> phantomScan() {
        return svc.phantomScan();
    }
}
