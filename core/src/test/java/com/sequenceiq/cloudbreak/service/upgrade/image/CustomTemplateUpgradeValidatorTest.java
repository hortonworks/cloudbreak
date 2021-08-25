package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@RunWith(MockitoJUnitRunner.class)
public class CustomTemplateUpgradeValidatorTest {

    private static final String BLUEPRINT_TEXT = "blueprint-text";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String BLUEPRINT_VERSION = "blueprintVersion";

    private static final String SERVICE_1 = "Service1";

    private static final String SERVICE_2 = "Service2";

    @InjectMocks
    private CustomTemplateUpgradeValidator underTest;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private PermittedServicesForUpgradeService permittedServicesForUpgradeService;

    private final Blueprint blueprint = createBlueprint();

    @Test
    @DisplayName("All services are permitted then validation should pass")
    public void testIsValidShouldReturnTrueWhenAllServicesArePermitted() {
        when(cmTemplateProcessor.getAllComponents()).thenReturn(createServiceComponents(Set.of(SERVICE_1, SERVICE_2)));
        createPermittedServicesForUpgrade(Set.of(SERVICE_1, SERVICE_2));
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);

        BlueprintValidationResult actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.isValid(blueprint));

        assertTrue(actual.isValid());
        assertNull(actual.getReason());
        verify(cmTemplateProcessorFactory).get(BLUEPRINT_TEXT);
        verify(permittedServicesForUpgradeService).isAllowedForUpgrade(SERVICE_1, BLUEPRINT_VERSION);
        verify(permittedServicesForUpgradeService).isAllowedForUpgrade(SERVICE_2, BLUEPRINT_VERSION);
    }

    @Test
    @DisplayName("At least one service is not permitted then validation should fail")
    public void testIsValidShouldReturnFalseWhenAnyServiceIsNotPermitted() {
        when(cmTemplateProcessor.getAllComponents()).thenReturn(createServiceComponents(Set.of(SERVICE_1, SERVICE_2)));
        createPermittedServicesForUpgrade(Set.of(SERVICE_1));
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);

        BlueprintValidationResult actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.isValid(blueprint));

        assertFalse(actual.isValid());
        assertEquals(String.format("The following services are not eligible for upgrade in the cluster template: [%s]", SERVICE_2), actual.getReason());
        verify(cmTemplateProcessorFactory).get(BLUEPRINT_TEXT);
        verify(permittedServicesForUpgradeService).isAllowedForUpgrade(SERVICE_1, BLUEPRINT_VERSION);
        verify(permittedServicesForUpgradeService).isAllowedForUpgrade(SERVICE_2, BLUEPRINT_VERSION);
    }

    private Blueprint createBlueprint() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        blueprint.setStackVersion(BLUEPRINT_VERSION);
        return blueprint;
    }

    private void createPermittedServicesForUpgrade(Set<String> allowedServices) {
        allowedServices.forEach(service -> when(permittedServicesForUpgradeService.isAllowedForUpgrade(eq(service), anyString())).thenReturn(true));
    }

    private Set<ServiceComponent> createServiceComponents(Set<String> serviceNames) {
        return serviceNames.stream()
                .map(this::createServiceComponent)
                .collect(Collectors.toSet());
    }

    private ServiceComponent createServiceComponent(String serviceName) {
        return ServiceComponent.of(serviceName, serviceName);
    }

}