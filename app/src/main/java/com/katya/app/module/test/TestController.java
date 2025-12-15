package com.katya.app.module.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    private final TestItemMapper testItemMapper;

    public TestController(TestItemMapper testItemMapper) {
        this.testItemMapper = testItemMapper;
    }

    // Health check
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        log.info("Health check requested");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Order Management System is running");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    // Get all items
    @GetMapping("/items")
    public ResponseEntity<List<TestItem>> getAllItems() {
        log.info("Fetching all test items");
        List<TestItem> items = testItemMapper.findAll();
        log.debug("Found {} items", items.size());
        return ResponseEntity.ok(items);
    }

    // Get item by id
    @GetMapping("/items/{id}")
    public ResponseEntity<TestItem> getItemById(@PathVariable Long id) {
        log.info("Fetching item with id: {}", id);
        TestItem item = testItemMapper.findById(id);
        if (item == null) {
            log.warn("Item not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    // Create new item
    @PostMapping("/items")
    public ResponseEntity<TestItem> createItem(@RequestBody TestItem item) {
        log.info("Creating new item: {}", item.getName());
        testItemMapper.insert(item);
        log.info("Created item with id: {}", item.getId());
        return ResponseEntity.ok(item);
    }

    // Update item
    @PutMapping("/items/{id}")
    public ResponseEntity<TestItem> updateItem(@PathVariable Long id, @RequestBody TestItem item) {
        log.info("Updating item with id: {}", id);
        TestItem existing = testItemMapper.findById(id);
        if (existing == null) {
            log.warn("Item not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        item.setId(id);
        testItemMapper.update(item);
        log.info("Updated item: {}", id);
        return ResponseEntity.ok(testItemMapper.findById(id));
    }

    // Delete item
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Map<String, String>> deleteItem(@PathVariable Long id) {
        log.info("Deleting item with id: {}", id);
        TestItem existing = testItemMapper.findById(id);
        if (existing == null) {
            log.warn("Item not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        testItemMapper.deleteById(id);
        log.info("Deleted item: {}", id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Item deleted successfully");
        return ResponseEntity.ok(response);
    }
}