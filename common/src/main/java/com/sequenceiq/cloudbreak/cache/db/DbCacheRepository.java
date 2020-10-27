package com.sequenceiq.cloudbreak.cache.db;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface DbCacheRepository<T extends DbCacheEntity> extends CrudRepository<T, DbCacheEntityKey> {
}
