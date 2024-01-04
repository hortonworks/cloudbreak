package com.sequenceiq.freeipa.repository;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.freeipa.entity.StackAuthentication;

@Transactional(TxType.REQUIRED)
public interface StackAuthenticationRepository extends CrudRepository<StackAuthentication, Long> {
}
