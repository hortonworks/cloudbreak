package com.sequenceiq.environment.credential;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Transactional(TxType.REQUIRED)
interface CredentialRepository extends JpaRepository<Credential, Long> {

    @Query("SELECT c FROM Credential c WHERE c.accountId= :accountId AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    Set<Credential> findActiveForAccountFilterByPlatforms(@Param("accountId") String accountId, @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @Query("SELECT c FROM Credential c WHERE c.name= :name AND c.accountId= :accountId AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    Optional<Credential> findActiveByNameAndAccountIdFilterByPlatforms(@Param("name") String name, @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @Query("SELECT c FROM Credential c WHERE c.id= :id AND c.accountId= :accountId AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    Optional<Credential> findActiveByIdAndAccountFilterByPlatforms(@Param("id") Long id, @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);

}