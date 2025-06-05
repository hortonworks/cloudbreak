package com.sequenceiq.environment.encryptionprofile.respository;

import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

@Transactional(Transactional.TxType.REQUIRED)
public interface EncryptionProfileRepository extends JpaRepository<EncryptionProfile, Long> {

    @Query("SELECT c FROM EncryptionProfile c WHERE c.accountId = :accountId AND c.name = :name "
            + "AND c.archived = FALSE")
    Optional<EncryptionProfile> findByNameAndAccountId(
            @Param("name") String name,
            @Param("accountId") String accountId);

}
