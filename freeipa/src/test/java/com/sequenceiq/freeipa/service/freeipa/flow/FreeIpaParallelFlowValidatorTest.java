package com.sequenceiq.freeipa.service.freeipa.flow;

import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.MODIFY_SELINUX_START_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeEvent;
import com.sequenceiq.flow.core.chain.init.config.FlowChainInitEvent;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;
import com.sequenceiq.flow.service.FlowNameFormatService;
import com.sequenceiq.flow.service.TestFlowView;
import com.sequenceiq.freeipa.FreeIpaFlowInformation;
import com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.CreateBindUserFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent;
import com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents;
import com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationEvent;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent;
import com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector;

@ExtendWith(MockitoExtension.class)
class FreeIpaParallelFlowValidatorTest {

    private static final Set<Class<?>> ALLOWED_PARALLEL_FLOW_CONFIG = Set.of(
            CreateBindUserFlowConfig.class,
            FreeIpaCleanupFlowConfig.class);

    private static final Long STACK_ID = 1L;

    private static final String COMMON_FLOW_EVENT = "testEvent";

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private FlowNameFormatService flowNameFormatService;

    @InjectMocks
    private FreeIpaParallelFlowValidator underTest;

    private static Stream<Arguments> testFlowAllowed() {
        Stream<Arguments> dynamicFlowConfigTest = getAllFlowConfig().stream().flatMap(flowConfig ->
                Stream.of(Arguments.of(FreeIpaCleanupEvent.CLEANUP_EVENT.event(), createFromFlowConfig(Set.of(flowConfig))),
                        Arguments.of(StackTerminationEvent.TERMINATION_EVENT.event(), createFromFlowConfig(Set.of(flowConfig))),
                        Arguments.of(CreateBindUserFlowEvent.CREATE_BIND_USER_EVENT.event(), createFromFlowConfig(Set.of(flowConfig))))
        );
        Stream<Arguments> staticFlowConfigTest = Stream.of(
                Arguments.of(FreeIpaCleanupEvent.CLEANUP_EVENT.event(), createFromFlowConfig(Set.of(UpscaleFlowConfig.class))),
                Arguments.of(StackTerminationEvent.TERMINATION_EVENT.event(), createFromFlowConfig(Set.of(UpscaleFlowConfig.class))),
                Arguments.of(CreateBindUserFlowEvent.CREATE_BIND_USER_EVENT.event(), createFromFlowConfig(Set.of(UpscaleFlowConfig.class))),
                Arguments.of(COMMON_FLOW_EVENT, createFromFlowConfig(Set.of(CreateBindUserFlowConfig.class))),
                Arguments.of(COMMON_FLOW_EVENT, createFromFlowConfig(Set.of(FreeIpaCleanupFlowConfig.class))),
                Arguments.of(COMMON_FLOW_EVENT, createFromFlowConfig(Set.of(CreateBindUserFlowConfig.class, FreeIpaCleanupFlowConfig.class))),
                Arguments.of(COMMON_FLOW_EVENT, createFromFlowConfig(Set.of()))
        );
        return Stream.concat(staticFlowConfigTest, dynamicFlowConfigTest);
    }

    private static Stream<Arguments> testFlowNotAllowed() {
        Stream<Arguments> dynamicFlowConfigTest = getAllFlowConfig().stream()
                .filter(config -> !ALLOWED_PARALLEL_FLOW_CONFIG.contains(config))
                .map(config -> Arguments.of(COMMON_FLOW_EVENT, createFromFlowConfig(Set.of(config))));
        Stream<Arguments> staticFlowConfigTest = Stream.of(
                Arguments.of(COMMON_FLOW_EVENT, createFromFlowConfig(Set.of(CreateBindUserFlowConfig.class, UpscaleFlowConfig.class))),
                Arguments.of(COMMON_FLOW_EVENT, createFromFlowConfig(Set.of(FreeIpaCleanupFlowConfig.class, UpscaleFlowConfig.class))),
                Arguments.of(COMMON_FLOW_EVENT, createFromFlowConfig(Set.of(CreateBindUserFlowConfig.class, FreeIpaCleanupFlowConfig.class,
                        UpscaleFlowConfig.class)))
        );
        return Stream.concat(staticFlowConfigTest, dynamicFlowConfigTest);
    }

