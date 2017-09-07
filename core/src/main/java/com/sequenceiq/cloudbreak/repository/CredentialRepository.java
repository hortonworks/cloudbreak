package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Topology;

@EntityType(entityClass = Credential.class)
public interface CredentialRepository extends CrudRepository<Credential, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    @Override
    Credential findOne(@Param("id") Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    @Query("SELECT b FROM Credential b WHERE b.name= :name AND b.account= :account AND b.archived IS FALSE")
    Credential findOneByName(@Param("name") String name, @Param("account") String account);

    @Query("SELECT c FROM Credential c WHERE c.owner= :user AND c.archived IS FALSE")
    Set<Credential> findForUser(@Param("user") String user);

    @Query("SELECT c FROM Credential c WHERE ((c.account= :account AND c.publicInAccount= true) OR c.owner= :user) AND c.archived IS FALSE")
    Set<Credential> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    @Query("SELECT c FROM Credential c WHERE c.account= :account AND c.archived IS FALSE")
    Set<Credential> findAllInAccount(@Param("account") String account);

    @Query("SELECT c FROM Credential c WHERE c.name= :name AND ((c.publicInAccount=true and c.account= :account) OR c.owner= :owner) AND c.archived IS FALSE")
    Credential findByNameInAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    @Query("SELECT c FROM Credential c WHERE c.id= :id AND c.account= :account AND c.archived IS FALSE")
    Credential findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    @Query("SELECT c FROM Credential c WHERE c.owner= :owner and c.name= :name AND c.archived IS FALSE")
    Credential findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    Long countByTopology(Topology topology);
}