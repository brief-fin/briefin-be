package com.briefin.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.briefin.domain.auth.dto.request.LoginRequest;
import com.briefin.domain.auth.dto.request.SignUpRequest;
import com.briefin.domain.auth.dto.response.LoginResponse;
import com.briefin.domain.auth.dto.response.SignUpResponse;
import com.briefin.domain.users.entity.Users;
import com.briefin.domain.users.repository.UsersRepository;
import com.briefin.global.apipayload.code.status.ErrorCode;
import com.briefin.global.apipayload.exception.BriefinException;
import com.briefin.global.security.jwt.JwtProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        if (usersRepository.existsByEmail(request.email())) {
            throw new BriefinException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (!request.password().equals(request.passwordConfirm())) {
            throw new BriefinException(ErrorCode.PASSWORD_MISMATCH);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Users user = Users.builder()
                .email(request.email())
                .password(encodedPassword)
                .build();

        Users savedUser = usersRepository.saveAndFlush(user);

        Users foundUser = usersRepository.findById(savedUser.getId())
                .orElseThrow(() -> new BriefinException(ErrorCode.USER_NOT_FOUND));

        return new SignUpResponse(
                foundUser.getId(),
                foundUser.getEmail(),
                foundUser.getCreatedAt()
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Users user = usersRepository.findByEmail(request.email())
                .orElseThrow(() -> new BriefinException(ErrorCode.INVALID_LOGIN));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BriefinException(ErrorCode.INVALID_LOGIN);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail());

        return new LoginResponse(accessToken);
    }
}