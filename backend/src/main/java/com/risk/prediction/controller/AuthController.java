package com.risk.prediction.controller;

import com.risk.prediction.entity.User;
import com.risk.prediction.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        Map<String, Object> response = new HashMap<>();
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            response.put("success", true);
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("username", username);
            response.put("user", userInfo);
        } else {
            response.put("success", false);
            response.put("message", "账号或密码错误");
        }
        
        return response;
    }
}
