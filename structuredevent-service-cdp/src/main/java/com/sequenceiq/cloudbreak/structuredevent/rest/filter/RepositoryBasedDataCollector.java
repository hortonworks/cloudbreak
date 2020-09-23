package com.sequenceiq.cloudbreak.structuredevent.rest.filter;

import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_CRN;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_ID;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_NAME;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Path;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResource;
import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.cloudbreak.structuredevent.service.lookup.CDPAccountAwareRepositoryLookupService;

@Component
public class RepositoryBasedDataCollector {

    private final Map<String, AccountAwareResourceRepository<?, ?>> pathRepositoryMap = new HashMap<>();

    @Inject
    private ListableBeanFactory listableBeanFactory;

    @Inject
    private CDPAccountAwareRepositoryLookupService repositoryLookupService;

    @PostConstruct
    public void initializePathRepositoryMap() {
        Map<String, Object> accountEntityTypes = listableBeanFactory.getBeansWithAnnotation(AccountEntityType.class);
        for (Object accountEntityType : accountEntityTypes.values()) {
            Path pathAnnotation = AnnotationUtils.findAnnotation(accountEntityType.getClass().getSuperclass(), Path.class);
            AccountEntityType entityTypeAnnotation = AnnotationUtils.findAnnotation(accountEntityType.getClass(), AccountEntityType.class);
            if (pathAnnotation != null) {
                String pathValue = pathAnnotation.value();
                Class<?> entityClass = entityTypeAnnotation.value();
                AccountAwareResourceRepository<?, ?> repository = repositoryLookupService.getRepositoryForEntity(entityClass);
                pathRepositoryMap.put(pathValue, repository);
            }
        }
    }

    public void fetchDataFromDbIfNeed(Map<String, String> params) {
        String resourceCrn = params.get(RESOURCE_CRN);
        String resourceName = params.get(RESOURCE_NAME);
        for (Map.Entry<String, AccountAwareResourceRepository<?, ?>> pathRepositoryEntry : pathRepositoryMap.entrySet()) {
            AccountAwareResourceRepository<?, ?> resourceRepository = pathRepositoryEntry.getValue();
            if (resourceCrn == null && resourceName != null) {
                String accountId = ThreadBasedUserCrnProvider.getAccountId();
                Optional<? extends AccountAwareResource> entity = resourceRepository.findByNameAndAccountId(resourceName, accountId);
                if (entity.isPresent()) {
                    AccountAwareResource resource = entity.get();
                    params.put(RESOURCE_ID, Long.toString(resource.getId()));
                    params.put(RESOURCE_CRN, resource.getResourceCrn());
                    break;
                }
            } else if (resourceName == null) {
                String accountId = ThreadBasedUserCrnProvider.getAccountId();
                Optional<? extends AccountAwareResource> entity = resourceRepository.findByResourceCrnAndAccountId(resourceCrn, accountId);
                if (entity.isPresent()) {
                    AccountAwareResource resource = entity.get();
                    params.put(RESOURCE_ID, Long.toString(resource.getId()));
                    params.put(RESOURCE_NAME, resource.getName());
                    break;
                }
            }

        }
    }
}
