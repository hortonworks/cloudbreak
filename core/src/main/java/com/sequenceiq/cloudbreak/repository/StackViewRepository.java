package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = StackView.class)
@Transactional(TxType.REQUIRED)
@OrganizationResourceType(resource = OrganizationResource.STACK)
public interface StackViewRepository extends OrganizationResourceRepository<StackView, Long> {

    @DisableCheckPermissions
    @Query("SELECT s FROM StackView s WHERE s.id= :id")
    Optional<StackView> findByIdWithoutAuthorization(@Param("id") Long id);

    @Override
    default <S extends StackView> S save(S entity) {
        throw new UnsupportedOperationException("salala");
    }
}
