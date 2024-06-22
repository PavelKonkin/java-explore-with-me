package ru.practicum.page;

import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public class OffsetPage extends PageRequest {
    @Getter
    private final int from;
    private final int size;

    public OffsetPage(int from, int size, Sort sort) {
        super(from > 0 ? from / size : 0, size, sort);
        this.from = from;
        this.size = size;
    }

    @Override
    public int getPageNumber() {
        return from > 0 ? from / size : 0;
    }

    @Override
    public int getPageSize() {
        return size;
    }

    @Override
    public long getOffset() {
        return from;
    }

}

