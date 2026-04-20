package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationHandlerSelectors.REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.FINISH_REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationFailureEvent;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class MigrateZookeeperToKraftRemoveUnusedZookeeperHandlerTest {

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 2L;

    private static final String ELIGIBLE_RUNTIME_VERSION = "7.3.2-10000";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterView clusterView;

    @InjectMocks
    private MigrateZookeeperToKraftRemoveUnusedZookeeperHandler underTest;

    @Test
    void skipsDeletionWhenBlueprintNotEligible() throws Exception {
        StackDto stackDto = new StackDto();
        ReflectionTestUtils.setField(stackDto, "cluster", clusterView);
        MigrateZookeeperToKraftFinalizationEvent request =
                new MigrateZookeeperToKraftFinalizationEvent(REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFinalizationEvent.class, result);
        assertEquals(FINISH_REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.name(), result.getSelector());
        verifyNoInteractions(clusterApiConnectors);
        verifyNoInteractions(clusterComponentConfigProvider);
    }

    @Test
    void skipsDeletionForOtherBlueprint() throws Exception {
        StackDto stackDto = stackWithBlueprintName("7.3.2 - Data Engineering");
        MigrateZookeeperToKraftFinalizationEvent request =
                new MigrateZookeeperToKraftFinalizationEvent(REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFinalizationEvent.class, result);
        assertEquals(FINISH_REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.name(), result.getSelector());
        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    void skipsDeletionWhenRuntimeVersionIsNot732() throws Exception {
        mockRuntimeVersion("7.3.1-10000");
        MigrateZookeeperToKraftFinalizationEvent request =
                new MigrateZookeeperToKraftFinalizationEvent(REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackWithBlueprintName("7.3.2 - Streams Messaging Light Duty: Apache Kafka"));

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFinalizationEvent.class, result);
        assertEquals(FINISH_REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.name(), result.getSelector());
        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    void skipsDeletionWhenPatchVersionIsLessThan10000() throws Exception {
        mockRuntimeVersion("7.3.2-0");
        MigrateZookeeperToKraftFinalizationEvent request =
                new MigrateZookeeperToKraftFinalizationEvent(REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackWithBlueprintName("7.3.2 - Streams Messaging Light Duty: Apache Kafka"));

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFinalizationEvent.class, result);
        assertEquals(FINISH_REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.name(), result.getSelector());
        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    void deletesZookeeperWhenPatchVersionIsHigherThan10000() throws Exception {
        StackDto stackDto = stackWithBlueprintName("7.3.2 - Streams Messaging Light Duty: Apache Kafka");
        mockRuntimeVersion("7.3.2-10001");
        MigrateZookeeperToKraftFinalizationEvent request =
                new MigrateZookeeperToKraftFinalizationEvent(REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFinalizationEvent.class, result);
        assertEquals(FINISH_REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.name(), result.getSelector());
        InOrder inOrder = inOrder(clusterApi);
        inOrder.verify(clusterApi).stopClouderaManagerService("ZOOKEEPER", true);
        inOrder.verify(clusterApi).deleteClouderaManagerService("ZOOKEEPER");
    }

    @Test
    void skipsDeletionWhenRuntimeVersionIsMissing() throws Exception {
        when(clusterView.getId()).thenReturn(CLUSTER_ID);
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(Optional.empty());
        MigrateZookeeperToKraftFinalizationEvent request =
                new MigrateZookeeperToKraftFinalizationEvent(REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackWithBlueprintName("7.3.2 - Streams Messaging Light Duty: Apache Kafka"));

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFinalizationEvent.class, result);
        assertEquals(FINISH_REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.name(), result.getSelector());
        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    void deletesZookeeperForStreamsMessagingBlueprint() throws Exception {
        deletesZookeeperWhenBlueprintContains(stackWithBlueprintName("7.3.2 - Streams Messaging Light Duty: Apache Kafka"));
    }

    @Test
    void deletesZookeeperForHybridStreamsMessagingBlueprint() throws Exception {
        deletesZookeeperWhenBlueprintContains(stackWithBlueprintName("7.3.2 - Hybrid Streams Messaging Light Duty: Apache Kafka"));
    }

    @Test
    void skipsDeletionForStreamingAnalyticsBlueprint() throws Exception {
        StackDto stackDto = stackWithBlueprintName("7.3.2 - Streaming Analytics Light Duty with Apache Flink");
        MigrateZookeeperToKraftFinalizationEvent request =
                new MigrateZookeeperToKraftFinalizationEvent(REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFinalizationEvent.class, result);
        assertEquals(FINISH_REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.name(), result.getSelector());
        verifyNoInteractions(clusterApiConnectors);
    }

    private void deletesZookeeperWhenBlueprintContains(StackDto stackDto) throws Exception {
        mockEligibleRuntimeVersion();
        MigrateZookeeperToKraftFinalizationEvent request =
                new MigrateZookeeperToKraftFinalizationEvent(REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFinalizationEvent.class, result);
        assertEquals(FINISH_REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.name(), result.getSelector());
        InOrder inOrder = inOrder(clusterApi);
        inOrder.verify(clusterApi).stopClouderaManagerService("ZOOKEEPER", true);
        inOrder.verify(clusterApi).deleteClouderaManagerService("ZOOKEEPER");
    }

    @Test
    void failureWhenDeleteThrows() throws Exception {
        StackDto stackDto = stackWithBlueprintName("7.3.2 - Streams Messaging Light Duty: Apache Kafka");
        mockEligibleRuntimeVersion();
        MigrateZookeeperToKraftFinalizationEvent request =
                new MigrateZookeeperToKraftFinalizationEvent(REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        doThrow(new RuntimeException("cm error")).when(clusterApi).deleteClouderaManagerService("ZOOKEEPER");

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFinalizationFailureEvent.class, result);
        assertEquals(FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), result.getSelector());
        InOrder inOrder = inOrder(clusterApi);
        inOrder.verify(clusterApi).stopClouderaManagerService("ZOOKEEPER", true);
        inOrder.verify(clusterApi).deleteClouderaManagerService("ZOOKEEPER");
    }

    @Test
    void failureWhenStopThrows() throws Exception {
        StackDto stackDto = stackWithBlueprintName("7.3.2 - Streams Messaging Light Duty: Apache Kafka");
        mockEligibleRuntimeVersion();
        MigrateZookeeperToKraftFinalizationEvent request =
                new MigrateZookeeperToKraftFinalizationEvent(REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        doThrow(new RuntimeException("stop failed")).when(clusterApi).stopClouderaManagerService("ZOOKEEPER", true);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFinalizationFailureEvent.class, result);
        verify(clusterApi).stopClouderaManagerService("ZOOKEEPER", true);
        verify(clusterApi, never()).deleteClouderaManagerService("ZOOKEEPER");
    }

    private void mockEligibleRuntimeVersion() {
        mockRuntimeVersion(ELIGIBLE_RUNTIME_VERSION);
    }

    private void mockRuntimeVersion(String version) {
        when(clusterView.getId()).thenReturn(CLUSTER_ID);
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct().withName("CDH").withVersion(version);
        when(clusterComponentConfigProvider.getCdhProduct(CLUSTER_ID)).thenReturn(Optional.of(cdhProduct));
    }

    private StackDto stackWithBlueprintName(String name) {
        Blueprint blueprint = new Blueprint();
        blueprint.setName(name);
        StackDto stackDto = new StackDto();
        ReflectionTestUtils.setField(stackDto, "blueprint", blueprint);
        ReflectionTestUtils.setField(stackDto, "cluster", clusterView);
        return stackDto;
    }
}
