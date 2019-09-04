package com.sequenceiq.environment.parameters.dao.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;

@Transactional(TxType.REQUIRED)
public interface AwsParametersRepository extends BaseParametersRepository<AwsParameters> {
}
