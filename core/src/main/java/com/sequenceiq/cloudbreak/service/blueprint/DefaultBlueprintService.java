package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.CentralBlueprintParameterQueryService;
import com.sequenceiq.cloudbreak.blueprint.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.blueprint.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractOrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
public class DefaultBlueprintService extends AbstractOrganizationAwareResourceService<Blueprint> implements BlueprintService, LegacyBlueprintService  {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBlueprintService.class);

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Inject
    private CentralBlueprintParameterQueryService centralBlueprintParameterQueryService;

    @Inject
    private OrganizationService organizationService;

    public Blueprint get(Long id) {
        return blueprintRepository.findById(id).orElseThrow(notFound("Blueprint", id));
    }

    public Blueprint create(Organization organization, Blueprint blueprint, Collection<Map<String, Map<String, String>>> properties) {
        LOGGER.debug("Creating blueprint: Organization: {} ({})", organization.getId(), organization.getName());
        Blueprint savedBlueprint;
        if (properties != null && !properties.isEmpty()) {
            LOGGER.info("Extend blueprint with the following properties: {}", properties);
            Map<String, Map<String, String>> configs = new HashMap<>(properties.size());
            for (Map<String, Map<String, String>> property : properties) {
                for (Map.Entry<String, Map<String, String>> entry : property.entrySet()) {
                    Map<String, String> configValues = configs.get(entry.getKey());
                    if (configValues != null) {
                        configValues.putAll(entry.getValue());
                    } else {
                        configs.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            String extendedBlueprint = blueprintProcessorFactory.get(blueprint.getBlueprintText())
                    .extendBlueprintGlobalConfiguration(SiteConfigurations.fromMap(configs), false).asText();
            LOGGER.info("Extended blueprint result: {}", extendedBlueprint);
            blueprint.setBlueprintText(extendedBlueprint);
        }
        try {
            savedBlueprint = create(blueprint, organization.getId());
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.BLUEPRINT, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg, ex);
        }
        return savedBlueprint;
    }

    public Iterable<Blueprint> saveAll(Iterable<Blueprint> entities) {
        return blueprintRepository.saveAll(entities);
    }

    public Blueprint delete(Long id) {
        return delete(get(id));
    }

    @Override
    public Blueprint delete(Blueprint blueprint) {
        LOGGER.info("Deleting blueprint with name: {}", blueprint.getName());
        if (ResourceStatus.USER_MANAGED.equals(blueprint.getStatus())) {
            blueprintRepository.delete(blueprint);
        } else {
            blueprint.setName(NameUtil.postfixWithTimestamp(blueprint.getName()));
            blueprint.setStatus(ResourceStatus.DEFAULT_DELETED);
            blueprint = blueprintRepository.save(blueprint);
        }
        return blueprint;
    }

    @Override
    protected OrganizationResourceRepository<Blueprint, Long> repository() {
        return blueprintRepository;
    }

    @Override
    protected OrganizationResource resource() {
        return OrganizationResource.BLUEPRINT;
    }

    @Override
    protected void prepareDeletion(Blueprint blueprint) {
        List<Cluster> clustersWithThisBlueprint = clusterService.getByBlueprint(blueprint);
        if (!clustersWithThisBlueprint.isEmpty()) {
            if (clustersWithThisBlueprint.size() > 1) {
                String clusters = clustersWithThisBlueprint
                        .stream()
                        .map(Cluster::getName)
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with blueprint '%s'. Please remove these before deleting the blueprint. "
                                + "The following clusters are using this blueprint: [%s]", blueprint.getName(), clusters));
            }
            throw new BadRequestException(String.format("There is a cluster ['%s'] which uses blueprint '%s'. Please remove this "
                    + "cluster before deleting the blueprint", clustersWithThisBlueprint.get(0).getName(), blueprint.getName()));
        }
    }

    @Override
    protected void prepareCreation(Blueprint resource) {

    }

    @Override
    public Set<String> queryCustomParameters(String name, Organization organization) {
        Blueprint blueprint = getByNameForOrganization(name, organization);
        return centralBlueprintParameterQueryService.queryCustomParameters(blueprint.getBlueprintText());
    }

    @Override
    public Set<ConfigQueryEntry> queryFileSystemParameters(String blueprintName, String clusterName,
            String storageName, String fileSystemType, String accountName, boolean attachedCluster, Organization organization) {
        Blueprint blueprint = getByNameForOrganization(blueprintName, organization);

        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withClusterName(clusterName)
                .withStorageName(storageName)
                .withBlueprintText(blueprint.getBlueprintText())
                .withFileSystemType(fileSystemType)
                .withAccountName(accountName)
                .withAttachedCluster(attachedCluster)
                .build();

        return centralBlueprintParameterQueryService.queryFileSystemParameters(fileSystemConfigQueryObject);
    }
}
