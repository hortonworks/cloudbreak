package com.sequenceiq.environment.parameters.dao.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.environment.parameters.dao.domain.GcpParameters;

@Transactional(TxType.REQUIRED)
public interface GcpParametersRepository extends BaseParametersRepository<GcpParameters> {
}
