package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = ManagementPack.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
@OrganizationResourceType(resource = OrganizationResource.MPACK)
public interface ManagementPackRepository extends OrganizationResourceRepository<ManagementPack, Long> {
    ManagementPack findOneById(Long id);

    Set<ManagementPack> findByOwner(String owner);

    Set<ManagementPack> findByAccount(String account);

    ManagementPack findOneByNameAndOwner(String name, String owner);

    @Query("SELECT m FROM ManagementPack m WHERE  m.name= :name and ((m.publicInAccount=true and m.account= :account) or m.owner= :owner)")
    ManagementPack findOneByNameBasedOnAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    ManagementPack findOneByNameAndAccount(String name, String account);

    ManagementPack findOneByIdAndAccount(Long id, String account);

    @Query("SELECT m FROM ManagementPack m WHERE ((m.account= :account AND m.publicInAccount= true) OR m.owner= :user)")
    Set<ManagementPack> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);
}
