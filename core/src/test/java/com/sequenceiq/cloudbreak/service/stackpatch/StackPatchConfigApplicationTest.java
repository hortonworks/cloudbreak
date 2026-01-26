package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.domain.stack.StackPatchTypeStatus.DEPRECATED;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.job.stackpatcher.config.ExistingStackPatcherConfig;
import com.sequenceiq.cloudbreak.job.stackpatcher.config.StackPatchTypeConfig;

@SpringBootTest(classes = StackPatchConfigApplicationTest.TestConfig.class)
@EnableConfigurationProperties(ExistingStackPatcherConfig.class)
public class StackPatchConfigApplicationTest {

    @Inject
    private ExistingStackPatcherConfig underTest;

    @Test
    void testStackPatchTypeConfigs() {
        Map<StackPatchType, StackPatchTypeConfig> patchConfigs = underTest.getPatchConfigs();
        patchConfigs.entrySet().stream()
                .filter(stackPatchTypeEntry -> DEPRECATED.equals(stackPatchTypeEntry.getKey().getStatus()))
                .forEach(stackPatchTypeEntry -> assertFalse(stackPatchTypeEntry.getValue().isEnabled(),
                        String.format("%s stack patch type should be disabled in config.", stackPatchTypeEntry.getKey())));
    }

    @Configuration
    static class TestConfig { }
}
