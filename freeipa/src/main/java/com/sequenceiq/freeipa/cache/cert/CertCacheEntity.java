package com.sequenceiq.freeipa.cache.cert;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.cache.db.DbCacheEntity;
import com.sequenceiq.cloudbreak.cache.db.DbCacheEntityKey;

@Entity
@Table(name = "certcache")
public class CertCacheEntity extends DbCacheEntity<Cert> {

    public CertCacheEntity() {
    }

    public CertCacheEntity(DbCacheEntityKey key, Cert value) {
        super(key, value);
    }
}
