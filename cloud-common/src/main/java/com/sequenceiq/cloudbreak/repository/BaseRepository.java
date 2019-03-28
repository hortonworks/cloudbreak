package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.aspect.ConditionType.PRE;
import static com.sequenceiq.cloudbreak.authorization.ResourceAction.READ;
import static com.sequenceiq.cloudbreak.authorization.ResourceAction.WRITE;

import java.io.Serializable;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.aspect.PermissionType;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByTargetId;

@NoRepositoryBean
@Transactional(TxType.REQUIRED)
public interface BaseRepository<T, ID extends Serializable> extends CrudRepository<T, ID> {

    @Override
    @HasPermission(condition = PRE, targetIndex = 0, permission = PermissionType.WRITE)
    @CheckPermissionsByTarget(action = WRITE, targetIndex = 0)
    <S extends T> S save(S entity);

    @Override
    @DisableHasPermission
    @CheckPermissionsByTarget(action = WRITE, targetIndex = 0)
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    @Override
    @HasPermission
    @CheckPermissionsByReturnValue(action = READ)
    Optional<T> findById(ID id);

    @Override
    @DisableHasPermission
    @CheckPermissionsByTargetId(action = READ)
    boolean existsById(ID id);

    @Override
    @HasPermission
    @CheckPermissionsByReturnValue(action = READ)
    Iterable<T> findAll();

    @Override
    @DisableHasPermission
    @CheckPermissionsByReturnValue(action = READ)
    Iterable<T> findAllById(Iterable<ID> ids);

    @Override
    @DisableHasPermission
    long count();

    @Override
    @CheckPermissionsByTargetId(action = READ)
    @HasPermission(condition = PRE, targetIndex = 0, permission = PermissionType.WRITE)
    void deleteById(ID id);

    @Override
    @CheckPermissionsByTarget(action = WRITE, targetIndex = 0)
    @HasPermission(condition = PRE, targetIndex = 0, permission = PermissionType.WRITE)
    void delete(T entity);

    @Override
    @CheckPermissionsByTarget(action = WRITE, targetIndex = 0)
    @HasPermission(condition = PRE, targetIndex = 0, permission = PermissionType.WRITE)
    void deleteAll(Iterable<? extends T> entities);

    @Override
    @DisableHasPermission
    void deleteAll();
}
