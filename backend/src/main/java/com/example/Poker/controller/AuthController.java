package com.example.Poker.controller;

import com.example.Poker.dto.AuthDto;
import com.example.Poker.entity.User;
import com.example.Poker.security.JwtUtil;
import com.example.Poker.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Server is reachable");
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDto.AuthResponse> register(@RequestBody AuthDto.RegisterRequest request) {
        User user = userService.register(request);
        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(new AuthDto.AuthResponse(token, user.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@RequestBody AuthDto.LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        User user = userService.findByUsername(request.username());
        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(new AuthDto.AuthResponse(token, user.getUsername()));
    }
}
