package com.sequenceiq.cloudbreak.cache.db;

import java.util.Optional;
import java.util.concurrent.Callable;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.util.Assert;

public class DatabaseCache<V,E extends DbCacheEntity<V>> extends AbstractValueAdaptingCache {

    private final DbCacheRepository<E> dbCacheRepository;

    private final String name;

    private final DbCacheEntityFactory<E> entityFactory;

    public DatabaseCache(DbCacheRepository<E> dbCacheRepository, String name, DbCacheEntityFactory<E> entityFactory) {
        super(false);
        Assert.notNull(name, "Name must not be null");
        Assert.notNull(dbCacheRepository, "Cache must not be null");
        Assert.notNull(entityFactory, "Entity factory must not be null");
        this.dbCacheRepository = dbCacheRepository;
        this.name = name;
        this.entityFactory = entityFactory;
    }

    @Override
    protected Object lookup(Object key) {
        Optional<E> result = dbCacheRepository.findById(createKey(key));
        if (result.isPresent()) {
            return result.get().getValue();
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return dbCacheRepository;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Optional<E> result = dbCacheRepository.findById(createKey(key));
        if (result.isPresent()) {
            return (T) result.get().getValue();
        } else {
            try {
                T value = valueLoader.call();
                E dbCacheEntity = entityFactory.create(key, value);
                dbCacheRepository.save(dbCacheEntity);
                return value;
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        }
    }

    @Override
    public void put(Object key, Object value) {
        E dbCacheEntity = entityFactory.create(key, value);
        dbCacheRepository.save(dbCacheEntity);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        if (!dbCacheRepository.existsById(createKey(key))) {
            put(key, value);
        }
        return toValueWrapper(value);
    }

    @Override
    public void evict(Object key) {
        dbCacheRepository.deleteById(createKey(key));
    }

    @Override
    public void clear() {
        dbCacheRepository.deleteAll();
    }

    private DbCacheEntityKey createKey(Object key) {
        return new DbCacheEntityKey(key.toString());
    }
}
