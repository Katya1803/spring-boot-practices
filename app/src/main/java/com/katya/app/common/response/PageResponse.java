package com.katya.app.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    @Builder.Default
    private boolean success = true;

    private String message;

    private List<T> items;

    private PageMetadata pagination;

    private String traceId;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageMetadata {
        private int currentPage;
        private int pageSize;
        private long totalItems;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long total) {
        int totalPages = (int) Math.ceil((double) total / size);

        PageMetadata metadata = PageMetadata.builder()
                .currentPage(page)
                .pageSize(size)
                .totalItems(total)
                .totalPages(totalPages)
                .hasNext(page < totalPages)
                .hasPrevious(page > 1)
                .build();

        return PageResponse.<T>builder()
                .success(true)
                .items(items)
                .pagination(metadata)
                .build();
    }
}