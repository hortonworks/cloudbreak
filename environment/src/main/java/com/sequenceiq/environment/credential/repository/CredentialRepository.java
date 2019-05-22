package com.sequenceiq.environment.credential.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.environment.credential.domain.Credential;

@Transactional(TxType.REQUIRED)
public interface CredentialRepository extends JpaRepository<Credential, Long> {

    @Query("SELECT c FROM Credential c WHERE c.accountId= :accountId AND (c.name= :name OR c.resourceCrn= :name) "
            + "AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    Optional<Credential> findByNameAndAccountId(
            @Param("name") String name,
            @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @Query("SELECT c FROM Credential c WHERE c.accountId= :accountId AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    Set<Credential> findAllByAccountId(
            @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);
}