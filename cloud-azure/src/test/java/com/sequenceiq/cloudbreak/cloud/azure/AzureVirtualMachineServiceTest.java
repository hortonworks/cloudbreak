package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.rest.RestException;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;

@RunWith(MockitoJUnitRunner.class)
public class AzureVirtualMachineServiceTest {

    private static final String INSTANCE_1 = "instance-1";

    private static final String INSTANCE_2 = "instance-2";

    private static final String INSTANCE_3 = "instance-3";

    private static final String RESOURCE_GROUP = "resource-group";

    @InjectMocks
    private AzureVirtualMachineService underTest;

    @Mock
    private AzureClient azureClient;

    @Test
    public void testGetVirtualMachinesByIdShouldReturnTheVirtualMachines() {
        Set<String> privateInstanceIds = createPrivateInstanceIds();
        PagedList<VirtualMachine> virtualMachines = createPagedList();

        when(azureClient.getVirtualMachines(RESOURCE_GROUP)).thenReturn(virtualMachines);

        Map<String, VirtualMachine> actual = underTest.getVirtualMachinesByName(azureClient, RESOURCE_GROUP, privateInstanceIds);

        assertEquals(INSTANCE_1, actual.get(INSTANCE_1).name());
        assertEquals(INSTANCE_2, actual.get(INSTANCE_2).name());
        assertEquals(INSTANCE_3, actual.get(INSTANCE_3).name());
    }

    @Test
    public void testGetVirtualMachinesByIdShouldReturnOneVirtualMachinesWhenOnlyTheFirstPageAvailable() {
        Set<String> privateInstanceIds = createPrivateInstanceIds();
        PagedList<VirtualMachine> virtualMachinesWithOnePage = createPagedListWithOnePage();

        when(azureClient.getVirtualMachines(RESOURCE_GROUP)).thenReturn(virtualMachinesWithOnePage);

        Map<String, VirtualMachine> actual = underTest.getVirtualMachinesByName(azureClient, RESOURCE_GROUP, privateInstanceIds);

        assertEquals(INSTANCE_1, actual.get(INSTANCE_1).name());
        assertEquals(1, actual.size());
    }

    private PagedList<VirtualMachine> createPagedList() {
        PagedList<VirtualMachine> pagedList = new PagedList<>() {
            @Override
            public Page<VirtualMachine> nextPage(String nextPageLink) throws RestException {
                return null;
            }
        };
        pagedList.add(createVirtualMachine(INSTANCE_1));
        pagedList.add(createVirtualMachine(INSTANCE_2));
        pagedList.add(createVirtualMachine(INSTANCE_3));
        return pagedList;
    }

    private PagedList<VirtualMachine> createPagedListWithOnePage() {
        PagedList<VirtualMachine> pagedList = new PagedList<>() {
            @Override
            public Page<VirtualMachine> nextPage(String nextPageLink) throws RestException {
                return null;
            }

            @Override
            public void loadNextPage() {
            }

            @Override
            public boolean hasNextPage() {
                return false;
            }
        };
        pagedList.add(createVirtualMachine(INSTANCE_1));
        return pagedList;
    }

    private VirtualMachine createVirtualMachine(String vmId) {
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        when(virtualMachine.name()).thenReturn(vmId);
        return virtualMachine;
    }

    private Set<String> createPrivateInstanceIds() {
        return Set.of(INSTANCE_1, INSTANCE_2, INSTANCE_3);
    }

}