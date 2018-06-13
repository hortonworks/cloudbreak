package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.RDSConfig;

@EntityType(entityClass = RDSConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface RdsConfigRepository extends CrudRepository<RDSConfig, Long> {

    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.owner= :user AND r.status = 'USER_MANAGED'")
    Set<RDSConfig> findForUser(@Param("user") String user);

    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE ((r.account= :account AND r.publicInAccount= true) OR r.owner= :user) "
            + "AND r.status = 'USER_MANAGED' ")
    Set<RDSConfig> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.account= :account AND r.status = 'USER_MANAGED'")
    Set<RDSConfig> findAllBasedOnAccount(@Param("account") String account);

    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.owner= :owner and r.name= :name AND r.status = 'USER_MANAGED'")
    RDSConfig findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.name= :name and r.account= :account AND r.status = 'USER_MANAGED'")
    RDSConfig findOneByName(@Param("name") String name, @Param("account") String account);

    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE  r.id= :id and r.account= :account AND r.status <> 'DEFAULT_DELETED'")
    RDSConfig findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE  r.name= :name "
            + "and ((r.publicInAccount=true and r.account= :account) or r.owner= :owner) AND r.status = 'USER_MANAGED'")
    RDSConfig findByNameBasedOnAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.id= :id AND r.status <> 'DEFAULT_DELETED'")
    RDSConfig findById(@Param("id") Long id);

    @Query("SELECT r FROM RDSConfig r INNER JOIN r.clusters cluster LEFT JOIN FETCH r.clusters WHERE cluster.id= :clusterId "
            + "AND ((r.account= :account AND r.publicInAccount= true) OR r.owner= :user)")
    Set<RDSConfig> findByClusterId(@Param("user") String user, @Param("account") String account, @Param("clusterId") Long clusterId);

    @Query("SELECT r FROM RDSConfig r INNER JOIN r.clusters cluster LEFT JOIN FETCH r.clusters WHERE cluster.id= :clusterId "
            + "AND ((r.account= :account AND r.publicInAccount= true) OR r.owner= :user) AND r.status = 'USER_MANAGED'")
    Set<RDSConfig> findUserManagedByClusterId(@Param("user") String user, @Param("account") String account, @Param("clusterId") Long clusterId);

    @Query("SELECT r FROM RDSConfig r INNER JOIN r.clusters cluster WHERE cluster.id= :clusterId "
            + "AND ((r.publicInAccount=true and r.account= :account) or r.owner= :user) AND r.status <> 'DEFAULT_DELETED' AND r.type= :type")
    RDSConfig findByClusterIdAndType(@Param("user") String user, @Param("account") String account, @Param("clusterId") Long clusterId,
            @Param("type") String type);
}
