package com.sequenceiq.authorization.repository;

import java.io.Serializable;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.authorization.resource.ResourceAction;

@NoRepositoryBean
@Transactional(TxType.REQUIRED)
public interface BaseCrudRepository<T, ID extends Serializable> extends CrudRepository<T, ID> {

    @Override
    @CheckPermission(action = ResourceAction.WRITE)
    <S extends T> S save(S entity);

    @Override
    @CheckPermission(action = ResourceAction.WRITE)
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    @Override
    @CheckPermission(action = ResourceAction.READ)
    Optional<T> findById(ID id);

    @Override
    @CheckPermission(action = ResourceAction.READ)
    boolean existsById(ID id);

    @Override
    @CheckPermission(action = ResourceAction.READ)
    Iterable<T> findAll();

    @Override
    @CheckPermission(action = ResourceAction.READ)
    Iterable<T> findAllById(Iterable<ID> ids);

    @Override
    @CheckPermission(action = ResourceAction.READ)
    long count();

    @Override
    @CheckPermission(action = ResourceAction.WRITE)
    void deleteById(ID id);

    @Override
    @CheckPermission(action = ResourceAction.WRITE)
    void delete(T entity);

    @Override
    @CheckPermission(action = ResourceAction.WRITE)
    void deleteAll(Iterable<? extends T> entities);

    @Override
    @CheckPermission(action = ResourceAction.WRITE)
    void deleteAll();
}
