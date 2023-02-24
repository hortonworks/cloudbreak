package com.sequenceiq.datalake.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;

@Repository
public interface SdxStatusRepository extends CrudRepository<SdxStatusEntity, Long> {

    SdxStatusEntity findFirstByDatalakeIsOrderByIdDesc(SdxCluster sdxCluster);

    SdxStatusEntity findFirstByDatalakeIdIsOrderByIdDesc(Long id);

    @Query("SELECT sse FROM SdxStatusEntity sse WHERE sse.id IN " +
            "( SELECT max(isse.id) FROM SdxStatusEntity isse WHERE isse.status IN :statuses and isse.datalake.id IN :datalakeIds " +
            "GROUP BY (isse.datalake.id))")
    List<SdxStatusEntity> findLatestSdxStatusesFilteredByStatusesAndDatalakeIds(@Param("statuses") Collection<DatalakeStatusEnum> statuses,
            @Param("datalakeIds") Collection<Long> datalakeIds);

}
