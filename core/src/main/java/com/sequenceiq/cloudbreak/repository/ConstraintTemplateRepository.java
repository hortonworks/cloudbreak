package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@Transactional(TxType.REQUIRED)
@EntityType(entityClass = ConstraintTemplate.class)
@OrganizationResourceType(resource = OrganizationResource.CONSTRAINT_TEMPLATE)
public interface ConstraintTemplateRepository extends OrganizationResourceRepository<ConstraintTemplate, Long> {

    @Query("SELECT t FROM ConstraintTemplate t WHERE t.name= :name and ((t.account= :account and t.publicInAccount=true) or t.owner= :owner) "
            + "AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED'")
    ConstraintTemplate findByNameInAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);
}
