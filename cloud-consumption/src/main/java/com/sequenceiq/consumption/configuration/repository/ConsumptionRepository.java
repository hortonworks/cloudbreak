package com.sequenceiq.consumption.configuration.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.consumption.domain.Consumption;

@Transactional(TxType.REQUIRED)
@EntityType(entityClass = Consumption.class)
public interface ConsumptionRepository extends AccountAwareResourceRepository<Consumption, Long>, JobResourceRepository<Consumption, Long> {

    @Query("SELECT e.id FROM Consumption e " +
            "WHERE e.accountId = :accountId " +
            "AND e.resourceCrn = :resourceCrn ")
    Optional<Long> findIdByResourceCrnAndAccountId(@Param("resourceCrn") String resourceCrn, @Param("accountId") String accountId);

    @Query("SELECT e.id FROM Consumption e " +
            "WHERE e.accountId = :accountId " +
            "AND e.name = :name")
    Optional<Long> findIdByNameAndAccountId(@Param("name") String name, @Param("accountId") String accountId);

    @Query("SELECT e.resourceCrn as resourceCrn, e.id as id, e.name as name " +
            "FROM Consumption e WHERE e.resourceCrn in (:resourceCrns)")
    List<ResourceBasicView> findAllResourceBasicViewByResourceCrns(@Param("resourceCrns") Collection<String> resourceCrns);

    @Query("SELECT e.resourceCrn as remoteResourceId, e.id as localId, e.name as name " +
            "FROM Consumption e WHERE e.id = :resourceId")
    Optional<JobResource> getJobResource(@Param("resourceId") Long resourceId);

    @Query("SELECT e.resourceCrn as resourceCrn, e.id as id, e.name as name " +
            "FROM Consumption e " +
            "WHERE e.name in (:names) " +
            "AND e.accountId = :accountId")
    List<ResourceBasicView> findAllResourceBasicViewByNamesAndAccountId(@Param("names") Collection<String> names, @Param("accountId") String accountId);

    @Query("SELECT c " +
            "FROM Consumption c " +
            "WHERE c.monitoredResourceCrn = :monitoredResourceCrn " +
            "AND c.consumptionType = 'STORAGE' " +
            "AND c.storageLocation = :storageLocation")
    Optional<Consumption> findStorageConsumptionByMonitoredResourceCrnAndLocation(@Param("monitoredResourceCrn") String monitoredResourceCrn,
            @Param("storageLocation") String storageLocation);

    @Query("SELECT COUNT(c)>0 " +
            "FROM Consumption c " +
            "WHERE c.monitoredResourceCrn = :monitoredResourceCrn " +
            "AND c.consumptionType = 'STORAGE' " +
            "AND c.storageLocation = :storageLocation")
    boolean doesStorageConsumptionExistWithLocationForMonitoredCrn(@Param("monitoredResourceCrn") String monitoredResourceCrn,
            @Param("storageLocation") String storageLocation);

    @Query("SELECT c.resourceCrn as remoteResourceId, c.id as localId, c.name as name " +
            "FROM Consumption c " +
            "WHERE c.consumptionType = 'STORAGE' ")
    List<JobResource> findAllStorageConsumptionJobResource();
}