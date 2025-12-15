package com.katya.app.module.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestItem {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}
