package com.sequenceiq.cloudbreak.init.clustertemplate;

import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.DefaultClusterTemplateV4RequestToClusterTemplateConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.distrox.v1.distrox.service.InternalClusterTemplateValidator;

@Service
public class DefaultClusterTemplateCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClusterTemplateCache.class);

    private final Map<String, Pair<DefaultClusterTemplateV4Request, String>> defaultClusterTemplates = new ConcurrentHashMap<>();

    @Value("#{'${cb.clustertemplate.defaults:}'.split(',')}")
    private List<String> clusterTemplates;

    private String defaultTemplateDir = "defaults/clustertemplates";

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private InternalClusterTemplateValidator internalClusterTemplateValidator;

    @Inject
    private DefaultClusterTemplateV4RequestToClusterTemplateConverter defaultClusterTemplateV4RequestToClusterTemplateConverter;

    @Inject
    private ProviderPreferencesService preferencesService;

    @Inject
    private CommonGovService commonGovService;

    @PostConstruct
    public void loadClusterTemplatesFromFile() {
        if (clusterTemplates.stream().anyMatch(StringUtils::isNotEmpty)) {
            LOGGER.debug("Default cluster template is loaded into cache by property: {}", clusterTemplates);
            loadByProperty();
        } else {
            loadByResourceDir();
        }
    }

    private void loadByResourceDir() {
        List<String> files;
        try {
            files = getFiles();
        } catch (Exception e) {
            LOGGER.warn("Failed to load files from: {}, original msg: {}", defaultTemplateDir, e.getMessage(), e);
            return;
        }
        if (!files.isEmpty()) {
            LOGGER.debug("Default clustertemplate is loaded into cache by resource dir: {}", String.join(", ", files));
            loadByClasspathPath(files);
        } else {
            LOGGER.debug("No default cluster template");
        }
    }

    private void loadByProperty() {
        loadByClasspathPath(clusterTemplates
                .stream()
                .filter(StringUtils::isNotBlank)
                .map(s -> defaultTemplateDir + File.separator + s.trim()).collect(Collectors.toList()));
    }

    private void loadByClasspathPath(Collection<String> names) {
        Set<String> enabledPlatforms = preferencesService.enabledPlatforms();
        Set<String> enabledGovPlatforms = preferencesService.enabledGovPlatforms();
        names.stream()
                .filter(StringUtils::isNotBlank)
                .forEach(clusterTemplateName -> {
                    try {
                        String templateAsString = readFileFromClasspath(clusterTemplateName);
                        addClusterTemplateToDefaultClusterTemplates(
                                clusterTemplateName,
                                templateAsString,
                                enabledPlatforms,
                                enabledGovPlatforms);
                        LOGGER.debug("Default clustertemplate is loaded into cache by resource file: {}", clusterTemplateName);
                    } catch (IOException e) {
                        String msg = "Could not load cluster template: " + clusterTemplateName;
                        if (!clusterTemplateName.endsWith(".json")) {
                            msg += ". The json postfix is missing?";
                        }
                        LOGGER.warn(msg, e);
                    }
                });
    }

    private void addClusterTemplateToDefaultClusterTemplates(String clusterTemplateName, String templateAsString,
            Set<String> enabledPlatforms, Set<String> enabledGovPlatforms) throws IOException {
        DefaultClusterTemplateV4Request clusterTemplateRequest = new Json(templateAsString).get(DefaultClusterTemplateV4Request.class);
        boolean useIt = doUseIt(clusterTemplateName, clusterTemplateRequest, enabledPlatforms, enabledGovPlatforms);
        if (useIt) {
            if (defaultClusterTemplates.get(clusterTemplateRequest.getName()) != null) {
                LOGGER.warn("Default cluster template exists and it will be override: {}", clusterTemplateRequest.getName());
            }
            defaultClusterTemplates.put(
                    clusterTemplateRequest.getName(),
                    Pair.of(clusterTemplateRequest, Base64Util.encode(templateAsString)));
        }
    }

    private boolean doUseIt(String clusterTemplateName, DefaultClusterTemplateV4Request clusterTemplateRequest,
            Set<String> enabledPlatforms, Set<String> enabledGovPlatforms) {
        boolean useIt = true;
        String aws = CloudPlatform.AWS.name();
        LOGGER.info("Enabled commercial platforms: {}", enabledPlatforms);
        LOGGER.info("Enabled gov platforms: {}", enabledGovPlatforms);
        boolean awsGovTemplate = clusterTemplateName.contains(CommonGovService.GOV);
        boolean awsGovEnabledOnDeployment = enabledGovPlatforms.contains(aws);
        boolean awsCommercialEnabledOnDeployment = enabledPlatforms.contains(aws);
        boolean localDeployment = awsGovEnabledOnDeployment && awsCommercialEnabledOnDeployment;
        boolean govCloudDeployment = commonGovService.govCloudDeployment(enabledGovPlatforms, enabledPlatforms);
        if (isAwsTemplate(clusterTemplateRequest, aws)) {
            LOGGER.info("The current {} template is an aws template", clusterTemplateRequest.getName());
            // the template version is AWS
            if (isLocalDeploymentAndGovTemplate(awsGovTemplate, localDeployment)) {
                // in case of local deployment we dont need to load gov templates
                LOGGER.info("The deployment is a local deployment so skipping this {} template", clusterTemplateRequest.getName());
                useIt = false;
            } else if (!localDeployment) {
                if (isCommercialDeploymentAndGovAWSTemplate(awsGovTemplate, awsCommercialEnabledOnDeployment)) {
                    // NOT a Gov deployment we dont need to load gov templates
                    LOGGER.info("The deployment is a commercial deployment so skipping this {} gov template", clusterTemplateRequest.getName());
                    useIt = false;
                }
                if (isGovDeploymentAndCommercialAWSTemplate(awsGovTemplate, awsGovEnabledOnDeployment)) {
                    // Gov deployment so no need to load commercial aws templates
                    LOGGER.info("The deployment is a gov deployment so skipping this {} commercial template", clusterTemplateRequest.getName());
                    useIt = false;
                }
            }
        } else if (govCloudDeployment) {
            // this is a gov deployment no need to load any other template just gov templates
            useIt = false;
        }
        return useIt;
    }

    private static boolean isAwsTemplate(DefaultClusterTemplateV4Request clusterTemplateRequest, String aws) {
        return clusterTemplateRequest.getCloudPlatform().equals(aws);
    }

    private static boolean isLocalDeploymentAndGovTemplate(boolean awsGovTemplate, boolean localDeployment) {
        return localDeployment && awsGovTemplate;
    }

    private static boolean isCommercialDeploymentAndGovAWSTemplate(boolean awsGovTemplate, boolean awsEnabled) {
        return awsGovTemplate && awsEnabled;
    }

    private static boolean isGovDeploymentAndCommercialAWSTemplate(boolean awsGovTemplate, boolean awsGovEnabled) {
        return !awsGovTemplate && awsGovEnabled;
    }

    public Map<String, Pair<DefaultClusterTemplateV4Request, String>> defaultClusterTemplateRequests() {
        return defaultClusterTemplates;
    }

    public Map<String, String> defaultClusterTemplateRequestsForUser() {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        boolean hybridEnabled = entitlementService.hybridCloudEnabled(workspace.getTenant().getName());
        return defaultClusterTemplates.entrySet().stream()
                .filter(e -> notHybridOrHybridEnabled(e.getValue().getKey(), hybridEnabled))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue()));
    }

    public Map<String, ClusterTemplate> defaultClusterTemplates() {
        Map<String, ClusterTemplate> defaultTemplates = new HashMap<>();
        defaultClusterTemplateRequests().forEach((key, value) -> {
            DefaultClusterTemplateV4Request defaultClusterTemplate = value.getKey();
            ClusterTemplate clusterTemplate = defaultClusterTemplateV4RequestToClusterTemplateConverter.convert(defaultClusterTemplate);
            defaultTemplates.put(key, clusterTemplate);
        });
        return defaultTemplates;
    }

    public Set<String> getDefaultInstanceTypesFromTemplates(String cloudPlatform, Architecture architecture) {
        return defaultClusterTemplateRequests()
                .entrySet()
                .stream()
                .map(e -> collectInstancesForPlatform(cloudPlatform, architecture, e))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private Set<String> collectInstancesForPlatform(String cloudPlatform, Architecture architecture,
            Map.Entry<String, Pair<DefaultClusterTemplateV4Request, String>> entry) {
        if (isPlatformMatch(cloudPlatform, entry) && isArchitectureMatch(architecture, entry)) {
            return entry.getValue().getKey().getDistroXTemplate().getInstanceGroups()
                    .stream()
                    .map(template -> template.getTemplate().getInstanceType())
                    .collect(Collectors.toSet());
        } else {
            return Set.of();
        }
    }

    private boolean isArchitectureMatch(Architecture architecture, Map.Entry<String, Pair<DefaultClusterTemplateV4Request, String>> entry) {
        String templateArchitecture = entry.getValue().getKey().getDistroXTemplate().getArchitecture();
        templateArchitecture = templateArchitecture == null ? Architecture.X86_64.name() : templateArchitecture;
        return architecture.name().equalsIgnoreCase(templateArchitecture);
    }

    private boolean isPlatformMatch(String cloudPlatform, Map.Entry<String, Pair<DefaultClusterTemplateV4Request, String>> entry) {
        return cloudPlatform != null && entry.getValue().getKey().getCloudPlatform().equalsIgnoreCase(cloudPlatform);
    }

    public List<ClusterTemplate> defaultClusterTemplatesByNames(Collection<String> templateNamesMissingFromDb, Set<Blueprint> blueprints) {
        List<ClusterTemplate> defaultTemplates = new ArrayList<>();
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        String accountId = workspace.getTenant().getName();
        boolean internalTenant = entitlementService.internalTenant(accountId);
        boolean hybridEnabled = entitlementService.hybridCloudEnabled(accountId);
        defaultClusterTemplateRequests().forEach((key, value) -> {
            if (templateNamesMissingFromDb.contains(key)) {
                DefaultClusterTemplateV4Request defaultClusterTemplate = value.getKey();
                if (internalClusterTemplateValidator.shouldPopulate(defaultClusterTemplate, internalTenant) &&
                        notHybridOrHybridEnabled(defaultClusterTemplate, hybridEnabled)) {
                    ClusterTemplate clusterTemplate = defaultClusterTemplateV4RequestToClusterTemplateConverter.convert(defaultClusterTemplate);
                    clusterTemplate.setWorkspace(workspace);
                    Optional<Blueprint> blueprint = blueprints.stream()
                            .filter(e -> e.getName().equals(defaultClusterTemplate.getDistroXTemplate().getCluster().getBlueprintName()))
                            .findFirst();
                    if (blueprint.isPresent()) {
                        clusterTemplate.setClouderaRuntimeVersion(blueprint.get().getStackVersion());
                        defaultTemplates.add(clusterTemplate);
                    }
                }
            }
        });
        return defaultTemplates;
    }

    private boolean notHybridOrHybridEnabled(DefaultClusterTemplateV4Request defaultClusterTemplate, boolean hybridEnabled) {
        return !defaultClusterTemplate.getType().isHybrid() || hybridEnabled;
    }

    public Collection<String> defaultClusterTemplateNames() {
        Collection<String> defaultTemplates = new ArrayList<>();
        defaultClusterTemplateRequests().forEach((key, value) -> {
            defaultTemplates.add(key);
        });
        return defaultTemplates;
    }

    public DefaultClusterTemplateV4Request getDefaultClusterTemplate(String defaultTemplateJson) {
        try {
            return JsonUtil.readValue(defaultTemplateJson, DefaultClusterTemplateV4Request.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Default cluster template could not be added, causes: " + e.getMessage(), e);
        }
    }

    protected void setClusterTemplates(List<String> clusterTemplates) {
        this.clusterTemplates = clusterTemplates;
    }

    protected void setDefaultTemplateDir(String defaultTemplateDir) {
        this.defaultTemplateDir = defaultTemplateDir;
    }

    public String getByName(String name) {
        return defaultClusterTemplates.get(name).getValue();
    }

    private List<String> getFiles() throws IOException {
        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
        return Arrays.stream(patternResolver.getResources("classpath:" + defaultTemplateDir + "/**/*.json"))
                .map(resource -> {
                    try {
                        String[] path = resource.getURL().getPath().split(defaultTemplateDir);
                        return String.format("%s%s", defaultTemplateDir, path[1]);
                    } catch (IOException e) {
                        // wrap to runtime exception because of lambda and log the error in the caller method.
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }
}
