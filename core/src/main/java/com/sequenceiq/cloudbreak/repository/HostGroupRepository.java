package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.domain.projection.HostGroupRepairView;

@EntityType(entityClass = HostGroup.class)
@Transactional(TxType.REQUIRED)
public interface HostGroupRepository extends CrudRepository<HostGroup, Long> {

    @EntityGraph(value = "HostGroup.instanceGroup.instanceMetaData", type = EntityGraphType.LOAD)
    @Query("SELECT h FROM HostGroup h WHERE h.cluster.id= :clusterId")
    Set<HostGroup> findHostGroupsInCluster(@Param("clusterId") Long clusterId);

    @EntityGraph(value = "HostGroup.instanceGroup.instanceMetaData", type = EntityGraphType.LOAD)
    @Query("SELECT h FROM HostGroup h WHERE h.cluster.id= :clusterId AND h.name= :hostGroupName")
    Optional<HostGroup> findHostGroupInClusterByNameWithInstanceMetadas(@Param("clusterId") Long clusterId, @Param("hostGroupName") String hostGroupName);

    @Query("SELECT COUNT(h) > 0 FROM HostGroup h WHERE h.cluster.id= :clusterId AND h.name= :hostGroupName")
    boolean hasHostGroupInCluster(@Param("clusterId") Long clusterId, @Param("hostGroupName") String hostGroupName);

    @Query("SELECT h.recoveryMode FROM HostGroup h WHERE h.cluster.id= :clusterId AND h.name= :hostGroupName")
    RecoveryMode getRecoveryMode(@Param("clusterId") Long clusterId, @Param("hostGroupName") String hostGroupName);

    @Query("SELECT h.name as name, h.recoveryMode as recoveryMode from HostGroup h "
            + "WHERE h.cluster.id= :clusterId AND h.name= :hostGroupName")
    Optional<HostGroupRepairView> findHostGroupRepairViewInClusterByName(@Param("clusterId") Long clusterId, @Param("hostGroupName") String hostGroupName);

    @EntityGraph(value = "HostGroup.instanceGroup.instanceMetaData", type = EntityGraphType.LOAD)
    @Query("SELECT h FROM HostGroup h JOIN h.recipes r WHERE r.id= :recipeId")
    Set<HostGroup> findAllHostGroupsByRecipe(@Param("recipeId") Long recipeId);

    @Query("SELECT h FROM HostGroup h LEFT JOIN FETCH h.recipes LEFT JOIN FETCH h.generatedRecipes WHERE h.cluster.id= :clusterId")
    Set<HostGroup> findHostGroupsInClusterWithRecipes(@Param("clusterId") Long clusterId);

    @EntityGraph(value = "HostGroup.instanceGroup.instanceMetaData", type = EntityGraphType.LOAD)
    @Query("SELECT h FROM HostGroup h LEFT JOIN FETCH h.recipes LEFT JOIN FETCH h.generatedRecipes WHERE h.cluster.id= :clusterId AND h.name= :hostGroupName")
    HostGroup findHostGroupInClusterByNameWithRecipes(@Param("clusterId") Long clusterId, @Param("hostGroupName") String hostGroupName);

    @Query("SELECT r FROM HostGroup h " +
            "JOIN h.recipes r " +
            "WHERE h.cluster.id= :clusterId " +
            "AND h.name= :hostGroupName")
    List<Recipe> findRecipesForHostGroup(@Param("clusterId") Long clusterId, @Param("hostGroupName") String hostGroupName);

    @Query("SELECT h.recoveryMode FROM HostGroup h WHERE h.cluster.id= :clusterId AND h.name= :hostGroupName")
    Optional<RecoveryMode> findRecoveryModeForHostGroup(@Param("clusterId") Long clusterId, @Param("hostGroupName") String hostGroupName);
}
