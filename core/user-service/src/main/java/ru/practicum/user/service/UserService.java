package ru.practicum.user.service;

import ru.practicum.user.dto.AdminUserParam;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserPostDto;
import ru.practicum.user.model.User;
import java.util.List;

public interface UserService {
    UserDto create(UserPostDto userPostDto);

    List<UserDto> findAll(AdminUserParam params);

    void delete(Long userId);

    User findById(Long userId);
}
