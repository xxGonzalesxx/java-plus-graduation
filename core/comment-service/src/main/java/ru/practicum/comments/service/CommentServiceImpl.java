package ru.practicum.comments.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comments.client.EventClient;
import ru.practicum.comments.client.UserClient;
import ru.practicum.comments.dto.*;
import ru.practicum.comments.mapper.CommentMapper;
import ru.practicum.comments.model.Comment;
import ru.practicum.comments.model.CommentStatus;
import ru.practicum.comments.model.QComment;
import ru.practicum.comments.repository.CommentRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotAuthorized;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentMapper commentMapper;
    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    @Transactional
    public CommentDto create(PostCommentParam postCommentParam) {
        existsUser(postCommentParam.authorId());
        existsEvent(postCommentParam.eventId());

        Comment comment = commentMapper.postToComment(postCommentParam);
        comment.setStatus(CommentStatus.PENDING);
        comment.setCreatedOn(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        log.info("Created new comment {}", savedComment);
        return commentMapper.toCommentDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto update(UpdateCommentParam updCommentParam) {
        Comment comment = existsComment(updCommentParam.commentId());
        existsUser(updCommentParam.author());

        if (!comment.getAuthorId().equals(updCommentParam.author())) {
            throw new NotAuthorized("Comment can be edited only by its author.");
        }

        comment.setComment(updCommentParam.comment());
        comment.setEditedOn(LocalDateTime.now());
        Comment savedComment = commentRepository.save(comment);
        log.info("Updated comment {}", savedComment);
        return commentMapper.toCommentDto(savedComment);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long commentId) {
        Comment comment = existsComment(commentId);

        if (!comment.getAuthorId().equals(userId)) {
            throw new NotAuthorized("Comment can be deleted only by its author.");
        }

        commentRepository.delete(comment);
    }

    @Override
    public List<CommentDto> findAllByAuthor(Long userId) {
        BooleanExpression byAuthorId = QComment.comment1.authorId.eq(userId);
        Iterable<Comment> comments = commentRepository.findAll(byAuthorId);
        return StreamSupport.stream(comments.spliterator(), false)
                .map(commentMapper::toCommentDto)
                .toList();
    }

    @Override
    public List<CommentDto> findAllByEventAndAuthor(Long userId, Long eventId) {
        existsUser(userId);
        existsEvent(eventId);

        BooleanExpression byEventAndAuthorId = QComment.comment1.authorId.eq(userId)
                .and(QComment.comment1.eventId.eq(eventId));
        Iterable<Comment> comments = commentRepository.findAll(byEventAndAuthorId);
        return StreamSupport.stream(comments.spliterator(), false)
                .map(commentMapper::toCommentDto)
                .toList();
    }

    @Override
    public CommentDto findByIdAndAuthor(Long userId, Long commentId) {
        Comment comment = existsComment(commentId);
        existsUser(userId);

        if (!comment.getAuthorId().equals(userId)) {
            throw new NotAuthorized("Only author is allowed to see this comment");
        }

        return commentMapper.toCommentDto(comment);
    }

    @Override
    public List<CommentDto> getPublishedComments(CommentSearchParams params) {
        if (params.rangeStart() != null && params.rangeEnd() != null
                && params.rangeStart().isAfter(params.rangeEnd())) {
            throw new IllegalArgumentException("rangeStart не может быть позже rangeEnd");
        }

        Sort sortBy = Sort.by("createdOn").descending();
        if (params.sort() != null && params.sort().equalsIgnoreCase("asc")) {
            sortBy = Sort.by("createdOn").ascending();
        }
        Pageable pageable = PageRequest.of(params.from() / params.size(), params.size(), sortBy);

        QComment qComment = QComment.comment1;
        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(qComment.status.eq(CommentStatus.PUBLISHED));

        if (params.text() != null && !params.text().isBlank()) {
            predicate.and(qComment.comment.containsIgnoreCase(params.text()));
        }
        if (params.eventId() != null) {
            predicate.and(qComment.eventId.eq(params.eventId()));
        }
        if (params.rangeStart() != null) {
            predicate.and(qComment.createdOn.goe(params.rangeStart()));
        }
        if (params.rangeEnd() != null) {
            predicate.and(qComment.createdOn.loe(params.rangeEnd()));
        }

        List<Comment> comments = commentRepository.findAll(predicate, pageable).getContent();

        return comments.stream()
                .map(commentMapper::toCommentDto)
                .toList();
    }

    @Override
    public CommentDto getPublishedComment(Long commentId) {
        Comment comment = existsComment(commentId);

        if (comment.getStatus() != CommentStatus.PUBLISHED) {
            throw new NotFoundException("Comment is not published");
        }
        return commentMapper.toCommentDto(comment);
    }

    @Override
    public List<CommentDto> searchComments(AdminCommentSearchFilter filter) {
        log.info("Admin search comment with filter: {}", filter);

        if (filter.rangeStart() != null && filter.rangeEnd() != null
                && filter.rangeStart().isAfter(filter.rangeEnd())) {
            throw new ValidationException("rangeEnd cannot be earlier than rangeStart");
        }

        QComment qComment = QComment.comment1;
        BooleanBuilder predicate = new BooleanBuilder();

        Pageable pageable = PageRequest.of(filter.from() / filter.size(), filter.size());

        if (filter.text() != null && !filter.text().isBlank()) {
            predicate.and(qComment.comment.containsIgnoreCase(filter.text()));
        }
        if (filter.users() != null && !filter.users().isEmpty()) {
            predicate.and(qComment.authorId.in(filter.users()));
        }
        if (filter.eventId() != null) {
            predicate.and(qComment.eventId.eq(filter.eventId()));
        }
        if (filter.rangeStart() != null) {
            predicate.and(qComment.createdOn.goe(filter.rangeStart()));
        }
        if (filter.rangeEnd() != null) {
            predicate.and(qComment.createdOn.loe(filter.rangeEnd()));
        }
        if (filter.status() != null) {
            predicate.and(qComment.status.eq(filter.status()));
        }

        List<Comment> comments = commentRepository.findAll(predicate, pageable).getContent();

        if (comments.isEmpty()) {
            return List.of();
        }

        return commentMapper.toFullDtoList(comments);
    }

    @Override
    public CommentDto findCommentById(Long commentId) {
        log.info("Admin find comment id={}", commentId);
        return commentMapper.toCommentDto(existsComment(commentId));
    }

    @Override
    @Transactional
    public CommentDto updateStatusComment(Long commentId, UpdateCommentStatusRequest request) {
        log.info("Admin update comment id={} with status={}", commentId, request.status());

        Comment comment = existsComment(commentId);
        CommentStatus newStatus = request.status();

        if (newStatus == CommentStatus.PUBLISHED && comment.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("You can only publish a comment with status: PENDING");
        }
        if (newStatus == CommentStatus.REJECTED && comment.getStatus() == CommentStatus.PUBLISHED) {
            throw new ConflictException("You cant reject already published comments");
        }

        comment.setStatus(newStatus);
        return commentMapper.toCommentDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        log.info("Admin delete comment id={}", commentId);
        existsComment(commentId);
        commentRepository.deleteById(commentId);
    }

    private Comment existsComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Comment with id=%d was not found", commentId)));
    }

    private void existsUser(Long userId) {
        try {
            userClient.getUserById(userId);
        } catch (Exception e) {
            throw new NotFoundException(String.format("User with id=%d was not found", userId));
        }
    }

    private void existsEvent(Long eventId) {
        try {
            eventClient.getEventById(eventId);
        } catch (Exception e) {
            throw new NotFoundException(String.format("Event with id=%d was not found", eventId));
        }
    }
}