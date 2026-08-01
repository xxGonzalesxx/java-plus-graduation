package ru.practicum.comments.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comments.dto.CommentDto;
import ru.practicum.comments.dto.PostCommentDto;
import ru.practicum.comments.dto.PostCommentParam;
import ru.practicum.comments.dto.UpdateCommentParam;
import ru.practicum.comments.service.CommentService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
public class PrivateCommentController {
    private final CommentService commentService;

    @GetMapping("/comments")
    public List<CommentDto> findAllByUser(@PathVariable Long userId) {
        return commentService.findAllByAuthor(userId);
    }

    @GetMapping("/comments/{commentId}")
    public CommentDto findByIdAndAuthor(@PathVariable Long userId, @PathVariable Long commentId) {
        return commentService.findByIdAndAuthor(userId, commentId);
    }

    @GetMapping("/events/{eventId}/comments")
    public List<CommentDto> findAllByEventAndAuthor(@PathVariable Long userId, @PathVariable Long eventId) {
        return commentService.findAllByEventAndAuthor(userId, eventId);
    }

    @PostMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto create(@PathVariable Long userId,
                             @PathVariable Long eventId,
                             @Valid @RequestBody PostCommentDto postCommentDto) {
        PostCommentParam postCommentParam = new PostCommentParam(userId, eventId, postCommentDto.comment());
        return commentService.create(postCommentParam);
    }

    @PatchMapping("/comments/{commentId}")
    public CommentDto update(@PathVariable Long userId,
                             @PathVariable Long commentId,
                             @Valid @RequestBody PostCommentDto postCommentDto) {
        UpdateCommentParam updCommentParam = new UpdateCommentParam(userId, commentId, postCommentDto.comment());
        return commentService.update(updCommentParam);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId, @PathVariable Long commentId) {
        commentService.delete(userId, commentId);
    }
}