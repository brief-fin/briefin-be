package com.briefin.domain.users.service;

import com.briefin.domain.users.dto.UserResponseDto;
import com.briefin.domain.users.entity.Users;
import com.briefin.domain.users.repository.ScrapsRepository;
import com.briefin.domain.users.repository.WatchlistRepository;
import com.briefin.domain.users.repository.UsersRepository;
import com.briefin.domain.news.repository.NewsViewRepository;
import com.briefin.global.apipayload.code.status.ErrorCode;
import com.briefin.global.apipayload.exception.BriefinException;
import com.briefin.global.security.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UsersServiceImpl implements UsersService{
    private final UsersRepository usersRepository;
    private final WatchlistRepository watchlistRepository;
    private final ScrapsRepository scrapsRepository;
    private final NewsViewRepository newsViewRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    public UserResponseDto getUser(UUID userId){
        Users user = usersRepository.findById(userId)
                .orElseThrow(() ->new BriefinException(ErrorCode.USER_NOT_FOUND));
        return UserResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        usersRepository.findById(userId)
                .orElseThrow(() -> new BriefinException(ErrorCode.USER_NOT_FOUND));

        watchlistRepository.deleteByUserId(userId);
        scrapsRepository.deleteByUserId(userId);

        newsViewRepository.deleteByUserId(userId);

        refreshTokenService.delete(userId);

        usersRepository.deleteById(userId);
    }
}
