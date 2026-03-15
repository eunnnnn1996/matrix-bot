package com.example.bithumb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bithumb.domain.User;
import com.example.bithumb.dto.RefreshTokenRequestDto;
import com.example.bithumb.repository.UserRepository;
import com.example.bithumb.security.JwtProvider;
import com.example.bithumb.service.SignUpService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final SignUpService signUpService;

    // 현재 로그인한 사용자 정보 조회, 토큰 조회
    @GetMapping("/me")
    public User me(@RequestHeader("Authorization") String header) {

        String token = header.substring(7);

        Long userId = jwtProvider.getUserId(token);

        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenRequestDto request){

        signUpService.logout(request.getRefreshToken());

        return ResponseEntity.ok("로그아웃 완료");
    }
}