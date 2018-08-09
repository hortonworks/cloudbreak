package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.aspect.ConditionType.PRE;
import static com.sequenceiq.cloudbreak.validation.OrganizationPermissions.Action.READ;
import static com.sequenceiq.cloudbreak.validation.OrganizationPermissions.Action.WRITE;

import java.io.Serializable;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.aspect.PermissionType;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsInPostPhase;

@NoRepositoryBean
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
    Optional<T> findById(ID id);

    @Override
    @HasPermission(condition = PRE)
    boolean existsById(ID id);

    @Override
    @HasPermission
    @CheckPermissionsInPostPhase(action = READ)
    Iterable<T> findAll();

    @Override
    @DisableHasPermission
    @CheckPermissionsInPostPhase(action = READ)
    Iterable<T> findAllById(Iterable<ID> ids);

    @Override
    @DisableHasPermission
    long count();

    @Override
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
