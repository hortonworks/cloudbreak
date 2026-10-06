package com.sequenceiq.cloudbreak.core.cluster.prerequisite;

import static com.sequenceiq.cloudbreak.core.cluster.prerequisite.HuePythonVersionPinningPrerequisite.DETECT_BROKEN_PYTHON312_COMMAND;
import static com.sequenceiq.cloudbreak.core.cluster.prerequisite.HuePythonVersionPinningPrerequisite.DETECT_MISSING_PYTHON311_COMMAND;
import static com.sequenceiq.cloudbreak.core.cluster.prerequisite.HuePythonVersionPinningPrerequisite.PIN_HUE_PYTHON311_COMMAND;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue.HueRoles;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.image.ClusterUpgradeTargetImageService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.retry.RetryType;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class HuePythonVersionPinningPrerequisiteTest {

    private static final Long STACK_ID = 1L;

    private static final Long WORKSPACE_ID = 2L;

    private static final String TARGET_IMAGE_ID = "target-image-id";

    private static final String CATALOG_URL = "catalog-url";

    private static final String CATALOG_NAME = "catalog-name";

    private static final String BLUEPRINT = "{\"blueprint\": \"text\"}";

    private static final String HUE_HOST_GROUP = "master";

    private static final String OTHER_HOST_GROUP = "worker";

    private static final String HOST_1 = "host1.example.com";

    private static final String HOST_2 = "host2.example.com";

    private static final String AFFECTED_CDH_PARCEL = "7.3.2-1.cdh7.3.2.p10000.83281140";

    /** Salt's own KWARG_REGEX: a positional cmd.run argument matching it is consumed as a keyword argument instead. */
    private static final Pattern SALT_KWARG_PATTERN = Pattern.compile("^([^\\d\\W][\\w.-]*)=(?!=)(.*)$", Pattern.DOTALL);

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private ClusterUpgradeTargetImageService clusterUpgradeTargetImageService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Mock
    private StackDto stack;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private StatedImage targetStatedImage;

    @Mock
    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage;

    @Mock
    private GatewayConfig primaryGatewayConfig;

    @InjectMocks
    private HuePythonVersionPinningPrerequisite underTest;

    @ParameterizedTest
    @ValueSource(strings = {"7.3.2", "7.3.2-1.cdh7.3.2.p100.123", "7.3.2-1.cdh7.3.2.p9999.123", "7.3.1-1.cdh7.3.1.p20000.123", "not-a-cdh-version"})
    void shouldSkipWhenTargetCdhVersionIsBelowThreshold(String cdhParcelVersion) throws Exception {
        mockTargetCdhParcel(cdhParcelVersion);

        underTest.execute(stack);

        verifyNoInteractions(cmTemplateProcessorFactory, gatewayConfigService, hostOrchestrator);
    }

    @Test
    void shouldProbeHueHostsWhenTheTargetImageIsNotAvailable() throws Exception {
        when(stack.getId()).thenReturn(STACK_ID);
        when(clusterUpgradeTargetImageService.findTargetImage(STACK_ID)).thenReturn(Optional.empty());
        mockHueHostGroupWithHosts(HOST_1);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1), Map.of(HOST_1, ""));

        assertDoesNotThrow(() -> underTest.execute(stack));

        verifyNoPinning();
    }

    @Test
    void shouldProbeHueHostsWhenTheTargetImageIsNotFoundInTheCatalog() throws Exception {
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(clusterUpgradeTargetImageService.findTargetImage(STACK_ID)).thenReturn(Optional.of(targetImage()));
        when(imageCatalogService.getImage(WORKSPACE_ID, CATALOG_URL, CATALOG_NAME, TARGET_IMAGE_ID))
                .thenThrow(new CloudbreakImageNotFoundException("image not found"));
        mockHueHostGroupWithHosts(HOST_1);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1), Map.of(HOST_1, ""));

        assertDoesNotThrow(() -> underTest.execute(stack));

        verifyNoPinning();
    }

    @Test
    void shouldProbeHueHostsWhenTheTargetImageHasNoCdhParcel() throws Exception {
        mockTargetImageWithProducts(Set.of(new ClouderaManagerProduct().withName("SPARK3").withVersion("3.3.0")));
        mockHueHostGroupWithHosts(HOST_1);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1), Map.of(HOST_1, ""));

        assertDoesNotThrow(() -> underTest.execute(stack));

        verifyNoPinning();
    }

    @Test
    void shouldSkipWhenThereIsNoHostGroupWithHueServer() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        when(stack.getBlueprintJsonText()).thenReturn(BLUEPRINT);
        when(cmTemplateProcessorFactory.get(BLUEPRINT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getHostGroupsWithComponent(HueRoles.HUE_SERVER)).thenReturn(Set.of());

        underTest.execute(stack);

        verifyNoInteractions(gatewayConfigService, hostOrchestrator);
    }

    @Test
    void shouldFailWhenThereIsNoReachableHostInTheHueHostGroup() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts();

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.execute(stack));

        assertTrue(exception.getMessage().contains("There is no reachable host in the Hue host group(s): " + HUE_HOST_GROUP), exception.getMessage());
        verifyNoInteractions(hostOrchestrator);
    }

    @Test
    void shouldNotPinWhenNoHueHostHasBrokenPython312() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts(HOST_1, HOST_2);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1, HOST_2), Map.of(HOST_1, "", HOST_2, ""));

        assertDoesNotThrow(() -> underTest.execute(stack));

        verifyNoPinning();
    }

    @Test
    void shouldFailWhenTheDetectionReturnsNoResultFromAHueHost() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts(HOST_1, HOST_2);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1, HOST_2), Map.of(HOST_1, ""));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.execute(stack));

        assertTrue(exception.getMessage().contains("The broken Python 3.12 detection returned no result from host(s): " + HOST_2), exception.getMessage());
        verifyNoPinning();
    }

    @Test
    void shouldFailWhenTheDetectionReturnsAnEmptyResult() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts(HOST_1);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1), Map.of());

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.execute(stack));

        assertTrue(exception.getMessage().contains("The broken Python 3.12 detection returned no result from host(s): " + HOST_1), exception.getMessage());
        verifyNoPinning();
    }

    @Test
    void shouldFailWithoutPinningWhenThereIsNoUsablePython311OnABrokenHost() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts(HOST_1);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1), Map.of(HOST_1, "broken"));
        mockCommandResult(DETECT_MISSING_PYTHON311_COMMAND, Set.of(HOST_1), Map.of(HOST_1, "missing"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.execute(stack));

        assertTrue(exception.getMessage().contains("there is no Python 3.11 with a working psycopg2 module on host(s): " + HOST_1), exception.getMessage());
        verifyNoPinning();
    }

    @Test
    void shouldFailWhenThePython311AvailabilityCheckReturnsNoResultFromABrokenHost() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts(HOST_1);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1), Map.of(HOST_1, "broken"));
        mockCommandResult(DETECT_MISSING_PYTHON311_COMMAND, Set.of(HOST_1), Map.of());

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.execute(stack));

        assertTrue(exception.getMessage().contains("The Python 3.11 availability check returned no result from host(s): " + HOST_1), exception.getMessage());
        verifyNoPinning();
    }

    @Test
    void shouldPinHueToPython311OnTheBrokenHost() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts(HOST_1);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1), Map.of(HOST_1, "broken"));
        mockCommandResult(DETECT_MISSING_PYTHON311_COMMAND, Set.of(HOST_1), Map.of(HOST_1, ""));
        mockCommandResult(PIN_HUE_PYTHON311_COMMAND, Set.of(HOST_1), Map.of(HOST_1, ""));

        assertDoesNotThrow(() -> underTest.execute(stack));

        verify(hostOrchestrator).runCommandOnHosts(List.of(primaryGatewayConfig), Set.of(HOST_1), PIN_HUE_PYTHON311_COMMAND,
                RetryType.WITH_2_SEC_DELAY_MAX_15_TIMES);
    }

    @Test
    void shouldOnlyPinTheBrokenHostWhenTheHueHostsAreMixed() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts(HOST_1, HOST_2);
        Map<String, String> detectionResult = new LinkedHashMap<>();
        detectionResult.put(HOST_1, "");
        detectionResult.put(HOST_2, "broken");
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1, HOST_2), detectionResult);
        mockCommandResult(DETECT_MISSING_PYTHON311_COMMAND, Set.of(HOST_2), Map.of(HOST_2, ""));
        mockCommandResult(PIN_HUE_PYTHON311_COMMAND, Set.of(HOST_2), Map.of(HOST_2, ""));

        assertDoesNotThrow(() -> underTest.execute(stack));

        verify(hostOrchestrator).runCommandOnHosts(List.of(primaryGatewayConfig), Set.of(HOST_2), PIN_HUE_PYTHON311_COMMAND,
                RetryType.WITH_2_SEC_DELAY_MAX_15_TIMES);
    }

    @Test
    void shouldOnlyProbeTheHostsOfTheHueHostGroups() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        when(stack.getBlueprintJsonText()).thenReturn(BLUEPRINT);
        when(cmTemplateProcessorFactory.get(BLUEPRINT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getHostGroupsWithComponent(HueRoles.HUE_SERVER)).thenReturn(Set.of(HUE_HOST_GROUP));
        List<InstanceGroupDto> instanceGroupDtos = List.of(instanceGroupDto(HUE_HOST_GROUP, HOST_1), instanceGroupDto(OTHER_HOST_GROUP, HOST_2));
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroupDtos);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1), Map.of(HOST_1, ""));

        assertDoesNotThrow(() -> underTest.execute(stack));

        verify(hostOrchestrator).runCommandOnHosts(List.of(primaryGatewayConfig), Set.of(HOST_1), DETECT_BROKEN_PYTHON312_COMMAND,
                RetryType.WITH_2_SEC_DELAY_MAX_15_TIMES);
        verifyNoPinning();
    }

    @Test
    void shouldFailWhenThePinningReturnsNoResultFromABrokenHost() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts(HOST_1);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1), Map.of(HOST_1, "broken"));
        mockCommandResult(DETECT_MISSING_PYTHON311_COMMAND, Set.of(HOST_1), Map.of(HOST_1, ""));
        mockCommandResult(PIN_HUE_PYTHON311_COMMAND, Set.of(HOST_1), Map.of());

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.execute(stack));

        assertTrue(exception.getMessage().contains("The Hue Python 3.11 pinning returned no result from host(s): " + HOST_1), exception.getMessage());
    }

    @Test
    void shouldFailWhenThePinningReportsAFailure() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts(HOST_1);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1), Map.of(HOST_1, "broken"));
        mockCommandResult(DETECT_MISSING_PYTHON311_COMMAND, Set.of(HOST_1), Map.of(HOST_1, ""));
        mockCommandResult(PIN_HUE_PYTHON311_COMMAND, Set.of(HOST_1), Map.of(HOST_1, "failed"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.execute(stack));

        assertTrue(exception.getMessage().contains("Pinning Hue to Python 3.11 failed on host(s): " + HOST_1), exception.getMessage());
    }

    @Test
    void shouldFailWhenTheDetectionCannotBeExecuted() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts(HOST_1);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(List.of(primaryGatewayConfig));
        when(hostOrchestrator.runCommandOnHosts(List.of(primaryGatewayConfig), Set.of(HOST_1), DETECT_BROKEN_PYTHON312_COMMAND,
                RetryType.WITH_2_SEC_DELAY_MAX_15_TIMES)).thenThrow(new CloudbreakOrchestratorFailedException("salt is unreachable"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.execute(stack));

        assertTrue(exception.getMessage().contains("The broken Python 3.12 detection could not be executed on all affected hosts"), exception.getMessage());
        assertTrue(exception.getMessage().contains("salt is unreachable"), exception.getMessage());
    }

    @Test
    void shouldFailWhenThePinningCannotBeExecuted() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts(HOST_1);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1), Map.of(HOST_1, "broken"));
        mockCommandResult(DETECT_MISSING_PYTHON311_COMMAND, Set.of(HOST_1), Map.of(HOST_1, ""));
        when(hostOrchestrator.runCommandOnHosts(List.of(primaryGatewayConfig), Set.of(HOST_1), PIN_HUE_PYTHON311_COMMAND,
                RetryType.WITH_2_SEC_DELAY_MAX_15_TIMES)).thenThrow(new CloudbreakOrchestratorFailedException("salt is unreachable"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.execute(stack));

        assertTrue(exception.getMessage().contains("The Hue Python 3.11 pinning could not be executed on all affected hosts"), exception.getMessage());
    }

    private void mockTargetCdhParcel(String cdhParcelVersion) throws Exception {
        mockTargetImageWithProducts(Set.of(new ClouderaManagerProduct().withName("CDH").withVersion(cdhParcelVersion)));
    }

    private void mockTargetImageWithProducts(Set<ClouderaManagerProduct> products) throws Exception {
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(clusterUpgradeTargetImageService.findTargetImage(STACK_ID)).thenReturn(Optional.of(targetImage()));
        when(imageCatalogService.getImage(WORKSPACE_ID, CATALOG_URL, CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(targetStatedImage);
        when(targetStatedImage.getImage()).thenReturn(targetCatalogImage);
        when(clouderaManagerProductTransformer.transform(targetCatalogImage, true, false)).thenReturn(products);
    }

    private void mockHueHostGroupWithHosts(String... fqdns) {
        when(stack.getBlueprintJsonText()).thenReturn(BLUEPRINT);
        when(cmTemplateProcessorFactory.get(BLUEPRINT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getHostGroupsWithComponent(HueRoles.HUE_SERVER)).thenReturn(Set.of(HUE_HOST_GROUP));
        List<InstanceGroupDto> instanceGroupDtos = List.of(instanceGroupDto(HUE_HOST_GROUP, fqdns));
        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroupDtos);
    }

    private void mockCommandResult(String command, Set<String> targetFqdns, Map<String, String> result) throws CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(List.of(primaryGatewayConfig));
        when(hostOrchestrator.runCommandOnHosts(List.of(primaryGatewayConfig), targetFqdns, command, RetryType.WITH_2_SEC_DELAY_MAX_15_TIMES))
                .thenReturn(result);
    }

    private void verifyNoPinning() throws CloudbreakOrchestratorFailedException {
        verify(hostOrchestrator, never()).runCommandOnHosts(anyList(), anySet(), eq(PIN_HUE_PYTHON311_COMMAND), any(RetryType.class));
    }

    private InstanceGroupDto instanceGroupDto(String groupName, String... fqdns) {
        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        when(instanceGroupView.getGroupName()).thenReturn(groupName);
        List<InstanceMetadataView> instanceMetadataViews = Arrays.stream(fqdns)
                .map(fqdn -> {
                    InstanceMetadataView instanceMetadataView = mock(InstanceMetadataView.class);
                    // lenient: the instances of a host group that does not run Hue are never looked at
                    lenient().when(instanceMetadataView.isReachable()).thenReturn(true);
                    lenient().when(instanceMetadataView.getDiscoveryFQDN()).thenReturn(fqdn);
                    return instanceMetadataView;
                })
                .toList();
        return new InstanceGroupDto(instanceGroupView, instanceMetadataViews);
    }

    private Image targetImage() {
        return new Image("imageName", Map.of(), "redhat8", "redhat8", "x86_64", CATALOG_URL, CATALOG_NAME, TARGET_IMAGE_ID, Map.of(), "2024-01-01", 1L,
                Map.of());
    }

    @Test
    void shouldFailWhenTheDetectionReturnsAnOutputThatIsNotTheExpectedToken() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts(HOST_1);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1), Map.of(HOST_1, "ERROR executing 'cmd.run': Passed invalid arguments"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.execute(stack));

        assertTrue(exception.getMessage().contains("The broken Python 3.12 detection returned an unexpected result"), exception.getMessage());
        assertTrue(exception.getMessage().contains("Passed invalid arguments"), exception.getMessage());
        verifyNoPinning();
    }

    @Test
    void shouldFailWhenThePython311AvailabilityCheckReturnsAnOutputThatIsNotTheExpectedToken() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts(HOST_1);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1), Map.of(HOST_1, "broken"));
        mockCommandResult(DETECT_MISSING_PYTHON311_COMMAND, Set.of(HOST_1), Map.of(HOST_1, "ERROR executing 'cmd.run': Passed invalid arguments"));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.execute(stack));

        assertTrue(exception.getMessage().contains("The Python 3.11 availability check returned an unexpected result"), exception.getMessage());
        assertTrue(exception.getMessage().contains("Passed invalid arguments"), exception.getMessage());
        verifyNoPinning();
    }

    @Test
    void shouldTolerateTheSurroundingWhitespaceOfTheReportedToken() throws Exception {
        mockTargetCdhParcel(AFFECTED_CDH_PARCEL);
        mockHueHostGroupWithHosts(HOST_1);
        mockCommandResult(DETECT_BROKEN_PYTHON312_COMMAND, Set.of(HOST_1), Map.of(HOST_1, " broken\n"));
        mockCommandResult(DETECT_MISSING_PYTHON311_COMMAND, Set.of(HOST_1), Map.of(HOST_1, ""));
        mockCommandResult(PIN_HUE_PYTHON311_COMMAND, Set.of(HOST_1), Map.of(HOST_1, ""));

        assertDoesNotThrow(() -> underTest.execute(stack));

        verify(hostOrchestrator).runCommandOnHosts(List.of(primaryGatewayConfig), Set.of(HOST_1), PIN_HUE_PYTHON311_COMMAND,
                RetryType.WITH_2_SEC_DELAY_MAX_15_TIMES);
    }

    @Test
    void shouldNotStartTheCommandsWithATokenThatSaltWouldParseAsAKeywordArgument() {
        for (String command : List.of(DETECT_BROKEN_PYTHON312_COMMAND, DETECT_MISSING_PYTHON311_COMMAND, PIN_HUE_PYTHON311_COMMAND)) {
            assertFalse(SALT_KWARG_PATTERN.matcher(command).matches(),
                    "The command is submitted as a positional salt-api arg of cmd.run and salt would turn it into a keyword argument: " + command);
        }
    }
}
