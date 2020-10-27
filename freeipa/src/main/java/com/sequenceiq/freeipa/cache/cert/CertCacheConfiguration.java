package com.sequenceiq.freeipa.cache.cert;

import java.lang.reflect.Method;

import javax.inject.Inject;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cache.db.AbstractDbCacheDefinition;
import com.sequenceiq.cloudbreak.cache.db.DatabaseCache;
import com.sequenceiq.cloudbreak.cache.db.DbCacheEntityKey;

@Component
public class CertCacheConfiguration implements AbstractDbCacheDefinition {

    public static final String NAME = "CertCache";

    @Inject
    private CertCacheRepository repository;

    @Inject
    private CertCacheEntityFactory entityFactory;

    @Override
    public Cache cacheConfiguration() {
        return new DatabaseCache<Cert, CertCacheEntity>(repository, NAME, entityFactory);
    }

    @Override
    public Object generateKey(Object target, Method method, Object... params) {
        return new DbCacheEntityKey(SimpleKeyGenerator.generateKey(params).toString());
    }

    @Override
    public Class<?> type() {
        return Cert.class;
    }
}
