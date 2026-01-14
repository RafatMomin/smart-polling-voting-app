package onetoone.Users.controller;

import java.util.UUID;

import onetoone.Users.dto.ForgotPasswordRequest;
import onetoone.Users.dto.LoginRequest;
import onetoone.Users.dto.ResetPasswordRequest;
import onetoone.Users.repository.UserRepository;
import onetoone.Users.model.Users;
import onetoone.Users.service.MailingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    MailingService emailService;

    private String success = "{\"message\":\"success\"}";
    private String failure = "{\"message\":\"failure\"}";

    @PostMapping(path = "/users")
    String createUser(@RequestBody Users user) {
        if (user == null)
            return failure;
        // prevent same user signing up more than once
        if (userRepository.findByEmailId(user.getEmailId()) != null) {
            return "{\"message\":\"This email is already in use\"}";
        }
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerified(false);
        userRepository.save(user);
        String verificationLink = token;
        emailService.sendVerificationEmail(user.getEmailId(), verificationLink);
        return success;
    }

    @PostMapping("/users/logout")
    public String logout(@RequestHeader("Authorization") String token) {
        Users user = userRepository.findByauthtoken(token);

        if (user != null) {
            user.setAuthtoken(null);
            userRepository.save(user);
            return "{ \"message\":\"Logout successful\" }";
        }
        return "{ \"message\":\"Invalid token\" }";
    }

    @PostMapping("/users/login")
    String login(@RequestBody LoginRequest request) {
        Users user = userRepository.findByEmailIdAndPassword(request.getEmail(), request.getPassword());
        if (user != null && user.isVerified()) {
            String token = UUID.randomUUID().toString();
            user.setAuthtoken(token);
            userRepository.save(user);
            return "{ \"message\":\"Login successful\", \"token\":\"" + token + "\" }";
        }
        return "{\"message\":\"Invalid email, password, or not verified\"}";
    }

    @GetMapping("/users/me")
    public String currentUser(@RequestHeader("Authorization") String token) {
        Users user = userRepository.findByauthtoken(token);

        if (user == null) {
            return "{ \"message\":\"Invalid or expired token\" }";
        }

        return "{ \"user\":\"" + user.getEmailId() + "\" }";
    }

    @GetMapping("/users/verify")
    String verifyUser(@RequestParam String token) {
        Users user = userRepository.findByVerificationToken(token);
        if (user != null) {
            user.setVerified(true);
            user.setVerificationToken(null);
            userRepository.save(user);
            return "{\"message\":\"Email verified successfully!\"}";
        }
        return "{\"message\":\"Invalid or expired token!\"}";
    }

    @PutMapping("/users/forgot-password")
    String forgotPassword(@RequestBody ForgotPasswordRequest request) {
        Users user = userRepository.findByEmailId(request.getEmail());
        if (user == null) {
            return failure;
        }
        String token = generateSixDigitToken();
        user.setResetToken(token);
        userRepository.save(user);

        String resetToken = token;
        emailService.sendPasswordResetEmail(user.getEmailId(), resetToken);
        return "{\"message\":\"Password reset link sent to your email\"}";
    }

    @PutMapping("/users/reset-password")
    String resetPassword(@RequestBody ResetPasswordRequest request) {
        Users user = userRepository.findByResetToken(request.getToken());
        if (user == null) {
            return "{\"message\":\"Invalid or expired reset token\"}";
        }
        user.setPassword(request.getNewPassword());
        user.setResetToken(null);
        userRepository.save(user);
        return "{\"message\":\"Password reset successful\"}";
    }

    @DeleteMapping(path = "/users/{email}")
    String deleteUser(@PathVariable String email) {
        if (userRepository.existsById(email)) {
            userRepository.deleteById(email);
            return success;
        }
        return failure;
    }

    @GetMapping("/users/{email}")
    public String getUserById(@PathVariable String email) {
        Users user = userRepository.findByEmailId(email);
        if (user == null) {
            return "{\"message\":\"No user with the given email.\"}";
        }
        return "{\"user\":\"" + user.getEmailId() + "\"}";
    }

    @PutMapping("/users/{email}")
    public String updateUser(@PathVariable String email, @RequestBody Users updatedUser) {
        Users existingUser = userRepository.findByEmailId(email);
        if (existingUser == null) {
            return "{\"message\":\"No user with the given email.\"}";
        }
        if (updatedUser.getEmailId() != null) existingUser.setEmailId(updatedUser.getEmailId());
        if (updatedUser.getPassword() != null) existingUser.setPassword(updatedUser.getPassword());
        if (updatedUser.getName() != null) existingUser.setName(updatedUser.getName());
        userRepository.save(existingUser);
        return "{\"message\":\"User updated!\"}";
    }

    // generate 6 digit reset token
    private String generateSixDigitToken() {
        int code = (int) (Math.random() * 1_000_000); // 0-999999
        return String.format("%06d", code);
    }
}
