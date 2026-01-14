package onetoone.AI.controller;

import onetoone.AI.Service.ChatService;
import onetoone.AI.model.ChatMessage;
import onetoone.Users.model.Users;
import onetoone.Users.repository.UserRepository;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);


    // Original endpoint (no auth required) - backward compatible
    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatService.getChatResponse(message);
    }

    // New authenticated endpoint
    @GetMapping("/chat/auth")
    public String chatWithAuth(
            @RequestParam String message,
            @RequestHeader("Authorization") String authToken) {
        return chatService.getChatResponseWithAuth(message, authToken);
    }

    // Get user's chat history
    @GetMapping("/chat/history")
    public ResponseEntity<?> getChatHistory(@RequestHeader("Authorization") String authToken) {
        // Extract token and get user
        String token = authToken.startsWith("Bearer ")
                ? authToken.substring(7)
                : authToken;

        Users user = userRepository.findByauthtoken(token);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        List<ChatMessage> history = chatService.getUserChatHistory(user.getEmailId());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/chat/stats")
    public ResponseEntity<?> getChatStats(@RequestHeader("Authorization") String authToken) {
        String token = authToken.startsWith("Bearer ")
                ? authToken.substring(7)
                : authToken;

        Users user = userRepository.findByauthtoken(token);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        long messageCount = chatService.getUserMessageCount(user.getEmailId());
        return ResponseEntity.ok(Map.of("totalMessages", messageCount));
    }
}