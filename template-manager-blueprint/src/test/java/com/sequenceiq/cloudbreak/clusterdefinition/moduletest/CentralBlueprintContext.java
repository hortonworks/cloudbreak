package com.sequenceiq.cloudbreak.clusterdefinition.moduletest;

import static org.mockito.Mockito.mock;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.vault.core.VaultTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.clusterdefinition.CentralClusterDefinitionUpdater;
import com.sequenceiq.cloudbreak.clusterdefinition.validation.StackServiceComponentDescriptor;
import com.sequenceiq.cloudbreak.clusterdefinition.validation.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.ha.CloudbreakNodeConfig;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.service.metrics.MetricService;
import com.sequenceiq.cloudbreak.service.secret.SecretEngine;
import com.sequenceiq.cloudbreak.service.secret.SecretService;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@ContextConfiguration
public class CentralBlueprintContext {

    @Inject
    private CentralClusterDefinitionUpdater underTest;

    public CentralClusterDefinitionUpdater getUnderTest() {
        return underTest;
    }

    @Configuration
    @ComponentScan({"com.sequenceiq.cloudbreak.clusterdefinition", "com.sequenceiq.cloudbreak.template"})
    public static class SpringConfig {

        @Inject
        private List<FileSystemConfigurator<?>> fileSystemConfigurators;

        @Bean
        public Map<FileSystemType, FileSystemConfigurator<?>> fileSystemConfigurators() {
            Map<FileSystemType, FileSystemConfigurator<?>> map = new EnumMap<>(FileSystemType.class);
            for (FileSystemConfigurator<?> fileSystemConfigurator : fileSystemConfigurators) {
                map.put(fileSystemConfigurator.getFileSystemType(), fileSystemConfigurator);
            }
            return map;
        }

        @Bean
        public CloudbreakNodeConfig cloudbreakNodeConfigProvider() {
            return new CloudbreakNodeConfig();
        }

        @Bean
        public freemarker.template.Configuration configurationProvider() {
            return new freemarker.template.Configuration();
        }

        @Bean
        public JsonHelper jsonHelperProvider() {
            return new JsonHelper();
        }

        @Bean
        public CloudbreakResourceReaderService cloudbreakResourceReaderService() {
            return new CloudbreakResourceReaderService();
        }

        @Bean
        public SecretEngine secretEngine() {
            return Mockito.mock(SecretEngine.class);
        }

        @Bean
        public SecretService secretService() {
            return Mockito.mock(SecretService.class);
        }

        @Bean
        public MetricService metricService() {
            return Mockito.mock(MetricService.class);
        }

        @Bean
        public VaultTemplate vaultTemplate() {
            return mock(VaultTemplate.class);
        }

        @Bean
        public StackServiceComponentDescriptors stackServiceComponentDescriptors() throws Exception {
            Map<String, Integer> minCardinalityReps = Maps.newHashMap();
            minCardinalityReps.put("1", 1);
            minCardinalityReps.put("0-1", 0);
            minCardinalityReps.put("1-2", 1);
            minCardinalityReps.put("0+", 0);
            minCardinalityReps.put("1+", 1);
            minCardinalityReps.put("ALL", 0);
            Map<String, Integer> maxCardinalityReps = Maps.newHashMap();
            maxCardinalityReps.put("1", 1);
            maxCardinalityReps.put("0-1", 1);
            maxCardinalityReps.put("1-2", 2);
            maxCardinalityReps.put("0+", Integer.MAX_VALUE);
            maxCardinalityReps.put("1+", Integer.MAX_VALUE);
            maxCardinalityReps.put("ALL", Integer.MAX_VALUE);
            String stackServiceComponentsJson = FileReaderUtils.readFileFromClasspath("hdp/hdp-services.json");
            return createServiceComponentDescriptors(stackServiceComponentsJson, minCardinalityReps, maxCardinalityReps);
        }

        private StackServiceComponentDescriptors createServiceComponentDescriptors(String stackServiceComponentsJson, Map<String, Integer> minCardinalityReps,
                Map<String, Integer> maxCardinalityReps) throws Exception {
            Map<String, StackServiceComponentDescriptor> stackServiceComponentDescriptorMap = Maps.newHashMap();
            JsonNode rootNode = JsonUtil.readTree(stackServiceComponentsJson);
            JsonNode itemsNode = rootNode.get("items");
            for (JsonNode itemNode : itemsNode) {
                JsonNode componentsNode = itemNode.get("components");
                for (JsonNode componentNode : componentsNode) {
                    JsonNode stackServiceComponentsNode = componentNode.get("StackServiceComponents");
                    String componentName = stackServiceComponentsNode.get("component_name").asText();
                    String componentCategory = stackServiceComponentsNode.get("component_category").asText();
                    int minCardinality = parseCardinality(minCardinalityReps, stackServiceComponentsNode.get("cardinality").asText(), 0);
                    int maxCardinality = parseCardinality(maxCardinalityReps, stackServiceComponentsNode.get("cardinality").asText(), Integer.MAX_VALUE);
                    stackServiceComponentDescriptorMap.put(componentName, new StackServiceComponentDescriptor(componentName, componentCategory, minCardinality,
                            maxCardinality));
                }
            }
            return new StackServiceComponentDescriptors(stackServiceComponentDescriptorMap);
        }

        private int parseCardinality(Map<String, Integer> cardinalityReps, String cardinalityString, int defaultValue) {
            Integer cardinality = cardinalityReps.get(cardinalityString);
            return cardinality == null ? defaultValue : cardinality;
        }
    }

}
