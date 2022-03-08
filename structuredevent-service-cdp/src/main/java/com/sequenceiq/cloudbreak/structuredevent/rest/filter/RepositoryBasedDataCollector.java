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
import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.cloudbreak.structuredevent.service.lookup.CDPAccountAwareRepositoryLookupService;

@Component
public class RepositoryBasedDataCollector {

    private final Map<String, AccountAwareResourceRepository<?, ?>> pathRepositoryMap = new HashMap<>();

    @Inject
    private ListableBeanFactory listableBeanFactory;

    @Inject
    private CDPAccountAwareRepositoryLookupService repositoryLookupService;

    /**
     * Associates Spring repositories with URL path values.
     * <p>
     * Associations are determined by looking for @{code @AccountTypeEntity} and checking the superclass for an {@code @Path} annotation.
     * <p>
     * The repository type is determined from the value of the {@code @AccountTypeEntity}.
     */
    @PostConstruct
    public void initializePathRepositoryMap() {
        Map<String, Object> accountEntityTypes = listableBeanFactory.getBeansWithAnnotation(AccountEntityType.class);
        for (Object bean : accountEntityTypes.values()) {
            // find a @Path annotation attached to a class that's also annotated with @AccountEntityType
            // Normally these are "controller" classes, which implement an interface that has the @Path annotation
            Path pathAnnotation = AnnotationUtils.findAnnotation(bean.getClass().getSuperclass(), Path.class);

            // Retrieve the @AccountEntityType associated with the bean we're iterating over
            AccountEntityType entityTypeAnnotation = AnnotationUtils.findAnnotation(bean.getClass(), AccountEntityType.class);

            if (pathAnnotation != null) {
                String pathValue = pathAnnotation.value();
                Class<?> entityClass = entityTypeAnnotation.value();
                AccountAwareResourceRepository<?, ?> repository = repositoryLookupService.getRepositoryForEntity(entityClass);
                pathRepositoryMap.put(pathValue, repository);
            }
        }
    }

    /**
     * Attaches {@code RESOURCE_ID}, {@code RESOURCE_CRN}, or {@code RESOURCE_NAME} to the provided {@code params} if they're missing.
     *
     * @param params should include exactly one of {@code RESOURCE_CRN} or {@code RESOURCE_NAME}
     */
    public void fetchDataFromDbIfNeed(Map<String, String> params) {
        String resourceCrn = params.get(RESOURCE_CRN);
        String resourceName = params.get(RESOURCE_NAME);
        // check each repository for a matching CRN or Name.
        for (Map.Entry<String, AccountAwareResourceRepository<?, ?>> pathRepositoryEntry : pathRepositoryMap.entrySet()) {
            AccountAwareResourceRepository<?, ?> resourceRepository = pathRepositoryEntry.getValue();
            if (resourceCrn == null && resourceName != null) {
                String accountId = ThreadBasedUserCrnProvider.getAccountId();
                Optional<ResourceBasicView> entity = resourceRepository.findResourceBasicViewByNameAndAccountId(resourceName, accountId);
                if (entity.isPresent()) {
                    ResourceBasicView resource = entity.get();
                    params.put(RESOURCE_ID, Long.toString(resource.getId()));
                    params.put(RESOURCE_CRN, resource.getResourceCrn());
                    break;
                }
            } else if (resourceName == null) {
                Optional<ResourceBasicView> entity = resourceRepository.findResourceBasicViewByResourceCrn(resourceCrn);
                if (entity.isPresent()) {
                    ResourceBasicView resource = entity.get();
                    params.put(RESOURCE_ID, Long.toString(resource.getId()));
                    params.put(RESOURCE_NAME, resource.getName());
                    break;
                }
            }

        }
    }
}
