package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class CmParcelInfoRetrieverServiceTest {

    private static final String STACK_NAME = "stackName";

    private static final long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterApiConnectors apiConnectors;

    @InjectMocks
    private CmParcelInfoRetrieverService underTest;

    @Mock
    private Stack stack;

    @Mock
    private ClusterApi clusterApi;

    @BeforeEach
    void setup() {
        when(stack.getName()).thenReturn(STACK_NAME);
        when(apiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
    }

    @Test
    void testGetActiveParcelsWhenParcelReturnedThenParsedIntoParcelInfo() {
        Map<String, String> parcelNameToVersionMap = Map.of("CDH", "7.2.7-1.cdh7.2.7.p7.12569826");
        when(clusterApi.gatherInstalledParcels(STACK_NAME)).thenReturn(parcelNameToVersionMap);

        List<ParcelInfo> foundParcels = underTest.getActiveParcelsFromServer(STACK_ID);

        assertThat(foundParcels, hasSize(1));
        assertEquals("CDH", foundParcels.get(0).getName());
        assertEquals("7.2.7-1.cdh7.2.7.p7.12569826", foundParcels.get(0).getVersion());
    }

    @Test
    void testGetActiveParcelsWhenNoParcelReturnedThenEmptyList() {
        Map<String, String> parcelNameToVersionMap = Map.of();
        when(clusterApi.gatherInstalledParcels(STACK_NAME)).thenReturn(parcelNameToVersionMap);

        List<ParcelInfo> foundParcels = underTest.getActiveParcelsFromServer(STACK_ID);

        assertThat(foundParcels, hasSize(0));
    }

}
