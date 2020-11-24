package com.sequenceiq.freeipa.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.freeipa.entity.RootCert;

@Transactional(Transactional.TxType.REQUIRED)
public interface RootCertRepository extends CrudRepository<RootCert, Long> {

    Optional<RootCert> findByEnvironmentCrn(String environmentCrn);

    int deleteByStackId(Long stackId);
}
