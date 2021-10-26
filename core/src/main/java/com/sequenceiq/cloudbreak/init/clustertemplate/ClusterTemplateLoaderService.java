package com.sequenceiq.cloudbreak.init.clustertemplate;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Service
public class ClusterTemplateLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateLoaderService.class);

    @Inject
    private DefaultClusterTemplateCache defaultClusterTemplateCache;

    @Inject
    private ClusterTemplateService clusterTemplateService;

    @Inject
    private BlueprintService blueprintService;

    public boolean isDefaultClusterTemplateUpdateNecessaryForUser(Collection<ClusterTemplate> clusterTemplates) {
        Map<String, String> defaultTemplates = defaultClusterTemplateCache.defaultClusterTemplateRequests();
        List<ClusterTemplate> defaultTemplatesInDb = filterTemplatesForDefaults(clusterTemplates);
        LOGGER.info("Merge default cluster definition in db ({}) with cached templates {}", defaultTemplatesInDb.size(), defaultTemplates.size());
        if (defaultTemplatesInDb.size() < defaultTemplates.size()) {
            LOGGER.debug("Default cluster definitions in DB [{}] less than default cluster definitions size [{}]", defaultTemplatesInDb.size(),
                    defaultTemplates.size());
            return true;
        }
        if (!isAllDefaultTemplateExistsInDbByName(defaultTemplates, defaultTemplatesInDb)) {
            return true;
        }
        if (!isEveryDefaultTemplateInDbHasCrn(defaultTemplatesInDb)) {
            return true;
        }
        return isAllDefaultTemplateInDbHasTheSameContentAsCached(defaultTemplates, defaultTemplatesInDb);
    }

    private boolean isEveryDefaultTemplateInDbHasCrn(List<ClusterTemplate> defaultTemplatesInDb) {
        return defaultTemplatesInDb.stream()
                .allMatch(template -> template.getResourceCrn() != null);
    }

    private List<ClusterTemplate> filterTemplatesForDefaults(Collection<ClusterTemplate> clusterTemplates) {
        return clusterTemplates.stream().filter(template -> ResourceStatus.DEFAULT == template.getStatus())
                .collect(Collectors.toList());
    }

    private boolean isAllDefaultTemplateInDbHasTheSameContentAsCached(Map<String, String> defaultTemplates,
            Collection<ClusterTemplate> defaultTemplatesInDb) {
        return defaultTemplatesInDb.stream().anyMatch(clusterTemplate -> {
            String defaultTemplateBase64 = defaultTemplates.get(clusterTemplate.getName());
            return isTemplatesContentDifferent(clusterTemplate, defaultTemplateBase64);
        });
    }

    private boolean isTemplatesContentDifferent(ClusterTemplate clusterTemplate, String defaultTemplateBase64) {
        return !defaultTemplateBase64.equals(clusterTemplate.getTemplateContent());
    }

    private boolean isAllDefaultTemplateExistsInDbByName(Map<String, String> defaultTemplates,
            Collection<ClusterTemplate> defaultTemplatesInDb) {
        return defaultTemplatesInDb.stream().allMatch(s -> defaultTemplates.keySet().contains(s.getName()));
    }

    public Set<ClusterTemplate> loadClusterTemplatesForWorkspace(Set<ClusterTemplate> templatesInDb, Workspace workspace,
            Function<Iterable<ClusterTemplate>, Collection<ClusterTemplate>> saveMethod) {
        Set<ClusterTemplate> clusterTemplatesToSave = collectClusterTemplatesToSaveInDb(templatesInDb, workspace);
        LOGGER.debug("{} cluster definitions in the db and {} cluster definitions want to save", templatesInDb.size(), clusterTemplatesToSave.size());
        decorateWithCrn(clusterTemplatesToSave);
        Iterable<ClusterTemplate> savedClusterTemplates = measure(() -> saveMethod.apply(clusterTemplatesToSave), LOGGER,
                "saved in {} ms {} cluster definitions", clusterTemplatesToSave.size());
        return unifyTemplatesUpdatedAndUnmodified(templatesInDb, clusterTemplatesToSave, savedClusterTemplates);
    }

    private void decorateWithCrn(Set<ClusterTemplate> clusterTemplate) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        clusterTemplate.stream()
                .filter(template -> !hasCrn(template))
                .forEach(template -> {
                    template.setResourceCrn(clusterTemplateService.createCRN(accountId));
                });
    }

    private Set<ClusterTemplate> unifyTemplatesUpdatedAndUnmodified(Set<ClusterTemplate> templatesInDb, Set<ClusterTemplate> clusterTemplatesToSave,
            Iterable<ClusterTemplate> savedClusterTemplates) {
        Set<String> savedTemplatesName = clusterTemplatesToSave.stream().map(ClusterTemplate::getName).collect(Collectors.toSet());
        Set<ClusterTemplate> templatesNotModified = templatesInDb.stream()
                .filter(template -> !savedTemplatesName.contains(template.getName()))
                .collect(Collectors.toSet());
        Set<ClusterTemplate> unionOfTemplates = Sets.newHashSet(savedClusterTemplates);
        unionOfTemplates.addAll(templatesNotModified);
        return unionOfTemplates;
    }

    private Set<ClusterTemplate> collectClusterTemplatesToSaveInDb(Set<ClusterTemplate> templatesInDb, Workspace workspace) {
        Collection<String> defaultTemplateNames = measure(() -> defaultClusterTemplateCache.defaultClusterTemplateNames(), LOGGER,
                "Default cluster definitions fetched in {}ms");
        List<ClusterTemplate> defaultTemplatesInDb = filterTemplatesForDefaults(templatesInDb);
        Collection<String> templateNamesMissingFromDb = collectTemplatesMissingFromDb(defaultTemplateNames, defaultTemplatesInDb);
        Collection<ClusterTemplate> updatedTemplates = collectOutdatedTemplatesInDb(defaultTemplatesInDb);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Cluster definitions missing from DB: {}", templateNamesMissingFromDb);
        }
        Set<Blueprint> blueprints = blueprintService.getAllAvailableInWorkspaceWithoutUpdate(workspace);
        Collection<ClusterTemplate> templatesMissingFromDb = measure(() ->
                        defaultClusterTemplateCache.defaultClusterTemplatesByNames(templateNamesMissingFromDb, blueprints), LOGGER,
                "Missed cluster definitions fetched in {}ms");
        return Stream.concat(templatesMissingFromDb.stream(), updatedTemplates.stream()).collect(Collectors.toSet());
    }

    public Collection<String> collectTemplatesMissingFromDb(Collection<String> defaultTemplateNames, Collection<ClusterTemplate> defaultTemplatesInDb) {
        Set<String> defaultTemplateInDbNames = defaultTemplatesInDb.stream().map(ClusterTemplate::getName).collect(Collectors.toSet());
        return defaultTemplateNames.stream()
                .filter(defaultTemplateName -> !defaultTemplateInDbNames.contains(defaultTemplateName))
                .collect(Collectors.toSet());
    }

    public Collection<ClusterTemplate> collectOutdatedTemplatesInDb(Collection<ClusterTemplate> clusterTemplates) {
        return collectOutdatedTemplatesInDb(defaultClusterTemplateCache.defaultClusterTemplateRequests(), clusterTemplates);
    }

    public Collection<ClusterTemplate> collectOutdatedTemplatesInDb(Map<String, String> defaultTemplates, Collection<ClusterTemplate> defaultTemplatesInDb) {
        Collection<ClusterTemplate> outdatedTemplates = new HashSet<>();
        for (ClusterTemplate templateInDB : filterTemplatesForDefaults(defaultTemplatesInDb)) {
            Optional<String> defaultTemplate = Optional.ofNullable(defaultTemplates.get(templateInDB.getName()));
            if (defaultTemplate.isPresent()) {
                defaultTemplate
                        .filter(defaultTmplBase64 -> isTemplatesContentDifferent(templateInDB, defaultTmplBase64) || !hasCrn(templateInDB))
                        .ifPresent(defaultTmplBase64 -> {
                            templateInDB.setStatus(ResourceStatus.OUTDATED);
                            outdatedTemplates.add(templateInDB);
                        });
            } else {
                templateInDB.setStatus(ResourceStatus.OUTDATED);
                outdatedTemplates.add(templateInDB);
            }
        }
        return outdatedTemplates;
    }

    private boolean hasCrn(ClusterTemplate template) {
        return template.getResourceCrn() != null;
    }
}
