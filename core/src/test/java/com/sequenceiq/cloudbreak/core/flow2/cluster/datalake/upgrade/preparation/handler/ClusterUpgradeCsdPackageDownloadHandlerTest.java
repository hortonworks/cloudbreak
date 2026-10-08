package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.core.cluster.ClouderaManagerCsdDownloaderService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeCsdPackageDownloadHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private ClouderaManagerCsdDownloaderService downloader;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackDto stack;

    @InjectMocks
    private ClusterUpgradeCsdPackageDownloadHandler underTest;

    @Test
    void testDownloadPreservesPropertiesAndProducts() throws Exception {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        Set<ClouderaManagerProduct> products = Set.of(new ClouderaManagerProduct().withName("FLINK"));
        ClusterUpgradePreparationEvent request = new ClusterUpgradePreparationEvent("selector", STACK_ID, products, properties.targetImageId(), properties);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);

        ClusterUpgradePreparationEvent result = (ClusterUpgradePreparationEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        verify(downloader).downloadCsdFiles(stack, true, products, true);
        assertThat(result.getClusterUpgradeProperties()).isSameAs(properties);
        assertThat(result.getClouderaManagerProducts()).isSameAs(products);
        assertThat(result.getSelector()).isEqualTo("FINISH_CLUSTER_UPGRADE_PREPARATION_EVENT");
    }

    @Test
    void testDownloadFailure() throws Exception {
        Set<ClouderaManagerProduct> products = Set.of();
        ClusterUpgradePreparationEvent request = new ClusterUpgradePreparationEvent("selector", STACK_ID, products, "target", null);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        RuntimeException failure = new RuntimeException("download failed");
        doThrow(failure).when(downloader).downloadCsdFiles(stack, true, products, true);

        assertThat(underTest.doAccept(new HandlerEvent<>(new Event<>(request))))
                .isInstanceOfSatisfying(ClusterUpgradePreparationFailureEvent.class, result -> assertThat(result.getException()).isSameAs(failure));
    }
}
