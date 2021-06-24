package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.cloudbreak.domain.CustomConfigs;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = CustomConfigs.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface CustomConfigsRepository extends JpaRepository<CustomConfigs, Long> {

    @Query("SELECT c FROM CustomConfigs c LEFT JOIN FETCH c.configs WHERE c.name=?1 AND c.account=?2")
    Optional<CustomConfigs> findByNameAndAccountId(String name, String accountId);

    @Query("SELECT c FROM CustomConfigs c LEFT JOIN FETCH c.configs WHERE c.resourceCrn=?1")
    Optional<CustomConfigs> findByResourceCrn(String resourceCrn);

    @Query("SELECT DISTINCT c FROM CustomConfigs c LEFT JOIN FETCH c.configs")
    List<CustomConfigs> getAllCustomConfigs();

    @Query("DELETE FROM CustomConfigs c WHERE c.account=?1 AND c.name IN ?2")
    List<CustomConfigs> deleteMultipleByNames(String accountId, Set<String> names);

    @Query("SELECT c.resourceCrn FROM CustomConfigs c WHERE c.account=?1 AND c.name IN ?2")
    List<String> findResourceCrnsByNamesAndAccountId(String accountId, Collection<String> resourceNames);

    @Query("SELECT c.name FROM CustomConfigs c WHERE c.account=?1 AND c.resourceCrn IN ?2")
    List<ResourceCrnAndNameView> findResourceNamesByCrnsAndAccountId(String accountId, Collection<String> resourceCrns);

    @Query("SELECT c FROM CustomConfigs c WHERE c.account=?1")
    List<CustomConfigs> findByAccountId(String accountId);

    @Query("SELECT c.resourceCrn FROM CustomConfigs c WHERE c.name=?1 AND c.account=?2")
    Optional<String> findResourceCrnByNameAndAccountId(String name, String accountId);

}
