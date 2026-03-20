package com.briefin.domain.users.service;

import com.briefin.domain.users.dto.UserResponseDto;
import com.briefin.domain.users.entity.Users;
import com.briefin.domain.users.repository.UsersRepository;
import com.briefin.global.apipayload.code.status.ErrorCode;
import com.briefin.global.apipayload.exception.BriefinException;
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
    private  final UsersRepository usersRepository;
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
}
