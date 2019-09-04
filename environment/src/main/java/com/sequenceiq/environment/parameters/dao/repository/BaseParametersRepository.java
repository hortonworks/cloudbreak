package com.sequenceiq.environment.parameters.dao.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;

@Transactional(TxType.REQUIRED)
public interface BaseParametersRepository<T extends BaseParameters> extends JpaRepository<T, Long> {

    Optional<BaseParameters> findByEnvironmentId(Long envId);

}
