package com.katya.app.module.test;

import com.katya.app.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    // Health check
    @GetMapping("/actuator/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        log.info("Health check requested");

        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "UP");
        healthData.put("message", "CMS is running");
        healthData.put("timestamp", System.currentTimeMillis());

        ApiResponse<Map<String, Object>> response = ApiResponse.success(healthData);
        response.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(response);
    }

    // Get all items
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<TestItem>>> getAllItems() {
        log.info("Fetching all test items");

        List<TestItem> items = testService.getAllItems();

        ApiResponse<List<TestItem>> response = ApiResponse.success(
                "Fetched " + items.size() + " items",
                items
        );
        response.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(response);
    }

    // Get item by id
    @GetMapping("/items/{id}")
    public ResponseEntity<ApiResponse<TestItem>> getItemById(@PathVariable Long id) {
        log.info("Fetching item with id: {}", id);

        TestItem item = testService.getItemById(id);

        ApiResponse<TestItem> response = ApiResponse.success(item);
        response.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(response);
    }

    // Create new item
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<TestItem>> createItem(@RequestBody TestItem item) {
        log.info("Creating new item: {}", item.getName());

        TestItem created = testService.createItem(item);

        ApiResponse<TestItem> response = ApiResponse.success(
                "Item created successfully",
                created
        );
        response.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(response);
    }

    // Update item
    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<TestItem>> updateItem(
            @PathVariable Long id,
            @RequestBody TestItem item) {
        log.info("Updating item with id: {}", id);

        TestItem updated = testService.updateItem(id, item);

        ApiResponse<TestItem> response = ApiResponse.success(
                "Item updated successfully",
                updated
        );
        response.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(response);
    }

    // Delete item
    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable Long id) {
        log.info("Deleting item with id: {}", id);

        testService.deleteItem(id);

        ApiResponse<Void> response = ApiResponse.success("Item deleted successfully");
        response.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(response);
    }

    // Clear cache endpoint (for testing)
    @DeleteMapping("/cache")
    public ResponseEntity<ApiResponse<Void>> clearCache() {
        log.info("Clearing test item cache");

        testService.clearCache();

        ApiResponse<Void> response = ApiResponse.success("Cache cleared successfully");
        response.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(response);
    }
}