package ru.practicum.aggregator.model;

import lombok.Getter;
import java.util.Objects;

@Getter
public class EventPair {

    private final long a;
    private final long b;

    public EventPair(long x, long y) {
        if (x <= y) {
            this.a = x;
            this.b = y;
        } else {
            this.a = y;
            this.b = x;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventPair that = (EventPair) o;
        return a == that.a && b == that.b;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }
}
