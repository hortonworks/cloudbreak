package com.sequenceiq.environment.encryptionprofile.respository;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

@Transactional(Transactional.TxType.REQUIRED)
public interface EncryptionProfileRepository extends JpaRepository<EncryptionProfile, Long> {

    @Query("SELECT e FROM EncryptionProfile e WHERE e.accountId = :accountId AND e.archived = FALSE")
    List<EncryptionProfile> findAllByAccountId(@Param("accountId") String accountId);

    @Query("SELECT e FROM EncryptionProfile e WHERE e.accountId = :accountId AND e.name = :name "
            + "AND e.archived = FALSE")
    Optional<EncryptionProfile> findByNameAndAccountId(
            @Param("name") String name,
            @Param("accountId") String accountId);

    @Query("SELECT e FROM EncryptionProfile e WHERE e.resourceCrn = :resourceCrn AND e.archived = FALSE")
    Optional<EncryptionProfile> findByResourceCrn(@Param("resourceCrn") String resourceCrn);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(e.id, e.resourceCrn) FROM EncryptionProfile e " +
            "WHERE e.accountId = :accountId AND e.archived = FALSE")
    List<ResourceWithId> findAuthorizationResourcesByAccountId(@Param("accountId") String accountId);

    @Query("SELECT e.resourceCrn FROM EncryptionProfile e WHERE e.name = :resourceName AND e.accountId = :accountId AND e.archived = FALSE")
    Optional<String> findResourceCrnByNameAndAccountId(String resourceName, String accountId);

    @Query("SELECT e.resourceCrn FROM EncryptionProfile e WHERE e.name IN :resourceNames AND e.accountId = :accountId AND e.archived = FALSE")
    List<String> findAllResourceCrnByNameListAndAccountId(List<String> resourceNames, String accountId);

    @Query("SELECT e.name as name " +
            "FROM Environment e " +
            "JOIN e.encryptionProfile ep " +
            "WHERE ep.name = :name " +
            "AND e.accountId = :accountId " +
            "AND ep.accountId = :accountId ")
    List<String> findAllEnvNamesByEncrytionProfileNameAndAccountId(@Param("name") String name, @Param("accountId") String accountId);
}
