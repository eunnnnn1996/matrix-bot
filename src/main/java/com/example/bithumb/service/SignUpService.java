package com.example.bithumb.service;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.bithumb.domain.RefreshToken;
import com.example.bithumb.domain.User;
import com.example.bithumb.dto.LoginDto;
import com.example.bithumb.dto.LoginResponse;
import com.example.bithumb.dto.RefreshTokenRequestDto;
import com.example.bithumb.dto.SignUpDto;
import com.example.bithumb.repository.RefreshTokenRepository;
import com.example.bithumb.repository.UserRepository;
import com.example.bithumb.security.JwtProvider;
import com.example.bithumb.domain.Role;
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

        User user = new User();

        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(Role.USER);

        userRepository.save(user);
    }

    // 로그인
    public LoginResponse login(LoginDto req){

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        if(!passwordEncoder.matches(req.getPassword(), user.getPassword())){
            throw new RuntimeException("비밀번호 틀림");
        }

        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId());

        saveRefreshToken(user, refreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }

    // refresh token 재발급
    public LoginResponse refresh(String refreshToken){

        RefreshToken token = refreshTokenRepository
                .findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("refresh token 없음"));

        User user = token.getUser();

        String newAccessToken = jwtProvider.createAccessToken(user.getUserId(), user.getRole().name());
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

    public void logout(String refreshToken){

        RefreshToken token = refreshTokenRepository
                .findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("토큰 없음"));

        refreshTokenRepository.delete(token);
    }
}