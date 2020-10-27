package com.sequenceiq.cloudbreak.cache.db;

public interface DbCacheEntityFactory<E extends DbCacheEntity> {

    E create(Object key, Object value);
}
