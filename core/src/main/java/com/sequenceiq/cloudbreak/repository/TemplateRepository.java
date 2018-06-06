package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.Topology;

@EntityType(entityClass = Template.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface TemplateRepository extends CrudRepository<Template, Long> {

    @Override
    Template findOne(@Param("id") Long id);

    @Query("SELECT t FROM Template t WHERE t.owner= :user AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED'")
    Set<Template> findForUser(@Param("user") String user);

    @Query("SELECT t FROM Template t WHERE ((t.account= :account AND t.publicInAccount= true) OR t.owner= :user) "
            + "AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED'")
    Set<Template> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    @Query("SELECT t FROM Template t WHERE t.account= :account AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED'")
    Set<Template> findAllInAccount(@Param("account") String account);

    @Query("SELECT t FROM Template t WHERE t.name= :name and t.account= :account AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED'")
    Template findOneByName(@Param("name") String name, @Param("account") String account);

    @Query("SELECT t FROM Template t WHERE t.name= :name and ((t.account= :account and t.publicInAccount=true) or t.owner= :owner) "
            + "AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED'")
    Template findByNameInAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    @Query("SELECT t FROM Template t WHERE t.id= :id and t.account= :account AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED'")
    Template findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    @Query("SELECT t FROM Template t WHERE t.owner= :owner and t.name= :name AND deleted IS NOT TRUE AND t.status <> 'DEFAULT_DELETED'")
    Template findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    @Query("SELECT t FROM Template t WHERE t.account= :account AND (t.status = 'DEFAULT_DELETED' OR t.status = 'DEFAULT')")
    Set<Template> findAllDefaultInAccount(@Param("account") String account);

    Long countByTopology(Topology topology);
}