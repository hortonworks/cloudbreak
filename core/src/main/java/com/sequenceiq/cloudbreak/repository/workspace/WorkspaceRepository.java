package com.sequenceiq.cloudbreak.repository.workspace;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Workspace.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface WorkspaceRepository extends DisabledBaseRepository<Workspace, Long> {

    @Query("SELECT o FROM Workspace o LEFT JOIN FETCH o.tenant WHERE o.name= :name AND o.tenant= :tenant AND o.status <> 'DELETED'")
    Workspace getByName(@Param("name") String name, @Param("tenant") Tenant tenant);

    @Override
    @Query("SELECT o FROM Workspace o LEFT JOIN FETCH o.tenant WHERE o.id= :id AND o.status <> 'DELETED'")
    Optional<Workspace> findById(@Param("id") Long id);

    @Override
    @Query("SELECT o FROM Workspace o WHERE o.id= :id AND o.status <> 'DELETED'")
    boolean existsById(@Param("id") Long id);

    @Override
    @Query("SELECT o FROM Workspace o WHERE o.status <> 'DELETED'")
    Iterable<Workspace> findAll();

    @Override
    @Query("SELECT o FROM Workspace o WHERE o.status <> 'DELETED' AND o.id IN :ids")
    Iterable<Workspace> findAllById(@Param("ids") Iterable<Long> ids);

    @Query("SELECT o FROM Workspace o WHERE o.resourceCrn= :crn AND o.status <> 'DELETED'")
    Optional<Workspace> findByCrn(@Param("crn") String crn);

    @Query("SELECT o FROM Workspace o WHERE o.status <> 'DELETED' AND o.resourceCrn IN :crns")
    Iterable<Workspace> findAllByCrn(@Param("crns") Iterable<String> crns);
}
