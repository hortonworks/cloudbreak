package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action.READ;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganization;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = FlexSubscription.class)
@Transactional(Transactional.TxType.REQUIRED)
@OrganizationResourceType(resource = OrganizationResource.FLEXSUBSCRIPTION)
public interface FlexSubscriptionRepository extends OrganizationResourceRepository<FlexSubscription, Long> {

    @CheckPermissionsByReturnValue
    FlexSubscription findFirstByUsedForController(boolean usedForController);

    @CheckPermissionsByReturnValue
    FlexSubscription findFirstByIsDefault(boolean defaultFlag);

    @CheckPermissionsByOrganization(action = READ, organizationIndex = 1)
    Long countByNameAndOrganization(String name, Organization organization);

    @CheckPermissionsByOrganization(action = READ, organizationIndex = 1)
    Long countBySubscriptionIdAndOrganization(String subscriptionId, Organization organization);
}
