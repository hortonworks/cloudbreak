package com.sequenceiq.cloudbreak.service.upgrade.sync.component;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;

@ExtendWith(MockitoExtension.class)
public class CmServerQueryServiceTest {

    private static final String STACK_NAME = "stackName";

    @Mock
    private ClusterApiConnectors apiConnectors;

    @InjectMocks
    private CmServerQueryService underTest;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private Stack stack;

    @BeforeEach
    void setup() {
        when(stack.getName()).thenReturn(STACK_NAME);
        when(apiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
    }

    @Test
    void testGetActiveParcelsWhenParcelReturnedThenParsedIntoParcelInfo() {
        Map<String, String> parcelNameToVersionMap = Map.of("CDH", "7.2.7-1.cdh7.2.7.p7.12569826");
        when(clusterApi.gatherInstalledParcels(STACK_NAME)).thenReturn(parcelNameToVersionMap);

        Set<ParcelInfo> foundParcels = underTest.queryActiveParcels(stack);

        assertThat(foundParcels, hasSize(1));
        ParcelInfo activeParcel = foundParcels.iterator().next();
        assertEquals("CDH", activeParcel.getName());
        assertEquals("7.2.7-1.cdh7.2.7.p7.12569826", activeParcel.getVersion());
    }

    @Test
    void testGetActiveParcelsWhenNoParcelReturnedThenEmptyList() {
        Map<String, String> parcelNameToVersionMap = Map.of();
        when(clusterApi.gatherInstalledParcels(STACK_NAME)).thenReturn(parcelNameToVersionMap);

        Set<ParcelInfo> foundParcels = underTest.queryActiveParcels(stack);

        assertThat(foundParcels, hasSize(0));
    }

}
