package com.sequenceiq.cloudbreak.service.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.component.PreparedImages;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.ClusterComponentUpdateService;

@ExtendWith(MockitoExtension.class)
public class ClusterComponentUpdateServiceTest {

    private static final String COMPONENT_NAME = "COMPONENT_NAME";

    private static final Json ATTRIBUTE_JSON = new Json(new PreparedImages(List.of("image-id")));

    private static final Long CLUSTER_ID = 1L;

    private static final long STACK_ID = 1L;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ClusterService clusterService;

    @InjectMocks
    private ClusterComponentUpdateService underTest;

    @Test
    public void testPreparedClusterComponentUpdate() throws IOException {
        doReturn(Optional.of(CLUSTER_ID)).when(clusterService).findClusterIdByStackId(STACK_ID);
        Cluster mockCluster = mock(Cluster.class);
        doReturn(CLUSTER_ID).when(mockCluster).getId();
        ClusterComponent clusterComponent = new ClusterComponent();
        clusterComponent.setCluster(mockCluster);
        clusterComponent.setAttributes(ATTRIBUTE_JSON);
        doReturn(clusterComponent).when(clusterComponentConfigProvider).getComponent(CLUSTER_ID,
                ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES, ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES.name());
        doReturn(clusterComponent).when(clusterComponentConfigProvider).store(any(ClusterComponent.class));

        underTest.updateOrSavePreparedClusterComponent(STACK_ID, "image-id1");
        Json expectedAttribute = new Json(Map.of("preparedImages", List.of("image-id", "image-id1")));
        clusterComponent.setAttributes(expectedAttribute);

        verify(clusterComponentConfigProvider).store(clusterComponent);
    }

    @Test
    public void testPreparedClusterComponentCreate() throws IOException {
        doReturn(Optional.of(CLUSTER_ID)).when(clusterService).findClusterIdByStackId(STACK_ID);
        doReturn(null).when(clusterComponentConfigProvider).getComponent(STACK_ID,
                ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES, ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES.name());
        doReturn(new ClusterComponent()).when(clusterComponentConfigProvider).store(any(ClusterComponent.class));

        underTest.updateOrSavePreparedClusterComponent(STACK_ID, "image-id");

        verify(clusterService).getClusterReference(STACK_ID);
    }
}
