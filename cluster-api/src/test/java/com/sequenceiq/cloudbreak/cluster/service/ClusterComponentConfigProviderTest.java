package com.sequenceiq.cloudbreak.cluster.service;

import static com.sequenceiq.cloudbreak.common.type.ComponentType.cdhClusterComponentName;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.cdhProductDetails;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.repository.ClusterComponentRepository;
import com.sequenceiq.cloudbreak.repository.ClusterComponentViewRepository;

@ExtendWith(MockitoExtension.class)
class ClusterComponentConfigProviderTest {

    private static final Long CLUSTER_ID = 1L;

    private static final long CLUSTER_COMPONENT_ID = 10L;

    private static final long CLUSTER_COMPONENT_REV_1 = 1L;

    private static final long CLUSTER_COMPONENT_REV_2 = 2L;

    @Mock
    private ClusterComponentViewRepository componentViewRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private TransactionService transactionService;

    @Mock
    private ClusterComponentRepository componentRepository;

    @InjectMocks
    private ClusterComponentConfigProvider underTest;

    @Mock
    private AuditReader auditReader;

    private String json = "{\"name\":\"CDH\",\"version\":\"7.2.15-1.cdh7.2.15.p1.26792553\"," +
            "\"parcel\":\"http://build-cache.vpc.cloudera.com/s3/build/26792553/cdh/7.x/parcels/\"}";

    @Test
    void getNormalizedCdhProductFromRegularCdhVersion() {
        ClusterComponentView component = new ClusterComponentView();
        component.setComponentType(cdhProductDetails());
        component.setName("CDH");
        component.setAttributes(new Json(json));
        Set<ClusterComponentView> components = new HashSet<>();
        components.add(component);
        doReturn(components).when(componentViewRepository).findComponentViewsByClusterIdAndComponentType(any(), any());
        ClouderaManagerProduct product = underTest.getNormalizedCdhProductWithNormalizedVersion(CLUSTER_ID).get();
        assertEquals(product.getVersion(), "7.2.15");
    }

    @Test
    void testRestorePreviousVersionWhenMultipleOlderVersionsThenHighestIsSelected() throws TransactionService.TransactionExecutionException {
        try (MockedStatic<AuditReaderFactory> auditReaderFactory = Mockito.mockStatic(AuditReaderFactory.class)) {
            ClusterComponent clusterComponent = createClusterComponent(CLUSTER_COMPONENT_ID);
            setupInvokeAuditReaderInTransaction(auditReaderFactory);
            when(auditReader.getRevisions(ClusterComponent.class, CLUSTER_COMPONENT_ID))
                    .thenReturn(List.of(CLUSTER_COMPONENT_REV_1, CLUSTER_COMPONENT_REV_2));
            ClusterComponent previousClusterComponent = createClusterComponent(CLUSTER_COMPONENT_ID);
            when(auditReader.find(ClusterComponent.class, CLUSTER_COMPONENT_ID, CLUSTER_COMPONENT_REV_2)).thenReturn(previousClusterComponent);

            underTest.restorePreviousVersion(clusterComponent);

            verify(componentRepository).save(previousClusterComponent);
        }
    }

    @Test
    void testRestorePreviousVersionWhenNoRevisionInfoThenNothingSaved() throws TransactionService.TransactionExecutionException {
        try (MockedStatic<AuditReaderFactory> auditReaderFactory = Mockito.mockStatic(AuditReaderFactory.class)) {
            ClusterComponent clusterComponent = createClusterComponent(CLUSTER_COMPONENT_ID);
            setupInvokeAuditReaderInTransaction(auditReaderFactory);
            when(auditReader.getRevisions(ClusterComponent.class, CLUSTER_COMPONENT_ID)).thenReturn(List.of());

            underTest.restorePreviousVersion(clusterComponent);

            verify(auditReader, never()).find(any(), anyLong(), anyLong());
            verify(componentRepository, never()).save(any());
        }
    }

    @Test
    void testDeleteComponentByClusterIdAndComponentType() {
        underTest.deleteClusterComponentByClusterIdAndComponentType(CLUSTER_ID, ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES);
        verify(componentRepository).deleteComponentByClusterIdAndComponentType(CLUSTER_ID, ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES);
    }

    @Test
    void getComponentShouldReturnAttributeWhenComponentAndAttributeArePresent() {
        ClouderaManagerProduct cdh = underTest.getComponent(
                Set.of(createClusterComponent("CDH")),
                ClouderaManagerProduct.class,
                cdhProductDetails(),
                cdhClusterComponentName()
        );

        assertNotNull(cdh);
        assertTrue(cdh.getName().equals(cdhClusterComponentName()));
    }

    @Test
    void getComponentShouldReturnAttributeWhenComponentAndAttributeArePresentAndMoreComponentIsThere() {
        ClouderaManagerProduct cdh = underTest.getComponent(
                Set.of(
                        createClusterComponent("EFM"),
                        createClusterComponent("FLINK"),
                        createClusterComponent("CDH")
                ),
                ClouderaManagerProduct.class,
                cdhProductDetails(),
                cdhClusterComponentName()
        );

        assertNotNull(cdh);
        assertTrue(cdh.getName().equals(cdhClusterComponentName()));
    }

    @Test
    void getComponentShouldReturnAttributeWhenComponentAndAttributeAreNOTPresentAndMoreComponentIsThere() {
        ClouderaManagerProduct cdh = underTest.getComponent(
                Set.of(
                        createClusterComponent("EFM"),
                        createClusterComponent("FLINK"),
                        createClusterComponent("NIFI")
                ),
                ClouderaManagerProduct.class,
                cdhProductDetails(),
                cdhClusterComponentName()
        );

        assertNull(cdh);
    }

    private ClusterComponent createClusterComponent(String name) {
        String json = "{\"name\":\"%s\",\"version\":\"7.2.15-1.cdh7.2.15.p1.26792553\"," +
                "\"parcel\":\"http://build-cache.vpc.cloudera.com/s3/build/26792553/cdh/7.x/parcels/\"}";
        ClusterComponent clusterComponent = new ClusterComponent();
        clusterComponent.setAttributes(new Json(String.format(json, name)));
        clusterComponent.setName(name);
        clusterComponent.setComponentType(ComponentType.CDH_PRODUCT_DETAILS);
        clusterComponent.setCluster(new Cluster());
        return clusterComponent;
    }

    private ClusterComponent createClusterComponent(Long id) {
        ClusterComponent cc = new ClusterComponent();
        cc.setId(id);
        return cc;
    }

    private void setupInvokeAuditReaderInTransaction(MockedStatic<AuditReaderFactory> auditReaderFactory)
            throws TransactionService.TransactionExecutionException {
        auditReaderFactory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
        doAnswer((Answer<Void>) invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
    }

}
