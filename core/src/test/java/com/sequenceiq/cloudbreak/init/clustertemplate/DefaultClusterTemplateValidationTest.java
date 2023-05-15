package com.sequenceiq.cloudbreak.init.clustertemplate;

import static com.sequenceiq.cloudbreak.cloud.aws.common.DistroxEnabledInstanceTypes.AWS_ENABLED_TYPES_LIST;
import static com.sequenceiq.cloudbreak.cloud.azure.DistroxEnabledInstanceTypes.AZURE_ENABLED_TYPES_LIST;
import static com.sequenceiq.cloudbreak.cloud.gcp.DistroxEnabledInstanceTypes.GCP_ENABLED_TYPES_LIST;
import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspath;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Strings;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;

@ExtendWith(MockitoExtension.class)
public class DefaultClusterTemplateValidationTest {

    private Set<String> cloudPlatformsWhichSupportsInstanceTypes = Set.of("AWS", "AZURE", "GCP");

    private String defaultTemplateDir = "defaults/clustertemplates";

    private Map<String, DefaultClusterTemplateV4Request> templates = new HashMap<>();

    private Map<String, List<String>> instanceTypes = new HashMap<>();

    @BeforeEach
    public void before() throws FileNotFoundException {
        loadByResourceDir();
        loadEnabledInstances();
    }

    @Test
    public void testInstancesWhichAreInTheDefinitions() {
        Assert.assertTrue("Template list can not be empty.", !templates.entrySet().isEmpty());
        for (Map.Entry<String, DefaultClusterTemplateV4Request> entry : templates.entrySet()) {
            String cloudPlatform = entry.getValue().getCloudPlatform();
            if (cloudPlatformsWhichSupportsInstanceTypes.contains(cloudPlatform)) {
                for (InstanceGroupV1Request instanceGroup : entry.getValue().getDistroXTemplate().getInstanceGroups()) {
                    String instanceType = instanceGroup.getTemplate().getInstanceType();
                    List<String> enabledInstances = instanceTypes.get(cloudPlatform);
                    Assert.assertTrue(String.format("Instance type in the template is null in the '%s' template.", entry.getKey()),
                            !Strings.isNullOrEmpty(instanceType));
                    Assert.assertTrue(String.format("Enabled instance list is empty for %s provider.", cloudPlatform),
                            !enabledInstances.isEmpty());
                    Assert.assertTrue(
                            String.format("'%s' template contains an instance type which is not supported which is '%s'.",
                                    entry.getKey(), instanceType),
                            enabledInstances.contains(instanceType));
                }
            }
        }
    }

    private void loadByResourceDir() {
        List<String> files;
        try {
            files = getFiles();
        } catch (Exception e) {
            return;
        }
        if (!files.isEmpty()) {
            loadByClasspathPath(files);
        }
    }

    private void loadByClasspathPath(Collection<String> names) {
        names.stream()
                .filter(StringUtils::isNotBlank)
                .forEach(clusterTemplateName -> {
                    try {
                        String templateAsString = readFileFromClasspath(clusterTemplateName);
                        templates.put(clusterTemplateName, new Json(templateAsString).get(DefaultClusterTemplateV4Request.class));
                    } catch (IOException e) {

                    }
                });
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

    private void loadEnabledInstances() throws FileNotFoundException {

        List<String> awsEnabledInstanceTypes = AWS_ENABLED_TYPES_LIST
                .stream()
                .map(s -> s.replaceAll(" ", ""))
                .collect(Collectors.toList());

        List<String> azureEnabledInstanceTypes = AZURE_ENABLED_TYPES_LIST
                .stream()
                .map(s -> s.replaceAll(" ", ""))
                .collect(Collectors.toList());

        List<String> gcpEnabledInstanceTypes = GCP_ENABLED_TYPES_LIST
                .stream()
                .map(s -> s.replaceAll(" ", ""))
                .collect(Collectors.toList());

        instanceTypes.put("AWS", awsEnabledInstanceTypes);
        instanceTypes.put("AZURE", azureEnabledInstanceTypes);
        instanceTypes.put("GCP", gcpEnabledInstanceTypes);
    }
}
