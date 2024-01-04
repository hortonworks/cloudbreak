package com.sequenceiq.environment.parameters.dao.repository;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import com.sequenceiq.environment.parameters.dao.domain.GcpParameters;

@Transactional(TxType.REQUIRED)
public interface GcpParametersRepository extends BaseParametersRepository<GcpParameters> {
}
