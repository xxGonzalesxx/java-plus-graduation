package ru.practicum.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserPostDto;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.user.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    UserDto userToUserDto(User user);

    @Mapping(target = "id", ignore = true)
    User userPostDtoToUser(UserPostDto userPostDto);

    @Mapping(target = "id", ignore = true)
    UserShortDto userToUserShortDto(User user);

    @Mapping(target = "id", ignore = true)
    UserShortDto toShortDto(User user);
}