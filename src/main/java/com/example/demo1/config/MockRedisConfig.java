package com.example.demo1.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Redis模拟配置 - 当Redis禁用时使用内存模拟Redis功能
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
public class MockRedisConfig {

    // 使用ConcurrentHashMap模拟Redis存储
    private static final ConcurrentHashMap<String, String> mockRedisStore = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> mockRedisExpiry = new ConcurrentHashMap<>();

    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        return new MockStringRedisTemplate();
    }

    /**
     * 模拟StringRedisTemplate的实现
     */
    public static class MockStringRedisTemplate extends StringRedisTemplate {

        private final MockValueOperations valueOps = new MockValueOperations();

        @Override
        public ValueOperations<String, String> opsForValue() {
            return valueOps;
        }

        @Override
        public Boolean hasKey(String key) {
            cleanExpiredKeys();
            return mockRedisStore.containsKey(key);
        }

        @Override
        public Boolean delete(String key) {
            mockRedisStore.remove(key);
            mockRedisExpiry.remove(key);
            return true;
        }

        private void cleanExpiredKeys() {
            long now = System.currentTimeMillis();
            Set<Map.Entry<String, Long>> entries = mockRedisExpiry.entrySet();

            for (Map.Entry<String, Long> entry : entries) {
                if (entry.getValue() <= now) {
                    mockRedisStore.remove(entry.getKey());
                    mockRedisExpiry.remove(entry.getKey());
                }
            }
        }

        /**
         * 模拟ValueOperations的实现
         */
        private class MockValueOperations implements ValueOperations<String, String> {

            @Override
            public void set(String key, String value) {
                mockRedisStore.put(key, value);
            }

            @Override
            public void set(String key, String value, long timeout, TimeUnit unit) {
                mockRedisStore.put(key, value);
                long expiry = System.currentTimeMillis() + unit.toMillis(timeout);
                mockRedisExpiry.put(key, expiry);
            }

            @Override
            public String get(String key) {
                // 检查是否过期
                Long expiry = mockRedisExpiry.get(key);
                if (expiry != null && expiry <= System.currentTimeMillis()) {
                    mockRedisStore.remove(key);
                    mockRedisExpiry.remove(key);
                    return null;
                }
                return mockRedisStore.get(key);
            }

            @Override
            public String getAndDelete(String key) {
                String value = get(key);
                MockStringRedisTemplate.this.delete(key);
                return value;
            }

            @Override
            public String getAndExpire(String key, long timeout, TimeUnit unit) {
                String value = get(key);
                if (value != null) {
                    set(key, value, timeout, unit);
                }
                return value;
            }

            @Override
            public String getAndExpire(String key, Duration timeout) {
                return getAndExpire(key, timeout.toMillis(), TimeUnit.MILLISECONDS);
            }

            @Override
            public String getAndPersist(String key) {
                String value = get(key);
                if (value != null) {
                    mockRedisExpiry.remove(key);
                }
                return value;
            }

            @Override
            public String getAndSet(String key, String value) {
                String oldValue = get(key);
                set(key, value);
                return oldValue;
            }

            @Override
            public Boolean setIfAbsent(String key, String value) {
                if (!mockRedisStore.containsKey(key)) {
                    set(key, value);
                    return true;
                }
                return false;
            }

            @Override
            public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
                if (!mockRedisStore.containsKey(key)) {
                    set(key, value, timeout, unit);
                    return true;
                }
                return false;
            }

            @Override
            public Boolean setIfPresent(String key, String value) {
                if (mockRedisStore.containsKey(key)) {
                    set(key, value);
                    return true;
                }
                return false;
            }

            @Override
            public Boolean setIfPresent(String key, String value, long timeout, TimeUnit unit) {
                if (mockRedisStore.containsKey(key)) {
                    set(key, value, timeout, unit);
                    return true;
                }
                return false;
            }

            @Override
            public void multiSet(Map<? extends String, ? extends String> map) {
                if (map != null) {
                    mockRedisStore.putAll(map);
                }
            }

            @Override
            public Boolean multiSetIfAbsent(Map<? extends String, ? extends String> map) {
                if (map == null) {
                    return false;
                }

                for (String key : map.keySet()) {
                    if (mockRedisStore.containsKey(key)) {
                        return false;
                    }
                }

                multiSet(map);
                return true;
            }

            @Override
            public Long increment(String key) {
                return increment(key, 1L);
            }

            @Override
            public Long increment(String key, long delta) {
                String value = get(key);
                long newValue;

                if (value == null) {
                    newValue = delta;
                } else {
                    try {
                        newValue = Long.parseLong(value) + delta;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Value is not an integer");
                    }
                }

                set(key, String.valueOf(newValue));
                return newValue;
            }

            @Override
            public Double increment(String key, double delta) {
                String value = get(key);
                double newValue;

                if (value == null) {
                    newValue = delta;
                } else {
                    try {
                        newValue = Double.parseDouble(value) + delta;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Value is not a number");
                    }
                }

                set(key, String.valueOf(newValue));
                return newValue;
            }

            @Override
            public Long decrement(String key) {
                return increment(key, -1L);
            }

            @Override
            public Long decrement(String key, long delta) {
                return increment(key, -delta);
            }

            @Override
            public Integer append(String key, String value) {
                String currentValue = get(key);
                if (currentValue == null) {
                    set(key, value);
                    return value.length();
                }

                String newValue = currentValue + value;
                set(key, newValue);
                return newValue.length();
            }

            @Override
            public String get(Object key) {
                return get(key.toString());
            }

            @Override
            public String get(String key, long start, long end) {
                String value = get(key);
                if (value == null) {
                    return null;
                }

                int length = value.length();
                if (start >= length) {
                    return "";
                }

                end = Math.min(end, length - 1);
                return value.substring((int) start, (int) (end + 1));
            }

            @Override
            public List<String> multiGet(Collection<String> keys) {
                throw new UnsupportedOperationException("Method not implemented");
            }

            @Override
            public void set(String key, String value, long offset) {
                throw new UnsupportedOperationException("Method not implemented");
            }

            @Override
            public Long size(String key) {
                String value = get(key);
                return value != null ? (long) value.length() : 0L;
            }

            @Override
            public Boolean setBit(String key, long offset, boolean value) {
                throw new UnsupportedOperationException("Method not implemented");
            }

            @Override
            public Boolean getBit(String key, long offset) {
                throw new UnsupportedOperationException("Method not implemented");
            }

            @Override
            public List<Long> bitField(String key, BitFieldSubCommands commands) {
                throw new UnsupportedOperationException("Method not implemented");
            }

            @Override
            public RedisOperations<String, String> getOperations() {
                return MockStringRedisTemplate.this;
            }
        }
    }
}