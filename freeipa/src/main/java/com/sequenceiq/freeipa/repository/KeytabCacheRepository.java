package com.sequenceiq.freeipa.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.freeipa.entity.KeytabCache;

@Transactional(Transactional.TxType.REQUIRED)
public interface KeytabCacheRepository extends CrudRepository<KeytabCache, Long> {

    Optional<KeytabCache> findByEnvironmentCrnAndPrincipalHash(String environmentCrn, String principalHash);

    int deleteByEnvironmentCrnAndPrincipalHash(String environmentCrn, String principalHash);

    int deleteByEnvironmentCrn(String environmentCrn);

    int deleteByEnvironmentCrnAndHostName(String environmentCrn, String hostName);
}
