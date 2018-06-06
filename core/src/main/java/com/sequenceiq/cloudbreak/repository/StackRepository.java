package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@EntityType(entityClass = Stack.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface StackRepository extends CrudRepository<Stack, Long> {

    @Query("SELECT s from Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.cluster.ambariIp= :ambariIp AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Stack findByAmbari(@Param("ambariIp") String ambariIp);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE s.owner= :user AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Set<Stack> findForUserWithLists(@Param("user") String user);

    @Query("SELECT s FROM Stack s WHERE s.owner= :user AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Set<Stack> findForUser(@Param("user") String user);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "LEFT JOIN FETCH s.cluster c LEFT JOIN FETCH c.hostGroups WHERE ((s.account= :account AND s.publicInAccount= true) OR s.owner= :user) "
            + "AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Set<Stack> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.resources LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "LEFT JOIN FETCH s.cluster c LEFT JOIN FETCH c.hostGroups WHERE s.account= :account AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Set<Stack> findAllInAccountWithLists(@Param("account") String account);

    @Query("SELECT s FROM Stack s WHERE s.account= :account AND s.stackStatus.status <> 'DELETE_COMPLETED'")
    Set<Stack> findAllInAccount(@Param("account") String account);

    @Query("SELECT c FROM Stack c LEFT JOIN FETCH c.resources LEFT JOIN FETCH c.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData WHERE c.id= :id")
    Stack findOneWithLists(@Param("id") Long id);

    @Query("SELECT s FROM Stack s "
            + "WHERE s.datalakeId= :id AND s.stackStatus.status <> 'DELETE_COMPLETED' AND s.stackStatus.status <> 'DELETE_IN_PROGRESS'"
            + "AND s.stackStatus.status <> 'REQUESTED'")
    Set<Stack> findEphemeralClusters(@Param("id") Long id);

    @Query("SELECT distinct c FROM Stack c LEFT JOIN FETCH c.instanceGroups ig WHERE ig.template.id= :id")
    List<Stack> findAllStackForTemplate(@Param("id") Long id);

    @Query("SELECT s.id,s.stackStatus.status FROM Stack s WHERE s.id IN (:ids)")
    List<Object[]> findStackStatuses(@Param("ids") Set<Long> ids);

    @Query("SELECT c FROM Stack c WHERE c.cluster.id= :id")
    Stack findStackForCluster(@Param("id") Long id);

    @Query("SELECT s FROM Stack s WHERE s.id= :id and s.account= :account")
    Stack findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    @Query("SELECT s FROM Stack s WHERE s.name= :name and ((s.account= :account and s.publicInAccount=true) or s.owner= :owner)")
    Stack findByNameInAccountOrOwner(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    @Query("SELECT c FROM Stack c LEFT JOIN FETCH c.resources LEFT JOIN FETCH c.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
        + "WHERE c.name= :name and c.account= :account")
    Stack findByNameInAccountWithLists(@Param("name") String name, @Param("account") String account);

    @Query("SELECT c FROM Stack c WHERE c.name= :name and c.account= :account")
    Stack findByNameInAccount(@Param("name") String name, @Param("account") String account);

    @Query("SELECT t FROM Stack t LEFT JOIN FETCH t.resources LEFT JOIN FETCH t.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "WHERE t.owner= :owner and t.name= :name")
    Stack findByNameInUserWithLists(@Param("name") String name, @Param("owner") String owner);

    @Query("SELECT t FROM Stack t WHERE t.owner= :owner and t.name= :name")
    Stack findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status <> 'DELETE_COMPLETED'")
    List<Stack> findAllAlive();

    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status <> 'DELETE_COMPLETED' AND s.stackStatus.status <> 'REQUESTED' "
            + "AND s.stackStatus.status <> 'CREATE_IN_PROGRESS'")
    List<Stack> findAllAliveAndProvisioned();

    @Query("SELECT s FROM Stack s WHERE s.stackStatus.status IN :statuses")
    List<Stack> findByStatuses(@Param("statuses") List<Status> statuses);

    @Query("SELECT s FROM Stack s LEFT JOIN FETCH s.cluster LEFT JOIN FETCH s.credential LEFT JOIN FETCH s.network LEFT JOIN FETCH s.orchestrator "
            + "LEFT JOIN FETCH s.stackStatus LEFT JOIN FETCH s.securityConfig LEFT JOIN FETCH s.failurePolicy WHERE s.stackStatus.status <> 'DELETE_COMPLETED' "
            + "AND s.stackStatus.status <> 'DELETE_IN_PROGRESS'")
    Set<Stack> findAliveOnes();

    Long countByFlexSubscription(FlexSubscription flexSubscription);

    Long countByCredential(Credential credential);

    Long countByNetwork(Network network);
}
