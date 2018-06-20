package com.sequenceiq.cloudbreak.aspect;

import java.io.Serializable;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<T, ID extends Serializable> extends CrudRepository<T, ID> {

    @Override
    @HasPermission(condition = ConditionType.PRE, targetIndex = 0, permission = PermissionType.WRITE)
    <S extends T> S save(S entity);

    @Override
    @DisablePermission
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    @Override
    @HasPermission
    Optional<T> findById(ID id);

    @Override
    @HasPermission(condition = ConditionType.PRE)
    boolean existsById(ID id);

    @Override
    @HasPermission
    Iterable<T> findAll();

    @Override
    @DisablePermission
    Iterable<T> findAllById(Iterable<ID> ids);

    @Override
    @DisablePermission
    long count();

    @Override
    @HasPermission(condition = ConditionType.PRE, targetIndex = 0, permission = PermissionType.WRITE)
    void deleteById(ID id);

    @Override
    @HasPermission(condition = ConditionType.PRE, targetIndex = 0, permission = PermissionType.WRITE)
    void delete(T entity);

    @Override
    @HasPermission(condition = ConditionType.PRE, targetIndex = 0, permission = PermissionType.WRITE)
    void deleteAll(Iterable<? extends T> entities);

    @Override
    @DisablePermission
    void deleteAll();
}
