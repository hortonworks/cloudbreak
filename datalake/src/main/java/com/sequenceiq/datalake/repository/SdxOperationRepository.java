package com.sequenceiq.datalake.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.datalake.entity.operation.SdxOperation;

@Repository
@Transactional(Transactional.TxType.REQUIRED)
public interface SdxOperationRepository extends CrudRepository<SdxOperation, Long> {
    SdxOperation findSdxOperationByOperationId(String operationId);

    SdxOperation findSdxOperationBySdxClusterId(long sdxClusterId);
}