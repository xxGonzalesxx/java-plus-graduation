package ru.practicum.mapper;

import ru.practicum.HitDto;
import ru.practicum.model.EndpointHit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EndpointHitMapper {
    EndpointHit mapToEndpointHit(HitDto hitDto);
}
