package com.sequenceiq.cloudbreak.eventbus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Event<T> {

    private final T data;

    private final Headers headers;

    private volatile String key;

    public Event(T data) {
        this(new Headers(Map.of()), data);
    }

    public Event(Headers headers, T data) {
        this.headers = headers;
        this.data = data;
    }

    public static <T> Event<T> wrap(T data) {
        return new Event<>(data);
    }

    public T getData() {
        return data;
    }

    public Headers getHeaders() {
        return headers;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "Event{" +
                "data=" + data +
                ", headers=" + headers +
                ", key='" + key + '\'' +
                '}';
    }

    public static class Headers {

        private final Map<String, Object> headerMap;

        public Headers() {
            this.headerMap = new ConcurrentHashMap<>();
        }

        public Headers(Map<String, Object> headerMap) {
            if (headerMap == null) {
                this.headerMap = new ConcurrentHashMap<>();
            } else {
                this.headerMap = new ConcurrentHashMap<>(headerMap.entrySet()
                        .stream()
                        .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            }
        }

        public <V> V get(String key) {
            return (V) headerMap.get(key);
        }

        public void set(String key, Object value) {
            if (key != null) {
                headerMap.merge(key, value, (oldValue, newValue) -> newValue);
            }
        }

        public Map<String, Object> asMap() {
            return headerMap;
        }

        public boolean contains(String key) {
            return headerMap.containsKey(key);
        }

        @Override
        public String toString() {
            return "Headers{" +
                    "headerMap=" + headerMap +
                    '}';
        }
    }
}
