package com.bfrost.backend.common;

import java.util.List;

public record CursorPage<T>(
        List<T> items,
        String nextCursor,
        boolean hasMore
) {
    public static <T> CursorPage<T> of(List<T> items, int requestedSize, java.util.function.Function<T, String> cursorExtractor) {
        boolean hasMore = items.size() == requestedSize;
        List<T> page = hasMore ? items.subList(0, requestedSize - 1) : items;
        String nextCursor = (hasMore && !page.isEmpty()) ? cursorExtractor.apply(page.get(page.size() - 1)) : null;
        return new CursorPage<>(page, nextCursor, hasMore);
    }
}