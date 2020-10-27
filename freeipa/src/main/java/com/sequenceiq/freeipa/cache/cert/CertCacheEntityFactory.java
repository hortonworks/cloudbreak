package com.sequenceiq.freeipa.cache.cert;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cache.db.DbCacheEntityFactory;
import com.sequenceiq.cloudbreak.cache.db.DbCacheEntityKey;

@Component
public class CertCacheEntityFactory implements DbCacheEntityFactory<CertCacheEntity> {
    @Override
    public CertCacheEntity create(Object key, Object value) {
        return new CertCacheEntity(new DbCacheEntityKey(key.toString()), (Cert) value);
    }
}
