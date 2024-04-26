package com.sequenceiq.freeipa.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.entity.StackEncryption;

@Transactional(TxType.REQUIRED)
public interface StackEncryptionRepository extends CrudRepository<StackEncryption, Long> {

    Optional<StackEncryption> findStackEncryptionByStackId(@Param("stackId") Long stackId);

    void deleteStackEncryptionByStackId(@Param("stackId") Long stackId);

}
