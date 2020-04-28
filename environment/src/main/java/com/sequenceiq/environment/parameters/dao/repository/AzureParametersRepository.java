package com.sequenceiq.environment.parameters.dao.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.environment.parameters.dao.domain.AzureParameters;

@Transactional(TxType.REQUIRED)
public interface AzureParametersRepository extends BaseParametersRepository<AzureParameters> {
}
