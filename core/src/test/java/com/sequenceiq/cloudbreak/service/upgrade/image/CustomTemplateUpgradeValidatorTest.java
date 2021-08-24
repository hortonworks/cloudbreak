package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
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

    private Blueprint blueprint;

    @Before
    public void before() {
        ReflectionTestUtils.setField(underTest, "permittedServicesForUpgrade", createPermittedServicesForUpgrade());
        blueprint = createBlueprint();
    }

    @Test
    public void testIsValidShouldReturnFalseWhenTheUpgradePermittedServicesListIsEmpty() {
        ReflectionTestUtils.setField(underTest, "permittedServicesForUpgrade", Collections.emptySet());
        when(cmTemplateProcessor.getAllComponents()).thenReturn(createServiceComponentsWithUpgradePermittedServices());
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);

        BlueprintValidationResult actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.isValid(blueprint));

        assertFalse(actual.isValid());
        assertEquals("The following services are not eligible for upgrade in the cluster template: [HIVE_ON_TEZ, HIVE, HDFS, HUE, KNOX]", actual.getReason());

        verify(cmTemplateProcessorFactory).get(BLUEPRINT_TEXT);
    }

    @Test
    public void testIsValidShouldReturnTrueWhenAllUpgradePermittedServicesArePresentInTheServiceList() {
        when(cmTemplateProcessor.getAllComponents()).thenReturn(createServiceComponentsWithUpgradePermittedServices());
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);

        BlueprintValidationResult actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.isValid(blueprint));

        assertTrue(actual.isValid());
        assertNull(actual.getReason());
        verify(cmTemplateProcessorFactory).get(BLUEPRINT_TEXT);
    }

    @Test
    public void testIsValidShouldReturnFalseWhenUpgradePermittedServicesAreNotPresentInTheServiceList() {
        when(cmTemplateProcessor.getAllComponents()).thenReturn(createServiceComponentsWithUpgradePermittedServicesAndOneExtra());
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);

        BlueprintValidationResult actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.isValid(blueprint));

        assertFalse(actual.isValid());
        assertEquals("The following services are not eligible for upgrade in the cluster template: [NIFI]", actual.getReason());
        verify(cmTemplateProcessorFactory).get(BLUEPRINT_TEXT);
    }

    private Blueprint createBlueprint() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        return blueprint;
    }

    private Set<String> createPermittedServicesForUpgrade() {
        return Set.of("DAS", "HDFS", "HIVE", "HIVE_ON_TEZ", "HUE", "KNOX");
    }

    private Set<ServiceComponent> createServiceComponentsWithUpgradePermittedServicesAndOneExtra() {
        return Set.of(
                createServiceComponent("DAS"),
                createServiceComponent("HDFS"),
                createServiceComponent("HIVE_ON_TEZ"),
                createServiceComponent("HUE"),
                createServiceComponent("KNOX"),
                createServiceComponent("NIFI"));
    }

    private Set<ServiceComponent> createServiceComponentsWithUpgradePermittedServices() {
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