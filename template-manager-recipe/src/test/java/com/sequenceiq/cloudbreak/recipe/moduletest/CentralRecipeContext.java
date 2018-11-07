package com.sequenceiq.cloudbreak.recipe.moduletest;

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

import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.ha.CloudbreakNodeConfig;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.recipe.CentralRecipeUpdater;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.service.secret.SecretEngine;
import com.sequenceiq.cloudbreak.service.secret.SecretService;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurator;

@ContextConfiguration
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
        public SecretEngine secretEngine() {
            return Mockito.mock(SecretEngine.class);
        }

        @Bean
        public SecretService secretService() {
            return Mockito.mock(SecretService.class);
        }

        @Bean
        public VaultTemplate vaultTemplate() {
            return Mockito.mock(VaultTemplate.class);
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
