package com.sequenceiq.cloudbreak.init.clustertemplate;

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
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Service
public class ClusterTemplateLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateLoaderService.class);

    @Inject
    private DefaultClusterTemplateCache defaultClusterTemplateCache;

    public boolean isDefaultClusterTemplateUpdateNecessaryForUser(Collection<ClusterTemplate> clusterTemplates) {
        Map<String, String> defaultTemplates = defaultClusterTemplateCache.defaultClusterTemplateRequests();
        List<ClusterTemplate> defaultTemplatesInDb = filterTemplatesForDefaults(clusterTemplates);
        LOGGER.info("Merge default templates in db ({}) with cached templates {}", defaultTemplatesInDb.size(), defaultTemplates.size());
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
        Iterable<ClusterTemplate> savedClusterTemplates = saveMethod.apply(clusterTemplatesToSave);
        return unifyTemplatesUpdatedAndUnmodified(templatesInDb, clusterTemplatesToSave, savedClusterTemplates);
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
        Map<String, ClusterTemplate> defaultTemplates = defaultClusterTemplateCache.defaultClusterTemplates();
        List<ClusterTemplate> defaultTemplatesInDb = filterTemplatesForDefaults(templatesInDb);
        Collection<ClusterTemplate> templatesMissingFromDb = collectTemplatesMissingFromDb(defaultTemplates, defaultTemplatesInDb, workspace);
        Collection<ClusterTemplate> updatedTemplates = collectOutdatedTemplatesInDb(defaultTemplatesInDb);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Templates missing from DB: {}",
                    templatesMissingFromDb.stream().map(ClusterTemplate::getName).collect(Collectors.joining(", ")));
        }
        return Stream.concat(templatesMissingFromDb.stream(), updatedTemplates.stream()).collect(Collectors.toSet());
    }

    public Collection<ClusterTemplate> collectTemplatesMissingFromDb(Map<String, ClusterTemplate> defaultTemplates,
            Collection<ClusterTemplate> defaultTemplatesInDb, Workspace workspace) {
        Set<String> defaultTemplateInDbNames = defaultTemplatesInDb.stream().map(ClusterTemplate::getName).collect(Collectors.toSet());
        Set<ClusterTemplate> templatesMissingFromDb = defaultTemplates.values().stream()
                .filter(defaultTemplate -> !defaultTemplateInDbNames.contains(defaultTemplate.getName()))
                .collect(Collectors.toSet());
        templatesMissingFromDb.forEach(template -> template.setWorkspace(workspace));
        return templatesMissingFromDb;
    }

    public Collection<ClusterTemplate> collectOutdatedTemplatesInDb(Collection<ClusterTemplate> clusterTemplates) {
        return collectOutdatedTemplatesInDb(defaultClusterTemplateCache.defaultClusterTemplateRequests(), clusterTemplates);
    }

    public Collection<ClusterTemplate> collectOutdatedTemplatesInDb(Map<String, String> defaultTemplates,
            Collection<ClusterTemplate> defaultTemplatesInDb) {
        Collection<ClusterTemplate> outdatedTemplates = new HashSet<>();
        for (ClusterTemplate template : defaultTemplatesInDb) {
            Optional.ofNullable(defaultTemplates.get(template.getName()))
                    .filter(defaultTmplBase64 -> isTemplatesContentDifferent(template, defaultTmplBase64))
                    .ifPresent(defaultTmplBase64 -> outdatedTemplates.add(template));
        }
        return outdatedTemplates;
    }
}
