package com.sequenceiq.cloudbreak.workspace.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.data.repository.CrudRepository;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

public abstract class RepositoryLookupService<T extends CrudRepository<?, ?>> {

    private List<T> repositoryList;

    private final Map<Class<?>, T> repositoryMap = new HashMap<>();

    @PostConstruct
    private void initRepositoryMap() {
        repositoryList = getRepositoryList();
        if (!CollectionUtils.isEmpty(repositoryList)) {
            fillRepositoryMap();
        }
    }

    private void fillRepositoryMap() {
        for (T repo : repositoryList) {
            Arrays.stream(repo.getClass().getInterfaces())
                    .filter(clazz -> clazz.getName().contains("com.sequenceiq.cloudbreak"))
                    .findFirst().ifPresent(clazz -> {
                Class<?> entityClassForRepository = getEntityClassForRepository(clazz);
                repositoryMap.put(entityClassForRepository, repo);
            });
        }
    }

    private Class<?> getEntityClassForRepository(Class<?> originalInterface) {
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
