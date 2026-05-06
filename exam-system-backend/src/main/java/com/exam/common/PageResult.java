package com.exam.common;

import lombok.Data;

@Data
public class PageResult<T> {
    private Long total;
    private java.util.List<T> records;

    public PageResult(Long total, java.util.List<T> records) {
        this.total = total;
        this.records = records;
    }
}
