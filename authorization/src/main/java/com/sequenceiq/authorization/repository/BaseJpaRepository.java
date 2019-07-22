package com.sequenceiq.authorization.repository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.authorization.resource.ResourceAction;

@NoRepositoryBean
@Transactional(Transactional.TxType.REQUIRED)
public interface BaseJpaRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

    @Override
    @CheckPermission(action = ResourceAction.READ)
    List<T> findAll();

    @Override
    @CheckPermission(action = ResourceAction.READ)
    List<T> findAll(Sort sort);

    @Override
    @CheckPermission(action = ResourceAction.READ)
    List<T> findAllById(Iterable<ID> ids);

    @Override
    @CheckPermission(action = ResourceAction.WRITE)
    <S extends T> List<S> saveAll(Iterable<S> entities);

    @Override
    @CheckPermission(action = ResourceAction.WRITE)
    <S extends T> S saveAndFlush(S entity);

    @Override
    @CheckPermission(action = ResourceAction.WRITE)
    void deleteInBatch(Iterable<T> entities);

    @Override
    @CheckPermission(action = ResourceAction.WRITE)
    void deleteAllInBatch();

    @Override
    @CheckPermission(action = ResourceAction.READ)
    T getOne(ID id);

    @Override
    @CheckPermission(action = ResourceAction.READ)
    <S extends T> List<S> findAll(Example<S> example);

    @Override
    @CheckPermission(action = ResourceAction.READ)
    <S extends T> List<S> findAll(Example<S> example, Sort sort);

    @Override
    @CheckPermission(action = ResourceAction.READ)
    Page<T> findAll(Pageable pageable);

    @Override
    @CheckPermission(action = ResourceAction.WRITE)
    <S extends T> S save(S entity);

    @Override
    @CheckPermission(action = ResourceAction.READ)
    Optional<T> findById(ID id);

    @Override
    @CheckPermission(action = ResourceAction.READ)
    boolean existsById(ID id);

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

    @Override
    @CheckPermission(action = ResourceAction.READ)
    <S extends T> Optional<S> findOne(Example<S> example);

    @Override
    @CheckPermission(action = ResourceAction.READ)
    <S extends T> Page<S> findAll(Example<S> example, Pageable pageable);

    @Override
    @CheckPermission(action = ResourceAction.READ)
    <S extends T> long count(Example<S> example);

    @Override
    @CheckPermission(action = ResourceAction.READ)
    <S extends T> boolean exists(Example<S> example);
}
