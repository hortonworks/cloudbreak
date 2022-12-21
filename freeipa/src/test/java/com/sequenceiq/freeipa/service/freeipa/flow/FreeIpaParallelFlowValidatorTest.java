package com.sequenceiq.freeipa.service.freeipa.flow;

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
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;
import com.sequenceiq.flow.service.FlowNameFormatService;
import com.sequenceiq.flow.service.TestFlowView;
import com.sequenceiq.freeipa.FreeIpaFlowInformation;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.CreateBindUserFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent;

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

    @ParameterizedTest
    @MethodSource
    public void testFlowAllowed(String selector, Set<FlowLogIdWithTypeAndTimestamp> runningFlows) {
        lenient().when(flowLogService.findAllRunningNonTerminationFlowsByResourceId(STACK_ID)).thenReturn(runningFlows);

        underTest.checkFlowAllowedToStart(selector, STACK_ID);
    }

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

    @ParameterizedTest
    @MethodSource
    public void testFlowNotAllowed(String selector, Set<FlowLogIdWithTypeAndTimestamp> runningFlows) {
        lenient().when(flowLogService.findAllRunningNonTerminationFlowsByResourceId(STACK_ID)).thenReturn(runningFlows);
        when(flowNameFormatService.formatFlows(anySet())).thenReturn("ForbiddenFlow");

        assertThrows(FlowsAlreadyRunningException.class, () -> underTest.checkFlowAllowedToStart(selector, STACK_ID));

        verify(flowNameFormatService, never()).formatFlowName(any());
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

    @Test
    public void testNewParallelFlows() {
        List<String> allowedParallelFlows = new FreeIpaFlowInformation().getAllowedParallelFlows();
        assertEquals(8, allowedParallelFlows.size(),
                "You have changed parallel flows for FreeIPA. Please make sure 'FreeIpaParallelFlowValidator' is adjusted if necessary");
        assertTrue(Set.of(
                        FreeIpaCleanupEvent.CLEANUP_EVENT.event(),
                        StackTerminationEvent.TERMINATION_EVENT.event(),
                        CreateBindUserFlowEvent.CREATE_BIND_USER_EVENT.event(),
                        SaltUpdateEvent.SALT_UPDATE_EVENT.event(),
                        ImageChangeEvents.IMAGE_CHANGE_EVENT.event(),
                        UpscaleFlowEvent.UPSCALE_EVENT.event(),
                        DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                        ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event()).containsAll(allowedParallelFlows),
                "You have changed parallel flows for FreeIPA. Please make sure 'FreeIpaParallelFlowValidator' is adjusted if necessary");
    }
}