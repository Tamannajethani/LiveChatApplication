package com.chatapp.chatapp.controller;
import com.chatapp.chatapp.model.User;
import com.chatapp.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public String login(@RequestBody User user)
    {
        Optional<User> existingUser=userRepository.findByUsernameAndPassword(user.getUsername(),user.getPassword());
        if(existingUser.isPresent()) {
            return "Login successful for user: " + user.getUsername();
        }
        else {
            return "Invalid username or password";

        }
    }
}
