package com.sequenceiq.cloudbreak.workspace.repository;

import java.io.Serializable;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByTargetId;
import com.sequenceiq.cloudbreak.workspace.repository.check.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.workspace.resource.ResourceAction;

@NoRepositoryBean
@DisableHasPermission
public interface DisabledBaseRepository<T, ID extends Serializable> extends CrudRepository<T, ID> {

    @Override
    @CheckPermissionsByTarget(action = ResourceAction.WRITE, targetIndex = 0)
    <S extends T> S save(S entity);

    @Override
    @CheckPermissionsByTarget(action = ResourceAction.WRITE, targetIndex = 0)
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    @Override
    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    Optional<T> findById(ID id);

    @Override
    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    boolean existsById(ID id);

    @Override
    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    Iterable<T> findAll();

    @Override
    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    Iterable<T> findAllById(Iterable<ID> ids);

    @Override
    @DisableCheckPermissions
    long count();

    @Override
    @CheckPermissionsByTargetId(action = ResourceAction.READ)
    void deleteById(ID id);

    @Override
    @CheckPermissionsByTarget(action = ResourceAction.WRITE, targetIndex = 0)
    void delete(T entity);

    @Override
    @CheckPermissionsByTarget(action = ResourceAction.WRITE, targetIndex = 0)
    void deleteAll(Iterable<? extends T> entities);

    @Override
    @DisableCheckPermissions
    void deleteAll();
}
