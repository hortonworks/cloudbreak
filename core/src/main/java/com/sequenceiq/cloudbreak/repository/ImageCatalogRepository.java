package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.ImageCatalog;

@EntityType(entityClass = ImageCatalog.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ImageCatalogRepository extends CrudRepository<ImageCatalog, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    @Query("SELECT ic FROM ImageCatalog ic WHERE ic.imageCatalogName = :name AND ic.archived is false AND "
            + "((ic.account= :account AND ic.publicInAccount= true) OR ic.owner= :owner)")
    ImageCatalog findByName(@Param("name") String name, @Param("owner") String owner, @Param("account") String account);

    @PostAuthorize("hasPermission(returnObject,'read')")
    @Query("SELECT ic FROM ImageCatalog ic WHERE ((ic.account= :account AND ic.publicInAccount= true) OR ic.owner= :owner) AND ic.archived is false")
    List<ImageCatalog> findAllPublicInAccount(@Param("owner") String owner, @Param("account") String account);
}
