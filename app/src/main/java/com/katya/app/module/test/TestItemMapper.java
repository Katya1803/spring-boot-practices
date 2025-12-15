package com.katya.app.module.test;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TestItemMapper {

    List<TestItem> findAll();

    TestItem findById(Long id);

    void insert(TestItem item);

    void update(TestItem item);

    void deleteById(Long id);
}
