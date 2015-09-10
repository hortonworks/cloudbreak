package com.sequenceiq.cloudbreak.service.eventbus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.service.Persister;
import com.sequenceiq.cloudbreak.repository.EntityType;

public abstract class AbstractCloudPersisterService<T> implements Persister<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCloudPersisterService.class);

    @Inject
    private List<CrudRepository> repositoryList;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    private Map<Class, CrudRepository> repositoryMap = new HashMap<>();

    @Override
    public abstract T persist(T data);

    @Override
    public abstract T update(T data);

    @Override
    public abstract T retrieve(T data);

    @PostConstruct
    public void checkRepoMap() {
        if (CollectionUtils.isEmpty(repositoryList)) {
            throw new IllegalStateException("No repositories provided!");
        } else {
            fillRepositoryMap();
        }
    }

    protected ConversionService getConversionService() {
        return conversionService;
    }

    private void fillRepositoryMap() {
        for (CrudRepository repo : repositoryList) {
            repositoryMap.put(getEntityClassForRepository(repo), repo);
        }
    }

    private Class getEntityClassForRepository(CrudRepository repo) {
        Class<?> originalInterface = repo.getClass().getInterfaces()[0];
        EntityType annotation = originalInterface.getAnnotation(EntityType.class);
        if (annotation == null) {
            throw new IllegalStateException("Entity class is not specified for repository: " + originalInterface.getSimpleName());
        }
        return annotation.entityClass();
    }

    protected <T> T getRepositoryForEntity(Class clazz) {
        T repo = (T) repositoryMap.get(clazz);
        if (repo == null) {
            throw new IllegalStateException("No repository found for the entityClass:" + clazz);
        }
        return repo;
    }
}
