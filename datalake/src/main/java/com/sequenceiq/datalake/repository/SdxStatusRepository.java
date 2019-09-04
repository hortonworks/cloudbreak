package com.sequenceiq.datalake.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;

@Repository
public interface SdxStatusRepository extends CrudRepository<SdxStatusEntity, Long> {

    SdxStatusEntity findFirstByDatalakeIsOrderByIdDesc(SdxCluster sdxCluster);

    List<SdxStatusEntity> findDistinctFirstByStatusInAndDatalakeIdInOrderByIdDesc(Collection<DatalakeStatusEnum> datalakeStatusEnums,
            Collection<Long> datalakeId);

}
