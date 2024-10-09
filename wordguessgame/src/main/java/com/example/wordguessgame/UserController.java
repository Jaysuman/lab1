package com.example.wordguessgame;



import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class UserController {

    @Autowired
    private WordRepository wordRepository;

    @GetMapping("/dashboard")
    public String showForm(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        String winmessage = (String) session.getAttribute("win_message");
        if (user != null) {
            model.addAttribute("user", user);
            model.addAttribute("levels", new String[]{"Easy", "Medium", "Hard"});
            model.addAttribute("selectedLevel", "");
            model.addAttribute("message", winmessage);
            return "word-form";
        } else {
            return "redirect:/";
        }
    }

    @PostMapping("/word")
    public String getWord(@ModelAttribute("selectedLevel") String selectedLevel, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        session.setAttribute("win_message", null);
        if (user != null) {
            Word word = wordRepository.findRandomWordByLevel(selectedLevel);
            model.addAttribute("word", word);
            session.setAttribute("word", word);
            session.setAttribute("attempts", 3);  // Initialize attempts to 3
            session.setAttribute("selectedLevel", selectedLevel); // Store selected level
            return "redirect:/showWord";
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("/showWord")
    public String showWord(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            Word wordarray = (Word) session.getAttribute("word");
            model.addAttribute("GivenHints", wordarray.getHints());
            model.addAttribute("GivenImage", wordarray.getImage());
            model.addAttribute("user", user);
            return "word-input";
        } else {
            return "redirect:/";
        }
    }

    @PostMapping("/getWord")
    public String checkWord(@RequestParam String word, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            Word wordarray = (Word) session.getAttribute("word");
            Integer attempts = (Integer) session.getAttribute("attempts");

            // Display the current hints and image
            model.addAttribute("GivenHints", wordarray.getHints());
            model.addAttribute("GivenImage", wordarray.getImage());

            if (wordarray.getWordName().equalsIgnoreCase(word)) {
                model.addAttribute("message", "Congratulations! You win.");
                int score = Math.toIntExact(user.getScore() + 10);
                return "redirect:/Score/" + user.getId() + "/" + score;
            } else {
                attempts--;  // Decrease attempts by 1
                session.setAttribute("attempts", attempts);

                if (attempts > 0) {
                    model.addAttribute("message", "Wrong guess! You have " + attempts + " attempt(s) left.");
                } else {
                    model.addAttribute("message", "Sorry! You've run out of attempts. The correct word was " + wordarray.getWordName() + ".");

                    // Get the selected level to fetch a new word
                    String selectedLevel = (String) session.getAttribute("selectedLevel");
                    Word newWord = wordRepository.findRandomWordByLevel(selectedLevel);

                    // Set the new word and reset attempts
                    session.setAttribute("word", newWord);
                    session.setAttribute("attempts", 3);  // Reset attempts to 3

                    // Update the model with the new word hints and image
                    model.addAttribute("GivenHints", newWord.getHints());
                    model.addAttribute("GivenImage", newWord.getImage());
                }

                model.addAttribute("user", user);
                return "word-input";  // Stay on the same page to show updated word
            }
        } else {
            return "redirect:/";
}
    }
}