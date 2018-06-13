package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.ClusterTemplate;

@EntityType(entityClass = ClusterTemplate.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ClusterTemplateRepository extends CrudRepository<ClusterTemplate, Long> {

    @Override
    ClusterTemplate findOne(@Param("id") Long id);

    @Query("SELECT b FROM ClusterTemplate b WHERE b.name= :name and b.account= :account")
    ClusterTemplate findOneByName(@Param("name") String name, @Param("account") String account);

    @Query("SELECT b FROM ClusterTemplate b WHERE b.owner= :user ")
    Set<ClusterTemplate> findForUser(@Param("user") String user);

    @Query("SELECT b FROM ClusterTemplate b WHERE ((b.account= :account AND b.publicInAccount= true) OR b.owner= :user)")
    Set<ClusterTemplate> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    @Query("SELECT b FROM ClusterTemplate b WHERE b.account= :account")
    Set<ClusterTemplate> findAllInAccount(@Param("account") String account);

    @Query("SELECT b FROM ClusterTemplate b WHERE  b.name= :name and ((b.publicInAccount=true and b.account= :account) or b.owner= :owner) ")
    ClusterTemplate findByNameInAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    @Query("SELECT b FROM ClusterTemplate b WHERE  b.id= :id and b.account= :account")
    ClusterTemplate findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    @Query("SELECT b FROM ClusterTemplate b WHERE b.owner= :owner and b.name= :name")
    ClusterTemplate findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    @Query("SELECT b FROM ClusterTemplate b WHERE b.account= :account")
    Set<ClusterTemplate> findAllDefaultInAccount(@Param("account") String account);

}