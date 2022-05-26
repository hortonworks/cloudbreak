package com.sequenceiq.cloudbreak.cluster.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.repository.ClusterComponentViewRepository;

@ExtendWith(MockitoExtension.class)
class ClusterComponentConfigProviderTest {

    private static final Long CLUSTER_ID = 1L;

    @Mock
    private ClusterComponentViewRepository componentViewRepository;

    @InjectMocks
    private ClusterComponentConfigProvider underTest;

    private String json = "{\"name\":\"CDH\",\"version\":\"7.2.15-1.cdh7.2.15.p1.26792553\",\"parcel\":\"http://build-cache.vpc.cloudera.com/s3/build/26792553/cdh/7.x/parcels/\"}";

    @Test
    void getNormalizedCdhProductFromRegularCdhVersion() {
        ClusterComponentView component = new ClusterComponentView();
        component.setComponentType(ComponentType.CDH_PRODUCT_DETAILS);
        component.setName("CDH");
        component.setAttributes(new Json(json));
        Set<ClusterComponentView> components = new HashSet<>();
        components.add(component);
        doReturn(components).when(componentViewRepository).findComponentViewsByClusterIdAndComponentType(any(), any());
        ClouderaManagerProduct product = underTest.getNormalizedCdhProductWithNormalizedVersion(CLUSTER_ID).get();
        assertEquals(product.getVersion(), "7.2.15");
    }
}