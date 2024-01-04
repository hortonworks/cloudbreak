package com.sequenceiq.environment.parameters.dao.repository;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import com.sequenceiq.environment.parameters.dao.domain.YarnParameters;

@Transactional(TxType.REQUIRED)
public interface YarnParametersRepository extends BaseParametersRepository<YarnParameters> {
}
