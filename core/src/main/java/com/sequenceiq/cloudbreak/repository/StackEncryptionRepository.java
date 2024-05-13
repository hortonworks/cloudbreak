package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = StackEncryption.class)
@Transactional(TxType.REQUIRED)
public interface StackEncryptionRepository extends CrudRepository<StackEncryption, Long> {

    Optional<StackEncryption> findStackEncryptionByStackId(@Param("stackId") Long stackId);

    void deleteStackEncryptionByStackId(@Param("stackId") Long stackId);

}

