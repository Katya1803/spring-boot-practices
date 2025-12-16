package com.katya.app.module.test;

import com.katya.app.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class TestService {

    private static final Logger log = LoggerFactory.getLogger(TestService.class);
    private static final String CACHE_KEY_PREFIX = "test:item:";
    private static final String CACHE_KEY_ALL = "test:items:all";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final TestItemMapper testItemMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public TestService(TestItemMapper testItemMapper, RedisTemplate<String, Object> redisTemplate) {
        this.testItemMapper = testItemMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Get all items with Redis caching
     */
    public List<TestItem> getAllItems() {
        log.info("Fetching all test items");

        // Try cache first
        @SuppressWarnings("unchecked")
        List<TestItem> cachedItems = (List<TestItem>) redisTemplate.opsForValue().get(CACHE_KEY_ALL);

        if (cachedItems != null) {
            log.debug("Cache hit for all items");
            return cachedItems;
        }

        // Cache miss - fetch from database
        log.debug("Cache miss - fetching from database");
        List<TestItem> items = testItemMapper.findAll();

        // Store in cache
        redisTemplate.opsForValue().set(CACHE_KEY_ALL, items, CACHE_TTL);
        log.debug("Cached {} items", items.size());

        return items;
    }

    /**
     * Get item by ID with caching
     */
    public TestItem getItemById(Long id) {
        log.info("Fetching item with id: {}", id);

        String cacheKey = CACHE_KEY_PREFIX + id;

        // Try cache first
        TestItem cachedItem = (TestItem) redisTemplate.opsForValue().get(cacheKey);
        if (cachedItem != null) {
            log.debug("Cache hit for item: {}", id);
            return cachedItem;
        }

        // Cache miss - fetch from database
        log.debug("Cache miss - fetching item from database");
        TestItem item = testItemMapper.findById(id);

        if (item == null) {
            throw new BusinessException("ITEM_NOT_FOUND", "Item not found with id: " + id);
        }

        // Store in cache
        redisTemplate.opsForValue().set(cacheKey, item, CACHE_TTL);

        return item;
    }

    /**
     * Create new item and invalidate cache
     */
    @Transactional
    public TestItem createItem(TestItem item) {
        log.info("Creating new item: {}", item.getName());

        // Validate
        if (item.getName() == null || item.getName().trim().isEmpty()) {
            throw new BusinessException("INVALID_NAME", "Item name cannot be empty");
        }

        testItemMapper.insert(item);
        log.info("Created item with id: {}", item.getId());

        // Invalidate all items cache
        redisTemplate.delete(CACHE_KEY_ALL);

        // Cache the new item
        String cacheKey = CACHE_KEY_PREFIX + item.getId();
        redisTemplate.opsForValue().set(cacheKey, item, CACHE_TTL);

        return item;
    }

    /**
     * Update item and invalidate cache
     */
    @Transactional
    public TestItem updateItem(Long id, TestItem item) {
        log.info("Updating item with id: {}", id);

        // Check if exists
        TestItem existing = testItemMapper.findById(id);
        if (existing == null) {
            throw new BusinessException("ITEM_NOT_FOUND", "Item not found with id: " + id);
        }

        // Validate
        if (item.getName() == null || item.getName().trim().isEmpty()) {
            throw new BusinessException("INVALID_NAME", "Item name cannot be empty");
        }

        item.setId(id);
        testItemMapper.update(item);
        log.info("Updated item: {}", id);

        // Invalidate caches
        redisTemplate.delete(CACHE_KEY_ALL);
        redisTemplate.delete(CACHE_KEY_PREFIX + id);

        return testItemMapper.findById(id);
    }

    /**
     * Delete item and invalidate cache
     */
    @Transactional
    public void deleteItem(Long id) {
        log.info("Deleting item with id: {}", id);

        // Check if exists
        TestItem existing = testItemMapper.findById(id);
        if (existing == null) {
            throw new BusinessException("ITEM_NOT_FOUND", "Item not found with id: " + id);
        }

        testItemMapper.deleteById(id);
        log.info("Deleted item: {}", id);

        // Invalidate caches
        redisTemplate.delete(CACHE_KEY_ALL);
        redisTemplate.delete(CACHE_KEY_PREFIX + id);
    }

    /**
     * Clear all test item caches
     */
    public void clearCache() {
        log.info("Clearing all test item caches");
        redisTemplate.delete(CACHE_KEY_ALL);
        // Clear individual item caches (in real app, you'd track these keys)
        log.info("Cache cleared");
    }
}