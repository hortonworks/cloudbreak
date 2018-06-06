package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;

@EntityType(entityClass = ConstraintTemplate.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ConstraintTemplateRepository extends CrudRepository<ConstraintTemplate, Long> {

    @Override
    ConstraintTemplate findOne(@Param("id") Long id);

    @Query("SELECT t FROM ConstraintTemplate t WHERE t.owner= :user AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED'")
    Set<ConstraintTemplate> findForUser(@Param("user") String user);

    @Query("SELECT t FROM ConstraintTemplate t WHERE ((t.account= :account AND t.publicInAccount= true) OR t.owner= :user) "
            + "AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED' ")
    Set<ConstraintTemplate> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    @Query("SELECT t FROM ConstraintTemplate t WHERE t.account= :account AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED'")
    Set<ConstraintTemplate> findAllInAccount(@Param("account") String account);

    @Query("SELECT t FROM ConstraintTemplate t WHERE t.name= :name and t.account= :account AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED' ")
    ConstraintTemplate findOneByName(@Param("name") String name, @Param("account") String account);

    @Query("SELECT t FROM ConstraintTemplate t WHERE t.name= :name and ((t.account= :account and t.publicInAccount=true) or t.owner= :owner) "
            + "AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED'")
    ConstraintTemplate findByNameInAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    @Query("SELECT t FROM ConstraintTemplate t WHERE t.id= :id and t.account= :account AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED'")
    ConstraintTemplate findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    @Query("SELECT t FROM ConstraintTemplate t WHERE t.owner= :owner and t.name= :name AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED'")
    ConstraintTemplate findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    @Query("SELECT t FROM ConstraintTemplate t WHERE t.account= :account AND (t.status = 'DEFAULT_DELETED' OR t.status = 'DEFAULT') ")
    Set<ConstraintTemplate> findAllDefaultInAccount(@Param("account") String account);

}
