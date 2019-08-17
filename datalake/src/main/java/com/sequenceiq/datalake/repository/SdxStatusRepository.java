package com.sequenceiq.datalake.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;

@Repository
@AuthorizationResourceType(resource = AuthorizationResource.DATALAKE)
public interface SdxStatusRepository extends CrudRepository<SdxStatusEntity, Long> {

    @CheckPermission(action = ResourceAction.READ)
    SdxStatusEntity findFirstByDatalakeIsOrderByIdDesc(SdxCluster sdxCluster);

    @CheckPermission(action = ResourceAction.READ)
    List<SdxStatusEntity> findDistinctFirstByStatusInAndDatalakeIdOrderByIdDesc(Collection<DatalakeStatusEnum> datalakeStatusEnums,
            Collection<Long> datalakeId);

}