    private static Set<Class<? extends AbstractFlowConfiguration>> getAllFlowConfig() {
        Reflections reflections = new Reflections("com.sequenceiq.freeipa.flow", new SubTypesScanner());
        return reflections.getSubTypesOf(AbstractFlowConfiguration.class);
    }

    private static Set<FlowLogIdWithTypeAndTimestamp> createFromFlowConfig(Collection<Class<?>> clazz) {
        return clazz.stream().map(FreeIpaParallelFlowValidatorTest::createFromFlowConfig).collect(Collectors.toSet());
    }

    private static FlowLogIdWithTypeAndTimestamp createFromFlowConfig(Class<?> clazz) {
        return new TestFlowView(clazz);
    }

    @ParameterizedTest
    @MethodSource
    public void testFlowAllowed(String selector, Set<FlowLogIdWithTypeAndTimestamp> runningFlows) {
        lenient().when(flowLogService.findAllRunningFlowsByResourceId(STACK_ID)).thenReturn(runningFlows);

        underTest.checkFlowAllowedToStart(selector, STACK_ID);
    }

    @ParameterizedTest
    @MethodSource
    public void testFlowNotAllowed(String selector, Set<FlowLogIdWithTypeAndTimestamp> runningFlows) {
        lenient().when(flowLogService.findAllRunningFlowsByResourceId(STACK_ID)).thenReturn(runningFlows);
        when(flowNameFormatService.formatFlows(anySet())).thenReturn("ForbiddenFlow");

        assertThrows(FlowsAlreadyRunningException.class, () -> underTest.checkFlowAllowedToStart(selector, STACK_ID));

        verify(flowNameFormatService, never()).formatFlowName(any());
    }

    @Test
    public void testNewParallelFlows() {
        List<String> allowedParallelFlows = new FreeIpaFlowInformation().getAllowedParallelFlows();
        assertEquals(21, allowedParallelFlows.size(),
                "You have changed parallel flows for FreeIPA. Please make sure 'FreeIpaParallelFlowValidator' is adjusted if necessary");
        assertTrue(Set.of(
                        RefreshEntitlementParamsEvent.REFRESH_ENTITLEMENT_PARAMS_TRIGGER_EVENT.event(),
                        FreeIpaCleanupEvent.CLEANUP_EVENT.event(),
                        StackTerminationEvent.TERMINATION_EVENT.event(),
                        CreateBindUserFlowEvent.CREATE_BIND_USER_EVENT.event(),
                        SaltUpdateEvent.SALT_UPDATE_EVENT.event(),
                        ImageChangeEvents.IMAGE_CHANGE_EVENT.event(),
                        UpscaleFlowEvent.UPSCALE_EVENT.event(),
                        DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                        ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(),
                        RebootEvent.REBOOT_EVENT.event(),
                        FullBackupEvent.FULL_BACKUP_EVENT.event(),
                        DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_SALT_VALIDATION_EVENT.event(),
                        FreeIpaVerticalScaleEvent.STACK_VERTICALSCALE_EVENT.event(),
                        AwsVariantMigrationEvent.CREATE_RESOURCES_EVENT.event(),
                        UpdateUserDataEvents.UPDATE_USERDATA_TRIGGER_EVENT.event(),
                        UpgradeCcmStateSelector.UPGRADE_CCM_TRIGGER_EVENT.event(),
                        ModifyProxyConfigEvent.MODIFY_PROXY_TRIGGER_EVENT.event(),
                        FlowChainInitEvent.FLOWCHAIN_INIT_TRIGGER_EVENT.event(),
                        FlowChainFinalizeEvent.FLOWCHAIN_FINALIZE_TRIGGER_EVENT.event(),
                        FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT.event(),
                        MODIFY_SELINUX_START_EVENT.event()).containsAll(allowedParallelFlows),
                "You have changed parallel flows for FreeIPA. Please make sure 'FreeIpaParallelFlowValidator' is adjusted if necessary");
    }
}