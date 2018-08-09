package com.sequenceiq.cloudbreak.aspect;

import java.io.Serializable;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
@DisableHasPermission
public interface DisabledBaseRepository<T, ID extends Serializable> extends CrudRepository<T, ID> {

    @Override
    <S extends T> S save(S entity);

    @Override
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    @Override
    Optional<T> findById(ID id);

    @Override
    boolean existsById(ID id);

    @Override
    Iterable<T> findAll();

    @Override
    Iterable<T> findAllById(Iterable<ID> ids);

    @Override
    long count();

    @Override
    void deleteById(ID id);

    @Override
    void delete(T entity);

    @Override
    void deleteAll(Iterable<? extends T> entities);

    @Override
    void deleteAll();
}
