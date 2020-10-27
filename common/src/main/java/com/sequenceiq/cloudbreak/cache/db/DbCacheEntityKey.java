package com.sequenceiq.cloudbreak.cache.db;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class DbCacheEntityKey implements Serializable {

    private String key;

    public DbCacheEntityKey() {
    }

    public DbCacheEntityKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }
}
