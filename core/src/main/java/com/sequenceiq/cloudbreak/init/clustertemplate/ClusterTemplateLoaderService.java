package com.sequenceiq.cloudbreak.init.clustertemplate;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.VaultService;

@Service
public class ClusterTemplateLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateLoaderService.class);

    @Inject
    private DefaultClusterTemplateCache defaultClusterTemplateCache;

    @Inject
    private VaultService vaultService;

    public boolean isDefaultClusterTemplateUpdateNecessaryForUser(Collection<ClusterTemplate> clusterTemplates) {
        Map<String, ClusterTemplate> defaultTemplates = defaultClusterTemplateCache.defaultClusterTemplates();
        List<ClusterTemplate> defaultTemplatesInDb = filterTemplatesForDefaults(clusterTemplates);
        if (defaultTemplatesInDb.size() < defaultTemplates.size()) {
            LOGGER.debug("Default templates in DB [{}] less than default templates size [{}]", defaultTemplatesInDb.size(), defaultTemplates.size());
            return true;
        }
        if (!isAllDefaultTemplateExistsInDbByName(defaultTemplates, defaultTemplatesInDb)) {
            return true;
        }
        return isAllDefaultTemplateInDbHasTheSameContentAsCached(defaultTemplates, defaultTemplatesInDb);
    }

    private List<ClusterTemplate> filterTemplatesForDefaults(Collection<ClusterTemplate> clusterTemplates) {
        return clusterTemplates.stream().filter(template -> ResourceStatus.DEFAULT == template.getStatus())
                .collect(Collectors.toList());
    }

    private boolean isAllDefaultTemplateInDbHasTheSameContentAsCached(Map<String, ClusterTemplate> defaultTemplates,
            Collection<ClusterTemplate> defaultTemplatesInDb) {
        return defaultTemplatesInDb.stream().anyMatch(clusterTemplate -> {
            ClusterTemplate defaultTemplate = defaultTemplates.get(clusterTemplate.getName());
            return isTemplatesContentDifferent(clusterTemplate, defaultTemplate);
        });
    }

    private boolean isTemplatesContentDifferent(ClusterTemplate clusterTemplate, ClusterTemplate defaultTemplate) {
        String templateContent = vaultService.resolveSingleValue(clusterTemplate.getTemplate());
        return !templateContent.equals(defaultTemplate.getTemplate())
                || !clusterTemplate.getDescription().equals(defaultTemplate.getDescription())
                || !clusterTemplate.getCloudPlatform().equals(defaultTemplate.getCloudPlatform());
    }

    private boolean isAllDefaultTemplateExistsInDbByName(Map<String, ClusterTemplate> defaultTemplates, Collection<ClusterTemplate> defaultTemplatesInDb) {
        return defaultTemplatesInDb.stream().map(ClusterTemplate::getName).collect(Collectors.toSet()).containsAll(defaultTemplates.keySet());
    }

    public Set<ClusterTemplate> loadClusterTemplatesForWorkspace(Set<ClusterTemplate> templatesInDb, Workspace workspace,
            Function<Iterable<ClusterTemplate>, Iterable<ClusterTemplate>> saveMethod) {
        Set<ClusterTemplate> clusterTemplatesToSave = collectClusterTemplatesToSaveInDb(templatesInDb, workspace);
        Iterable<ClusterTemplate> savedClusterTemplates = saveMethod.apply(clusterTemplatesToSave);
        return unifyTemplatesUpdatedAndUnmodified(templatesInDb, clusterTemplatesToSave, savedClusterTemplates);
    }

    private Set<ClusterTemplate> unifyTemplatesUpdatedAndUnmodified(Set<ClusterTemplate> templatesInDb, Set<ClusterTemplate> clusterTemplatesToSave,
            Iterable<ClusterTemplate> savedClusterTemplates) {
        Set<String> savedTemplatesName = clusterTemplatesToSave.stream().map(ClusterTemplate::getName).collect(Collectors.toSet());
        Set<ClusterTemplate> templatesNotModified = templatesInDb.stream()
                .filter(template -> !savedTemplatesName.contains(template.getName())).collect(Collectors.toSet());
        Set<ClusterTemplate> unionOfTemplates = Sets.newHashSet(savedClusterTemplates);
        unionOfTemplates.addAll(templatesNotModified);
        return unionOfTemplates;
    }

    private Set<ClusterTemplate> collectClusterTemplatesToSaveInDb(Set<ClusterTemplate> templatesInDb, Workspace workspace) {
        Map<String, ClusterTemplate> defaultTemplates = defaultClusterTemplateCache.defaultClusterTemplates();
        List<ClusterTemplate> defaultTemplatesInDb = filterTemplatesForDefaults(templatesInDb);
        Collection<ClusterTemplate> templatesMissingFromDb = collectTemplatesMissingFromDb(defaultTemplates, defaultTemplatesInDb, workspace);
        Collection<ClusterTemplate> updatedTemplates = updateAndCollectOutdatedTemplatesInDb(defaultTemplates, defaultTemplatesInDb);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Templates missing from DB: {}",
                    templatesMissingFromDb.stream().map(ClusterTemplate::getName).collect(Collectors.joining(", ")));
            LOGGER.debug("Templates to be updated in DB: {}",
                    updatedTemplates.stream().map(ClusterTemplate::getName).collect(Collectors.joining(", ")));
        }
        return Stream.concat(templatesMissingFromDb.stream(), updatedTemplates.stream()).collect(Collectors.toSet());
    }

    private Collection<ClusterTemplate> collectTemplatesMissingFromDb(Map<String, ClusterTemplate> defaultTemplates,
            Collection<ClusterTemplate> defaultTemplatesInDb, Workspace workspace) {
        Set<String> defaultTemplateInDbNames = defaultTemplatesInDb.stream().map(ClusterTemplate::getName).collect(Collectors.toSet());
        Set<ClusterTemplate> templatesMissingFromDb = defaultTemplates.values().stream()
                .filter(defaultTemplate -> !defaultTemplateInDbNames.contains(defaultTemplate.getName()))
                .collect(Collectors.toSet());
        templatesMissingFromDb.forEach(template -> template.setWorkspace(workspace));
        return templatesMissingFromDb;
    }

    private Collection<ClusterTemplate> updateAndCollectOutdatedTemplatesInDb(Map<String, ClusterTemplate> defaultTemplates,
            Iterable<ClusterTemplate> defaultTemplatesInDb) {
        Collection<ClusterTemplate> updatedTemplates = new HashSet<>();
        for (ClusterTemplate template : defaultTemplatesInDb) {
            ClusterTemplate defaultTemplate = defaultTemplates.get(template.getName());
            if (isTemplatesContentDifferent(template, defaultTemplate)) {
                template.setTemplate(defaultTemplate.getTemplate());
                template.setCloudPlatform(defaultTemplate.getCloudPlatform());
                template.setDescription(defaultTemplate.getDescription());
                updatedTemplates.add(template);
            }
        }
        return updatedTemplates;
    }
}
