package com.sequenceiq.cloudbreak.common.service.account;

import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotEmpty;

import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;

public interface AccountAwareResourceService<T extends AccountAwareResource> {

    T create(@Nonnull T resource, @NotEmpty String accountId);

    T createWithMdcContextRestore(@Nonnull T resource, @NotEmpty String accountId);

    T getByNameForWorkspaceId(@NotEmpty String name, @NotEmpty String accountId);

    Set<T> getByNamesForAccountId(Set<String> name, @NotEmpty String accountId);

    T getByNameForAccountId(@NotEmpty String name, @NotEmpty String accountId);

    Set<T> findAllByAccountId(@NotEmpty String accountId);

    T delete(@Nonnull T resource);

    T deleteWithMdcContextRestore(@Nonnull T resource);

    Set<T> delete(Set<T> resources);

    T deleteByNameFromAccountId(@NotEmpty String name, @NotEmpty String accountId);

    Set<T> deleteMultipleByNameFromAccountId(Set<String> names, @NotEmpty String accountId);

    Iterable<T> findAll();

    T pureSave(T resource);

    Iterable<T> pureSaveAll(Iterable<T> resources);
}
