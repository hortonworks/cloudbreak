package com.sequenceiq.common.api.tag.base;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonValue;

public abstract class TagsBase implements Serializable {

    @JsonValue
    private final Map<String, String> tags;

    public TagsBase() {
        tags = new HashMap<>();
    }

    public TagsBase(Map<String, String> tags) {
        this.tags = new HashMap<>(tags);
    }

    public TagsBase(TagsBase tags) {
        this();
        if (tags != null) {
            this.tags.putAll(tags.getAll());
        }
    }

    public void addTag(String key, String value) {
        tags.put(key, value);
    }

    public void addTags(Map<String, String> tags) {
        this.tags.putAll(tags);
    }

    public void addTags(TagsBase tags) {
        if (tags != null) {
            this.tags.putAll(tags.getAll());
        }
    }

    public String getTagValue(String key) {
        return tags.get(key);
    }

    public Map<String, String> getAll() {
        return new HashMap<>(tags);
    }

    public Set<String> getKeys() {
        return tags.keySet();
    }

    public Collection<String> getValues() {
        return tags.values();
    }

    public int size() {
        return tags.size();
    }

    public boolean isEmpty() {
        return tags.isEmpty();
    }

    public boolean hasTag(String key) {
        return tags.containsKey(key);
    }

    @Override
    public String toString() {
        return tags.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TagsBase)) {
            return false;
        }
        TagsBase tagsBase = (TagsBase) o;
        return Objects.equals(tags, tagsBase.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tags);
    }
}
