package com.example.wordguessgame;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

@Controller
@RequestMapping("admin/")
public class AdminController {

    @Autowired
    private WordService wordService;

    private static final String UPLOAD_DIR = "src/main/resources/static/";

    // For the word guessing game
    private static final String GAME_SESSION_ATTR = "game";
    private static final String WORD_SESSION_ATTR = "word";
    private static final String GUESSES_SESSION_ATTR = "guesses";
    private static final String GAME_OVER_SESSION_ATTR = "gameOver";

    @GetMapping("words")
    public String getAllWords(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user != null && user.getType().equals("admin")) {
            List<Word> words = wordService.getAllWords();
            model.addAttribute("words", words);
            model.addAttribute("user", user);
            return "word-list";
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("add-word")
    public String addWordForm(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user != null && user.getType().equals("admin")) {
            model.addAttribute("word", null);
            model.addAttribute("user", user);
            return "add-word";
        } else {
            return "redirect:/";
        }
    }

    @PostMapping("add-word")
    public String addWord(@RequestParam String wordName, @RequestParam String hints, @RequestParam String level,
                          @RequestParam MultipartFile imageFile, Model model, HttpSession session) throws IOException {
        User user = (User) session.getAttribute("user");
        if (user != null && user.getType().equals("admin")) {
            String imageName = imageFile.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + imageName);
            Files.createDirectories(path.getParent());
            Files.write(path, imageFile.getBytes());

            Word word = new Word();
            word.setWordName(wordName);
            word.setHints(hints);
            word.setLevel(level);
            word.setImage(imageName);

            wordService.saveWord(word);
            model.addAttribute("user", user);
            return "redirect:/admin/words";
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("start-game")
    public String startGame(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user != null && user.getType().equals("admin")) {
            // Start a new game
            Word word = getRandomWord();
            session.setAttribute(GAME_SESSION_ATTR, "active");
            session.setAttribute(WORD_SESSION_ATTR, word);
            session.setAttribute(GUESSES_SESSION_ATTR, 0);
            session.setAttribute(GAME_OVER_SESSION_ATTR, false);

            model.addAttribute("user", user);
            model.addAttribute("word", word);
            model.addAttribute("guessCount", 0);
            return "game";
        } else {
            return "redirect:/";
        }
    }

    @PostMapping("guess-word")
    public String guessWord(@RequestParam String guess, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user != null && user.getType().equals("admin")) {

            String gameStatus = (String) session.getAttribute(GAME_SESSION_ATTR);
            if (gameStatus == null || !gameStatus.equals("active")) {
                return "redirect:/admin/start-game";  // Start a new game if there's no active game
            }

            Word word = (Word) session.getAttribute(WORD_SESSION_ATTR);
            int guesses = (int) session.getAttribute(GUESSES_SESSION_ATTR);
            boolean gameOver = (boolean) session.getAttribute(GAME_OVER_SESSION_ATTR);

            if (gameOver) {
                return "redirect:/admin/start-game";  // Redirect if the game is over
            }

            if (guess.length() != 5) {
                model.addAttribute("error", "Guess must be exactly 5 letters.");
                return "game";
            }

            // Check guess
            String feedback = getFeedback(guess, word.getWordName());
            guesses++;
            session.setAttribute(GUESSES_SESSION_ATTR, guesses);

            if (feedback.equals("WIN")) {
                session.setAttribute(GAME_OVER_SESSION_ATTR, true);
                model.addAttribute("result", "WIN");
                return "game";
            }

            if (guesses >= 5) {
                session.setAttribute(GAME_OVER_SESSION_ATTR, true);
                model.addAttribute("result", "LOSE");
                model.addAttribute("correctWord", word.getWordName());
                return "game";
            }

            model.addAttribute("feedback", feedback);
            model.addAttribute("guesses", guesses);
            model.addAttribute("user", user);
            return "game";
        } else {
            return "redirect:/";
        }
    }

    private Word getRandomWord() {
        List<Word> words = wordService.getAllWords();
        Random random = new Random();
        return words.get(random.nextInt(words.size()));
    }

    private String getFeedback(String guess, String correctWord) {
        StringBuilder feedback = new StringBuilder();
        char[] guessArray = guess.toCharArray();
        char[] correctArray = correctWord.toCharArray();

        for (int i = 0; i < guessArray.length; i++) {
            if (guessArray[i] == correctArray[i]) {
                feedback.append("green");  // Correct letter, correct position
            } else if (correctWord.contains(String.valueOf(guessArray[i]))) {
                feedback.append("yellow");  // Correct letter, wrong position
            } else {
                feedback.append("black");  // Incorrect letter
            }
        }
        return feedback.toString();
    }

    @GetMapping("update-word")
    public String updateWordForm(@RequestParam Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null && user.getType().equals("admin")) {
            Word word = wordService.getWordById(id);
            model.addAttribute("word", word);
            model.addAttribute("user", user);
            return "add-word";
        } else {
            return "redirect:/";
        }
    }

    @PostMapping("update-word-done")
    public String updateWord(@RequestParam Long wid, @RequestParam String wordName, @RequestParam String hints,
                             @RequestParam String level, @RequestParam(required = false) MultipartFile imageFile, HttpSession session, Model model)
            throws IOException {

        User user = (User) session.getAttribute("user");
        if (user != null && user.getType().equals("admin")) {
            Word existingWord = wordService.getWordById(wid);
            existingWord.setWordName(wordName);
            existingWord.setHints(hints);
            existingWord.setLevel(level);

            if (imageFile != null && !imageFile.isEmpty()) {
                String oldImageName = existingWord.getImage();
                Path oldImagePath = Paths.get(UPLOAD_DIR + oldImageName);
                if (Files.exists(oldImagePath)) {
                    Files.delete(oldImagePath);
                }
                String newImageName = imageFile.getOriginalFilename();
                Path newPath = Paths.get(UPLOAD_DIR + newImageName);
                Files.write(newPath, imageFile.getBytes());
                existingWord.setImage(newImageName);
            }

            wordService.saveWord(existingWord);
            model.addAttribute("user", user);
            return "redirect:/admin/words";
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("delete-word")
    public String deleteWord(@RequestParam Long id, HttpSession session, Model model) throws IOException {
        User user = (User) session.getAttribute("user");
        if (user != null && user.getType().equals("admin")) {
            Word word = wordService.getWordById(id);
            String imageName = word.getImage();
            Path imagePath = Paths.get(UPLOAD_DIR + imageName);
            if (Files.exists(imagePath)) {
                Files.delete(imagePath);
            }
            wordService.deleteWord(id);
            model.addAttribute("user", user);
            return "redirect:/admin/words";
        } else {
            return "redirect:/";
        }
    }
}
