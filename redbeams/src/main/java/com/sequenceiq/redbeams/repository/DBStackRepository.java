package com.sequenceiq.redbeams.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@Transactional(Transactional.TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.DATALAKE)
public interface DBStackRepository extends BaseJpaRepository<DBStack, Long> {

    @CheckPermission(action = ResourceAction.READ)
    Optional<DBStack> findByNameAndEnvironmentId(String name, String environmentId);

    @CheckPermission(action = ResourceAction.READ)
    Optional<DBStack> findByResourceCrn(Crn crn);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT d.id FROM DBStack d LEFT JOIN d.dbStackStatus dss WHERE dss.status IN :statuses")
    Set<Long> findAllByStatusIn(@Param("statuses") Set<Status> statuses);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT d.id FROM DBStack d LEFT JOIN d.dbStackStatus dss WHERE d.id IN :dbStackIds AND dss.status IN :statuses")
    Set<Long> findAllByIdInAndStatusIn(@Param("dbStackIds") Set<Long> dbStackIds, @Param("statuses") Set<Status> statuses);
}
