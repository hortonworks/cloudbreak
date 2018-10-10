package com.sequenceiq.cloudbreak.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.data.repository.CrudRepository;
import org.springframework.util.CollectionUtils;

public abstract class RepositoryLookupService<T extends CrudRepository<?, ?>> {

    private List<T> repositoryList;

    private final Map<Class<?>, T> repositoryMap = new HashMap<>();

    @PostConstruct
    private void checkRepoMap() {
        repositoryList = getRepositoryList();
        if (CollectionUtils.isEmpty(repositoryList)) {
            throw new IllegalStateException("No repositories provided!");
        } else {
            fillRepositoryMap();
        }
    }

    private void fillRepositoryMap() {
        for (T repo : repositoryList) {
            repositoryMap.put(getEntityClassForRepository(repo), repo);
        }
    }

    private Class<?> getEntityClassForRepository(T repo) {
        Class<?> originalInterface = repo.getClass().getInterfaces()[0];
        EntityType annotation = originalInterface.getAnnotation(EntityType.class);
        if (annotation == null) {
            throw new IllegalStateException("Entity class is not specified for repository: " + originalInterface.getSimpleName());
        }
        return annotation.entityClass();
    }

    public <R> R getRepositoryForEntity(Class<?> clazz) {
        R repo = (R) repositoryMap.get(clazz);
        if (repo == null) {
            throw new IllegalStateException("No repository found for the entityClass:" + clazz);
        }
        return repo;
    }

    protected abstract List<T> getRepositoryList();
}
