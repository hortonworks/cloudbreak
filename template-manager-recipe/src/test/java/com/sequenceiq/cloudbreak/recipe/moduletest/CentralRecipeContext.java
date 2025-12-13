package com.sequenceiq.cloudbreak.recipe.moduletest;

import static org.mockito.Mockito.mock;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.vault.core.VaultTemplate;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.recipe.CentralRecipeUpdater;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.service.secret.SecretEngine;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurator;
import com.sequenceiq.common.model.FileSystemType;

@ContextConfiguration
@TestPropertySource(properties = {
        "cb.freeipa.dns.ttl=30"
})
public class CentralRecipeContext {

    @Inject
    private CentralRecipeUpdater underTest;

    public CentralRecipeUpdater getUnderTest() {
        return underTest;
    }

    @Configuration
    @ComponentScan({ "com.sequenceiq.cloudbreak.recipe", "com.sequenceiq.cloudbreak.template" })
    public static class SpringConfig {

        @Inject
        private List<FileSystemConfigurator<?>> fileSystemConfigurators;

        @MockBean
        private EntitlementService entitlementService;

        @Bean
        public Map<FileSystemType, FileSystemConfigurator<?>> fileSystemConfigurators() {
            Map<FileSystemType, FileSystemConfigurator<?>> map = new EnumMap<>(FileSystemType.class);
            for (FileSystemConfigurator<?> fileSystemConfigurator : fileSystemConfigurators) {
                map.put(fileSystemConfigurator.getFileSystemType(), fileSystemConfigurator);
            }
            return map;
        }

        @Bean
        public freemarker.template.Configuration configurationProvider() {
            return new freemarker.template.Configuration();
        }

        @Bean
        public SecretEngine secretEngine() {
            return mock(SecretEngine.class);
        }

        @Bean
        public SecretService secretService() {
            return mock(SecretService.class);
        }

        @Bean
        public MetricService metricService() {
            return mock(MetricService.class);
        }

        @Bean
        public VaultTemplate vaultTemplate() {
            return mock(VaultTemplate.class);
        }

        @Bean
        public JsonHelper jsonHelperProvider() {
            return new JsonHelper();
        }

        @Bean
        public CloudbreakResourceReaderService cloudbreakResourceReaderService() {
            return new CloudbreakResourceReaderService();
        }

        private int parseCardinality(Map<String, Integer> cardinalityReps, String cardinalityString, int defaultValue) {
            Integer cardinality = cardinalityReps.get(cardinalityString);
            return cardinality == null ? defaultValue : cardinality;
        }
    }

}
