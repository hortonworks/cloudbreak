package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Blueprint;

@EntityType(entityClass = Blueprint.class)
public interface BlueprintRepository extends CrudRepository<Blueprint, Long> {

    @Override
    Blueprint findOne(@Param("id") Long id);

    @Query("SELECT b FROM Blueprint b WHERE b.name= :name and b.account= :account AND b.status <> 'DEFAULT_DELETED'")
    Blueprint findOneByName(@Param("name") String name, @Param("account") String account);

    @Query("SELECT b FROM Blueprint b WHERE b.owner= :user AND b.status <> 'DEFAULT_DELETED'")
    Set<Blueprint> findForUser(@Param("user") String user);

    @Query("SELECT b FROM Blueprint b WHERE ((b.account= :account AND b.publicInAccount= true) OR b.owner= :user) AND b.status <> 'DEFAULT_DELETED'")
    Set<Blueprint> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    @Query("SELECT b FROM Blueprint b WHERE b.account= :account AND b.status <> 'DEFAULT_DELETED'")
    Set<Blueprint> findAllInAccount(@Param("account") String account);

    @Query("SELECT b FROM Blueprint b WHERE  b.name= :name and ((b.publicInAccount=true and b.account= :account) or b.owner= :owner) "
            + "AND b.status <> 'DEFAULT_DELETED'")
    Blueprint findByNameInAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    @Query("SELECT b FROM Blueprint b WHERE  b.id= :id and b.account= :account AND b.status <> 'DEFAULT_DELETED'")
    Blueprint findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    @Query("SELECT b FROM Blueprint b WHERE b.owner= :owner and b.name= :name AND b.status <> 'DEFAULT_DELETED'")
    Blueprint findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    @Query("SELECT b FROM Blueprint b WHERE b.account= :account AND (b.status = 'DEFAULT_DELETED' OR b.status = 'DEFAULT') ")
    Set<Blueprint> findAllDefaultInAccount(@Param("account") String account);
}