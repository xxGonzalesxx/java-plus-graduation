package ru.practicum.comments.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.comments.dto.CommentDto;
import ru.practicum.comments.dto.PostCommentParam;
import ru.practicum.comments.model.Comment;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "editedOn", ignore = true)
    Comment postToComment(PostCommentParam postCommentParam);

    CommentDto toCommentDto(Comment comment);

    List<CommentDto> toFullDtoList(List<Comment> comments);
}