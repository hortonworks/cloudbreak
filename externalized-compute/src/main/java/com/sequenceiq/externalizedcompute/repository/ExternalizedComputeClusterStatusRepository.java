package com.sequenceiq.externalizedcompute.repository;

import java.util.Collection;
import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatus;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum;

@Repository
@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = ExternalizedComputeClusterStatus.class)
public interface ExternalizedComputeClusterStatusRepository extends CrudRepository<ExternalizedComputeClusterStatus, Long> {

    ExternalizedComputeClusterStatus findFirstByExternalizedComputeClusterIsOrderByIdDesc(ExternalizedComputeCluster externalizedComputeCluster);

    ExternalizedComputeClusterStatus findFirstByExternalizedComputeClusterIdIsOrderByIdDesc(Long id);

    @Query("SELECT eccs FROM ExternalizedComputeClusterStatus eccs WHERE eccs.id IN " +
            "( SELECT max(ieccs.id) FROM ExternalizedComputeClusterStatus ieccs WHERE ieccs.status IN :statuses and " +
            "ieccs.externalizedComputeCluster.id IN :ids GROUP BY (ieccs.externalizedComputeCluster.id))")
    List<ExternalizedComputeClusterStatus> findLatestStatusesFilteredByStatusesAndClusterIds(
            @Param("statuses") Collection<ExternalizedComputeClusterStatusEnum> statuses,
            @Param("ids") Collection<Long> ids);

}
