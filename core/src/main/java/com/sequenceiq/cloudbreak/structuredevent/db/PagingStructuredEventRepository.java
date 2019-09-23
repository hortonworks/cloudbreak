package com.sequenceiq.cloudbreak.structuredevent.db;

import javax.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;

@EntityType(entityClass = StructuredEventEntity.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
@WorkspaceResourceType(resource = WorkspaceResource.STRUCTURED_EVENT)
public interface PagingStructuredEventRepository extends PagingAndSortingRepository<StructuredEventEntity, Long> {

    @CheckPermissionsByReturnValue
    Page<StructuredEventEntity> findByEventTypeAndResourceTypeAndResourceId(StructuredEventType eventType, String resourceType, Long resourceId,
            Pageable pageable);
}
