package com.sequenceiq.cloudbreak.cache.db;

import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class DbCacheEntity<V> {

    @EmbeddedId
    private DbCacheEntityKey key;

    @Embedded
    private V value;

    protected DbCacheEntity() {
    }

    public DbCacheEntity(DbCacheEntityKey key, V value) {
        this.key = key;
        this.value = value;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public DbCacheEntityKey getKey() {
        return key;
    }

    public void setKey(DbCacheEntityKey key) {
        this.key = key;
    }
}
