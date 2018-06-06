package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;

@EntityType(entityClass = Cluster.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ClusterRepository extends CrudRepository<Cluster, Long> {

    Cluster findById(@Param("id") Long id);

    Cluster findOneByStackId(long stackId);

    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.hostGroups LEFT JOIN FETCH c.containers LEFT JOIN FETCH c.components "
            + "LEFT JOIN FETCH c.rdsConfigs WHERE c.id= :id")
    Cluster findOneWithLists(@Param("id") Long id);

    @Query("SELECT c FROM Cluster c WHERE c.status IN :statuses")
    List<Cluster> findByStatuses(@Param("statuses") Collection<Status> statuses);

    @Query("SELECT c FROM Cluster c WHERE c.name= :name and c.account= :account")
    Cluster findByNameInAccount(@Param("name") String name, @Param("account") String account);

    @Query("SELECT c FROM Cluster c inner join c.hostGroups hg WHERE hg.constraint.constraintTemplate.id = :id")
    List<Cluster> findAllClustersForConstraintTemplate(@Param("id") Long id);

    @Query("SELECT c FROM Cluster c inner join c.rdsConfigs rc WHERE rc.id= :id")
    Set<Cluster> findAllClustersByRDSConfig(@Param("id") Long rdsConfigId);

    Long countByBlueprint(Blueprint blueprint);

    Long countByLdapConfig(LdapConfig ldapConfig);

    Long countByProxyConfig(ProxyConfig proxyConfig);
}