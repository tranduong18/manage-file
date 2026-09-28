package com.duong.managefile.service;

import com.duong.managefile.dto.response.UserDetailResponse;
import com.duong.managefile.entity.User;
import com.duong.managefile.exception.AppException;
import com.duong.managefile.exception.ErrorCode;
import com.duong.managefile.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "USER-SERVICE")
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public UserDetailResponse getUserDetail(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return UserDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
