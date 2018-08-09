package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = ImageCatalog.class)
@Transactional(Transactional.TxType.REQUIRED)
@HasPermission
public interface ImageCatalogRepository extends BaseRepository<ImageCatalog, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    @Query("SELECT ic FROM ImageCatalog ic WHERE ic.imageCatalogName = :name AND ic.archived is false AND "
            + "((ic.account= :account AND ic.publicInAccount= true) OR ic.owner= :owner)")
    ImageCatalog findByName(@Param("name") String name, @Param("owner") String owner, @Param("account") String account);

    @PostAuthorize("hasPermission(returnObject,'read')")
    @Query("SELECT ic FROM ImageCatalog ic WHERE ((ic.account= :account AND ic.publicInAccount= true) OR ic.owner= :owner) AND ic.archived is false")
    List<ImageCatalog> findAllPublicInAccount(@Param("owner") String owner, @Param("account") String account);
}
