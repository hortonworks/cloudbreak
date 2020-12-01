package com.sequenceiq.freeipa.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.freeipa.entity.RootCert;

@Transactional(Transactional.TxType.REQUIRED)
public interface RootCertRepository extends CrudRepository<RootCert, Long> {

    int deleteByStackId(Long stackId);

    Optional<RootCert> findByStackId(Long stackId);
}
