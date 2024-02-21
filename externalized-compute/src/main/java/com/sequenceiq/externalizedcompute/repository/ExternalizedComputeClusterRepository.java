package com.sequenceiq.externalizedcompute.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;

@Repository
@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = ExternalizedComputeCluster.class)
public interface ExternalizedComputeClusterRepository
        extends AccountAwareResourceRepository<ExternalizedComputeCluster, Long>, JobResourceRepository<ExternalizedComputeCluster, Long> {

    @Query("SELECT e.resourceCrn as resourceCrn, e.id as id, e.name as name " +
            "FROM ExternalizedComputeCluster e WHERE e.resourceCrn in (:resourceCrns)")
    List<ResourceBasicView> findAllResourceBasicViewByResourceCrns(@Param("resourceCrns") Collection<String> resourceCrns);

    @Query("SELECT e.resourceCrn as remoteResourceId, e.id as localId, e.name as name " +
            "FROM ExternalizedComputeCluster e " +
            "WHERE e.id = :resourceId")
    Optional<JobResource> getJobResource(@Param("resourceId") Long resourceId);

    @Query("SELECT e.resourceCrn as resourceCrn, e.id as id, e.name as name " +
            "FROM ExternalizedComputeCluster e " +
            "WHERE e.name in (:names) " +
            "AND e.accountId = :accountId")
    List<ResourceBasicView> findAllResourceBasicViewByNamesAndAccountId(@Param("names") Collection<String> names, @Param("accountId") String accountId);

    List<ExternalizedComputeCluster> findAllByEnvironmentCrnAndAccountIdAndDeletedIsNull(String environmentCrn, String accountId);

    Optional<ExternalizedComputeCluster> findByResourceCrnAndAccountIdAndDeletedIsNull(String resourceCrn, String accountId);

    Optional<ExternalizedComputeCluster> findByNameAndAccountIdAndDeletedIsNull(String name, String accountId);

    Optional<ExternalizedComputeCluster> findByIdAndDeletedIsNull(Long id);

}
