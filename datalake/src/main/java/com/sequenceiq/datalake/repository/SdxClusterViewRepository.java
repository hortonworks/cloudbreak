package com.sequenceiq.datalake.repository;

import java.util.Collection;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.datalake.entity.SdxClusterView;

@Repository
@Transactional(TxType.REQUIRED)
@EntityType(entityClass = SdxClusterView.class)
public interface SdxClusterViewRepository extends CrudRepository<SdxClusterView, Long> {

    Set<SdxClusterView> findByAccountIdAndEnvCrnIn(String accountId, Collection<String> envCrns);

}
