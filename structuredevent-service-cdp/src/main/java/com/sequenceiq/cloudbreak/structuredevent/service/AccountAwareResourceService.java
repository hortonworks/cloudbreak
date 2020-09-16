package com.sequenceiq.cloudbreak.structuredevent.service;

import java.util.Set;

import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResource;

public interface AccountAwareResourceService<T extends AccountAwareResource> {

    T create(T resource, String accountId);

    T createWithMdcContextRestore(T resource, String accountId);

    T getByNameForWorkspaceId(String name, String accountId);

    Set<T> getByNamesForAccountId(Set<String> name, String accountId);

    T getByNameForAccountId(String name, String accountId);

    Set<T> findAllByAccountId(String accountId);

    T delete(T resource);

    T deleteWithMdcContextRestore(T resource);

    Set<T> delete(Set<T> resources);

    T deleteByNameFromAccountId(String name, String accountId);

    Set<T> deleteMultipleByNameFromAccountId(Set<String> names, String accountId);

    Iterable<T> findAll();

    T pureSave(T resource);

    Iterable<T> pureSaveAll(Iterable<T> resources);
}
