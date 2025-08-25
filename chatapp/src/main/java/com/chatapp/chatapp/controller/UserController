package com.chatapp.chatapp.controller;
import com.chatapp.chatapp.model.User;
import com.chatapp.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public User createUser (@RequestBody User user){
        return userService.createUser(user);
    }

    @GetMapping
    public List<User> getAllUsers()
    {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id)
    {
        return userService.getUserById(id).orElse(null);
    }

    @PutMapping("/{id}")
    public User UpdateUser(@PathVariable Long id, @RequestBody User userDetails)
    {
      return userService.updateUser(id, userDetails);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> deleteUser (@PathVariable Long id)
    {

        userService.deleteUser(id);

        Map<String,Boolean> response =new HashMap<>();
        response.put("deleted",true);
        return response;
    }
}
