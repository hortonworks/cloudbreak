package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.freeipa.entity.Credential;

@Transactional(Transactional.TxType.REQUIRED)
public interface CredentialRepository extends CrudRepository<Credential, Long> {

}
