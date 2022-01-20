package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = CustomConfigurations.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface CustomConfigurationsRepository extends CrudRepository<CustomConfigurations, Long> {

    @Query("SELECT c FROM CustomConfigurations c LEFT JOIN FETCH c.configurations WHERE c.name = :customConfigsName AND c.account = :accountId")
    Optional<CustomConfigurations> findByNameAndAccountId(@Param("customConfigsName") String customConfigsName, @Param("accountId") String accountId);

    @Query("SELECT c FROM CustomConfigurations c LEFT JOIN FETCH c.configurations WHERE c.crn = :resourceCrn")
    Optional<CustomConfigurations> findByCrn(@Param("resourceCrn") String resourceCrn);

    @Query("SELECT DISTINCT c FROM CustomConfigurations c LEFT JOIN FETCH c.configurations")
    List<CustomConfigurations> getAllCustomConfigs();

    @Query("SELECT c.crn FROM CustomConfigurations c WHERE c.account = :accountId AND c.name IN (:resourceNames)")
    List<String> findResourceCrnsByNamesAndAccountId(@Param("accountId") String accountId, @Param("resourceNames") Collection<String> resourceNames);

    @Query("SELECT c.name FROM CustomConfigurations c WHERE c.account = :accountId AND c.crn IN (:resourceCrns)")
    List<ResourceCrnAndNameView> findResourceNamesByCrnsAndAccountId(
            @Param("accountId") String accountId, @Param("resourceCrns") Collection<String> resourceCrns);

    @Query("SELECT DISTINCT c FROM CustomConfigurations c LEFT JOIN FETCH c.configurations WHERE c.account = :accountId")
    List<CustomConfigurations> findCustomConfigsByAccountId(@Param("accountId") String accountId);

    @Query("SELECT c.crn FROM CustomConfigurations c WHERE c.name = :name AND c.account = :accountId")
    Optional<String> findResourceCrnByNameAndAccountId(@Param("name") String name, @Param("accountId") String accountId);

}
