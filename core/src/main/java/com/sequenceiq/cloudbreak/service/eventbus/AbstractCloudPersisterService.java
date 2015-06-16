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
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

public abstract class AbstractCloudPersisterService<T> implements Persister<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCloudPersisterService.class);

    @Inject
    private List<CrudRepository> repositoryList;

    private Map<Class, CrudRepository> repositoryMap = new HashMap<>();

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;


    @Override
    public abstract T persist(T data);

    protected ConversionService getConversionService() {
        return conversionService;
    }

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

    private void fillRepositoryMap() {
        for (CrudRepository repo : repositoryList) {
            Class entityClass = getEntityClassForRepository(repo);
            if (null != entityClass) {
                repositoryMap.put(entityClass, repo);
            }
        }
    }

    private Class getEntityClassForRepository(CrudRepository repo) {
        Class clazz = null;
        try {
            // force an exception that contains the domain object name
            repo.delete(Long.MIN_VALUE);
        } catch (Exception e) {
            String clazzStr = getDomainClassNameFromException(e);
            try {
                clazz = Class.forName(clazzStr);
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Invalid domain class: " + clazzStr);
            }
        }
        return clazz;
    }

    private String getDomainClassNameFromException(Exception e) {
        LOGGER.debug("Exception message: {}", e.getMessage());
        String classStr = null;

        for (String word : e.getMessage().split(" ")) {
            if (word.startsWith("com.sequenceiq")) {
                classStr = word.endsWith(".") ? word.replaceAll(".$", "") : word;
                break;
            }
        }
        return classStr;
    }

    protected CrudRepository getRepositoryForEntity(ProvisionEntity entity) {
        CrudRepository repo = null;
        if (repositoryMap.containsKey(entity.getClass())) {
            repo = repositoryMap.get(entity.getClass());
        } else {
            throw new IllegalStateException("No repository found for the entityClass:" + entity.getClass());
        }
        return repo;
    }
}
