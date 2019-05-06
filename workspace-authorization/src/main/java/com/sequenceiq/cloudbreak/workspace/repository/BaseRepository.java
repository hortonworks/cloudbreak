package com.sequenceiq.cloudbreak.workspace.repository;

import java.io.Serializable;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByTargetId;
import com.sequenceiq.cloudbreak.workspace.resource.ResourceAction;

@NoRepositoryBean
@Transactional(TxType.REQUIRED)
public interface BaseRepository<T, ID extends Serializable> extends CrudRepository<T, ID> {

    @Override
    @HasPermission(condition = ConditionType.PRE, targetIndex = 0, permission = PermissionType.WRITE)
    @CheckPermissionsByTarget(action = ResourceAction.WRITE, targetIndex = 0)
    <S extends T> S save(S entity);

    @Override
    @DisableHasPermission
    @CheckPermissionsByTarget(action = ResourceAction.WRITE, targetIndex = 0)
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    @Override
    @HasPermission
    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    Optional<T> findById(ID id);

    @Override
    @DisableHasPermission
    @CheckPermissionsByTargetId(action = ResourceAction.READ)
    boolean existsById(ID id);

    @Override
    @HasPermission
    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    Iterable<T> findAll();

    @Override
    @DisableHasPermission
    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    Iterable<T> findAllById(Iterable<ID> ids);

    @Override
    @DisableHasPermission
    long count();

    @Override
    @CheckPermissionsByTargetId(action = ResourceAction.READ)
    @HasPermission(condition = ConditionType.PRE, targetIndex = 0, permission = PermissionType.WRITE)
    void deleteById(ID id);

    @Override
    @CheckPermissionsByTarget(action = ResourceAction.WRITE, targetIndex = 0)
    @HasPermission(condition = ConditionType.PRE, targetIndex = 0, permission = PermissionType.WRITE)
    void delete(T entity);

    @Override
    @CheckPermissionsByTarget(action = ResourceAction.WRITE, targetIndex = 0)
    @HasPermission(condition = ConditionType.PRE, targetIndex = 0, permission = PermissionType.WRITE)
    void deleteAll(Iterable<? extends T> entities);

    @Override
    @DisableHasPermission
    void deleteAll();
}
