package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@RunWith(MockitoJUnitRunner.class)
public class CustomTemplateUpgradeValidatorTest {

    private static final String BLUEPRINT_TEXT = "blueprint-text";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @InjectMocks
    private CustomTemplateUpgradeValidator underTest;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private EntitlementService entitlementService;

    private Blueprint blueprint;

    @Before
    public void before() {
        ReflectionTestUtils.setField(underTest, "requiredServices", createRequiredServices());
        blueprint = createBlueprint();
    }

    @Test
    public void testIsValidShouldReturnTrueWhenTheEntitlementIsTurnedOn() {
        when(entitlementService.datahubRuntimeUpgradeEnabledForCustomTemplate(anyString())).thenReturn(true);

        assertTrue(ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.isValid(blueprint)));

        verify(entitlementService).datahubRuntimeUpgradeEnabledForCustomTemplate(anyString());
        verifyNoInteractions(cmTemplateProcessorFactory);
    }

    @Test
    public void testIsValidShouldReturnFalseWhenTheRequiredServicesListIsEmpty() {
        ReflectionTestUtils.setField(underTest, "requiredServices", Collections.emptySet());
        when(entitlementService.datahubRuntimeUpgradeEnabledForCustomTemplate(anyString())).thenReturn(false);
        when(cmTemplateProcessor.getAllComponents()).thenReturn(createServiceComponentsWithRequiredServices());
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);

        assertFalse(ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.isValid(blueprint)));

        verify(cmTemplateProcessorFactory).get(BLUEPRINT_TEXT);
    }

    @Test
    public void testIsValidShouldReturnTrueWhenAllRequiredServicesArePresentInTheServiceList() {
        when(entitlementService.datahubRuntimeUpgradeEnabledForCustomTemplate(anyString())).thenReturn(false);
        when(cmTemplateProcessor.getAllComponents()).thenReturn(createServiceComponentsWithRequiredServices());
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);

        assertTrue(ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.isValid(blueprint)));
        verify(cmTemplateProcessorFactory).get(BLUEPRINT_TEXT);
    }

    @Test
    public void testIsValidShouldReturnFalseWhenAllRequiredServicesAreNotPresentInTheServiceList() {
        when(entitlementService.datahubRuntimeUpgradeEnabledForCustomTemplate(anyString())).thenReturn(false);
        when(cmTemplateProcessor.getAllComponents()).thenReturn(createServiceComponentsWithRequiredServicesAndOneExtra());
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);

        assertFalse(ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.isValid(blueprint)));
        verify(cmTemplateProcessorFactory).get(BLUEPRINT_TEXT);
    }

    private Blueprint createBlueprint() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        return blueprint;
    }

    private Set<String> createRequiredServices() {
        return Set.of("DAS", "HDFS", "HIVE", "HIVE_ON_TEZ", "HUE", "KNOX");
    }

    private Set<ServiceComponent> createServiceComponentsWithRequiredServicesAndOneExtra() {
        return Set.of(
                createServiceComponent("DAS"),
                createServiceComponent("HDFS"),
                createServiceComponent("HIVE_ON_TEZ"),
                createServiceComponent("HUE"),
                createServiceComponent("KNOX"),
                createServiceComponent("NIFI"));
    }

    private Set<ServiceComponent> createServiceComponentsWithRequiredServices() {
        return Set.of(
                createServiceComponent("HDFS"),
                createServiceComponent("HIVE"),
                createServiceComponent("HIVE_ON_TEZ"),
                createServiceComponent("HUE"),
                createServiceComponent("KNOX"));
    }

    private ServiceComponent createServiceComponent(String serviceName) {
        return ServiceComponent.of(serviceName, serviceName);
    }

}