package com.example.bithumb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bithumb.dto.LoginDto;
import com.example.bithumb.dto.LoginResponse;
import com.example.bithumb.dto.RefreshTokenRequestDto;
import com.example.bithumb.dto.SignUpDto;
import com.example.bithumb.service.SignUpService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SignUpController {

    private final SignUpService signUpService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignUpDto request) {

        signUpService.signup(request);

        return ResponseEntity.ok("회원가입 완료");
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginDto request) {

        LoginResponse response = signUpService.login(request);

        return ResponseEntity.ok(response);
    }

    // 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshTokenRequestDto request) {

        LoginResponse response = signUpService.refresh(request.getRefreshToken());

        return ResponseEntity.ok(response);
    }
}