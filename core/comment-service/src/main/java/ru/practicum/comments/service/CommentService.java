package ru.practicum.comments.service;

import ru.practicum.comments.dto.AdminCommentSearchFilter;
import ru.practicum.comments.dto.CommentDto;

import ru.practicum.comments.dto.CommentSearchParams;
import ru.practicum.comments.dto.PostCommentParam;
import ru.practicum.comments.dto.UpdateCommentParam;
import ru.practicum.comments.dto.UpdateCommentStatusRequest;

import java.util.List;

public interface CommentService {
    CommentDto create(PostCommentParam postCommentParam);

    CommentDto update(UpdateCommentParam updCommentParam);

    void delete(Long userId, Long commentId);

    List<CommentDto> findAllByAuthor(Long userId);

    CommentDto findByIdAndAuthor(Long userId, Long commentId);

    List<CommentDto> findAllByEventAndAuthor(Long userId, Long eventId);

    List<CommentDto> getPublishedComments(CommentSearchParams params);

    CommentDto getPublishedComment(Long commentId);

    List<CommentDto> searchComments(AdminCommentSearchFilter filter);

    CommentDto findCommentById(Long commentId);

    CommentDto updateStatusComment(Long commentId, UpdateCommentStatusRequest status);

    void deleteComment(Long commentId);
}