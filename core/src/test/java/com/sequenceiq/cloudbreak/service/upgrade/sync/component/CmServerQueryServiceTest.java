package com.sequenceiq.cloudbreak.service.upgrade.sync.component;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static com.sequenceiq.cloudbreak.cluster.model.ParcelStatus.ACTIVATED;
import static com.sequenceiq.cloudbreak.cluster.model.ParcelStatus.DISTRIBUTED;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@ExtendWith(MockitoExtension.class)
public class CmServerQueryServiceTest {

    private static final String STACK_NAME = "stackName";

    private static final String CDH_PARCEL_NAME = "CDH";

    private static final String CDH_PARCEL_VERSION = "7.2.7-1.cdh7.2.7.p7.12569826";

    @InjectMocks
    private CmServerQueryService underTest;

    @Mock
    private ClusterApiConnectors apiConnectors;

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
    void testGetActiveParcelsWhenParcelReturned() {
        ParcelInfo parcel = new ParcelInfo(CDH_PARCEL_NAME, CDH_PARCEL_VERSION, ACTIVATED);
        when(clusterApi.gatherInstalledParcels(STACK_NAME)).thenReturn(Collections.singleton(parcel));

        Set<ParcelInfo> foundParcels = underTest.queryActiveParcels(stack);

        assertThat(foundParcels, hasSize(1));
        ParcelInfo activeParcel = foundParcels.iterator().next();
        assertEquals(CDH_PARCEL_NAME, activeParcel.getName());
        assertEquals(CDH_PARCEL_VERSION, activeParcel.getVersion());
    }

    @Test
    void testGetActiveParcelsWhenNoParcelReturnedThenEmptyList() {
        when(clusterApi.gatherInstalledParcels(STACK_NAME)).thenReturn(Collections.emptySet());

        Set<ParcelInfo> foundParcels = underTest.queryActiveParcels(stack);

        assertThat(foundParcels, hasSize(0));
    }

    @Test
    void testQueryAllParcelsShouldReturnAllParcel() {
        Set<ParcelInfo> parcels = Set.of(new ParcelInfo(CDH_PARCEL_NAME, CDH_PARCEL_VERSION, ACTIVATED), new ParcelInfo("NIFI", "123", DISTRIBUTED));
        when(clusterApi.getAllParcels(STACK_NAME)).thenReturn(parcels);

        Set<ParcelInfo> actual = underTest.queryAllParcels(stack);

        assertEquals(parcels, actual);
    }

    @Test
    void testQueryAllParcelsShouldReturnEmptySetWhenClusterApiDoesNotReturnAnyParcels() {
        when(clusterApi.getAllParcels(STACK_NAME)).thenReturn(Collections.emptySet());

        Set<ParcelInfo> actual = underTest.queryAllParcels(stack);

        assertEquals(Collections.emptySet(), actual);
    }

}
