package ru.practicum.event.compilation.service;

import ru.practicum.event.compilation.dto.CompilationDto;
import ru.practicum.event.compilation.dto.NewCompilationDto;
import ru.practicum.event.compilation.dto.UpdateCompilationDto;
import java.util.List;

public interface CompilationService {

    CompilationDto create(NewCompilationDto newCompilationDto);

    void delete(Long compilationId);

    CompilationDto update(UpdateCompilationDto newCompilationDto, Long compilationId);

    List<CompilationDto> getCompilations(boolean pinned, Integer from, Integer size);

    CompilationDto getCompilation(Long compId);
}
