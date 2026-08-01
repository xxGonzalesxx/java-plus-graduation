package ru.practicum.event.compilation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.compilation.dto.CompilationDto;
import ru.practicum.event.compilation.dto.NewCompilationDto;
import ru.practicum.event.compilation.dto.UpdateCompilationDto;
import ru.practicum.event.compilation.service.CompilationService;


@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/compilations")
public class AdminCompilationController {

    private final CompilationService adminCompilationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto create(@Valid @RequestBody NewCompilationDto compilation) {
        log.info("POST new compilation: {}", compilation);
        return adminCompilationService.create(compilation);
    }

    @DeleteMapping("/{compilationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long compilationId) {
        log.info("DELETE compilation with id={}", compilationId);
        adminCompilationService.delete(compilationId);
    }

    @PatchMapping("/{compilationId}")
    @ResponseStatus(HttpStatus.OK)
    public CompilationDto update(@Valid @RequestBody UpdateCompilationDto compilation, @PathVariable Long compilationId) {
        log.info("PATCH compilation with id={}: {}", compilationId, compilation);
        return adminCompilationService.update(compilation, compilationId);
    }
}
