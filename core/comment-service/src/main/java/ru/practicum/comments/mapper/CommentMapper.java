package ru.practicum.comments.mapper;

import ewm.event.mapper.EventMapper;
import ewm.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.comments.dto.CommentDto;
import ru.practicum.comments.dto.PostCommentParam;
import ru.practicum.comments.model.Comment;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {EventMapper.class, UserMapper.class})
public interface CommentMapper {

    @Mapping(target = "event", ignore = true)
    @Mapping(target = "author", ignore = true)
    Comment postToComment(PostCommentParam postCommentParam);

    CommentDto toCommentDto(Comment comment);

    List<CommentDto> toFullDtoList(List<Comment> comments);
}