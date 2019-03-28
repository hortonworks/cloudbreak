package com.sequenceiq.cloudbreak.aspect;

import static com.sequenceiq.cloudbreak.authorization.ResourceAction.READ;
import static com.sequenceiq.cloudbreak.authorization.ResourceAction.WRITE;

import java.io.Serializable;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByTargetId;
import com.sequenceiq.cloudbreak.aspect.workspace.DisableCheckPermissions;

@NoRepositoryBean
@DisableHasPermission
public interface DisabledBaseRepository<T, ID extends Serializable> extends CrudRepository<T, ID> {

    @Override
    @CheckPermissionsByTarget(action = WRITE, targetIndex = 0)
    <S extends T> S save(S entity);

    @Override
    @CheckPermissionsByTarget(action = WRITE, targetIndex = 0)
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    @Override
    @CheckPermissionsByReturnValue(action = READ)
    Optional<T> findById(ID id);

    @Override
    @CheckPermissionsByReturnValue(action = READ)
    boolean existsById(ID id);

    @Override
    @CheckPermissionsByReturnValue(action = READ)
    Iterable<T> findAll();

    @Override
    @CheckPermissionsByReturnValue(action = READ)
    Iterable<T> findAllById(Iterable<ID> ids);

    @Override
    @DisableCheckPermissions
    long count();

    @Override
    @CheckPermissionsByTargetId(action = READ)
    void deleteById(ID id);

    @Override
    @CheckPermissionsByTarget(action = WRITE, targetIndex = 0)
    void delete(T entity);

    @Override
    @CheckPermissionsByTarget(action = WRITE, targetIndex = 0)
    void deleteAll(Iterable<? extends T> entities);

    @Override
    @DisableCheckPermissions
    void deleteAll();
}
