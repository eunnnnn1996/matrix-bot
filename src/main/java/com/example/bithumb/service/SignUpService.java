package com.example.bithumb.service;

import java.time.LocalDateTime;

import javax.management.relation.Role;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.bithumb.domain.RefreshToken;
import com.example.bithumb.dto.LoginDto;
import com.example.bithumb.dto.LoginResponse;
import com.example.bithumb.dto.SignUpDto;
import com.example.bithumb.repository.RefreshTokenRepository;
import com.example.bithumb.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignUpService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // 회원가입
    public void signup(SignUpDto req){

        if(userRepository.findByEmail(req.getEmail()).isPresent()){
            throw new RuntimeException("이미 존재하는 이메일");
        }

        SignUpDto user = new SignUpDto();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(Role.USER);

        userRepository.save(user);
    }


    // 로그인
    public LoginResponse login(LoginRequest req){

        LoginDto user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        if(!passwordEncoder.matches(req.getPassword(), user.getPassword())){
            throw new RuntimeException("비밀번호 틀림");
        }

        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId());

        saveRefreshToken(user, refreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }


    // refresh token 재발급
    public LoginResponse refresh(String refreshToken){

        RefreshToken token = refreshTokenRepository
                .findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("refresh token 없음"));

        SignUpDto user = token.getUser();

        String newAccessToken = jwtProvider.createAccessToken(user.getUserId(), user.getRole());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getUserId());

        token.setRefreshToken(newRefreshToken);
        refreshTokenRepository.save(token);

        return new LoginResponse(newAccessToken, newRefreshToken);
    }


    private void saveRefreshToken(User user, String refreshToken){

        RefreshToken token = new RefreshToken();

        token.setUser(user);
        token.setRefreshToken(refreshToken);
        token.setExpiryDate(LocalDateTime.now().plusDays(7));

        refreshTokenRepository.save(token);
    }
}