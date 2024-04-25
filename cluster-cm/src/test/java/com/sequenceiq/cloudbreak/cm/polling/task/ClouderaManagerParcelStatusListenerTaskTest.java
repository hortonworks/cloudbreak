package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerParcelStatusListenerTaskTest {

    private static final String STACK_NAME = "stack-name";

    @Mock
    private ClouderaManagerCommandPollerObject pollerObject;

    @Mock
    private ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    @Mock
    private ClusterEventService clusterEventService;

    @Mock
    private Multimap<String, String> parcelVersions;

    @Mock
    private ApiClient apiClient;

    @Mock
    private StackDtoDelegate stack;

    @Mock
    private ParcelsResourceApi parcelsResourceApi;

    private ClouderaManagerParcelStatusListenerTask underTest;

    @BeforeEach
    void before() {
        underTest = new ClouderaManagerParcelStatusListenerTask(clouderaManagerApiPojoFactory,
                clusterEventService, parcelVersions, ParcelStatus.ACTIVATED);
        when(pollerObject.getApiClient()).thenReturn(apiClient);
        when(pollerObject.getStack()).thenReturn(stack);
        when(stack.getName()).thenReturn(STACK_NAME);
        when(clouderaManagerApiPojoFactory.getParcelsResourceApi(apiClient)).thenReturn(parcelsResourceApi);
    }

    @Test
    void testDoStatusCheckShouldReturnTrueIfAllParcelReachedTheDesiredState() throws ApiException {
        ApiParcelList parcelList = new ApiParcelList()
                .items(List.of(createParcel("CDH", "123", ParcelStatus.ACTIVATED.name()), createParcel("CFM", "124", ParcelStatus.ACTIVATED.name())));
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);

        assertTrue(underTest.doStatusCheck(pollerObject));
    }

    @Test
    void testDoStatusCheckShouldReturnFalseIfOneParcelDoesNotReachedTheDesiredState() throws ApiException {
        ApiParcelList parcelList = new ApiParcelList()
                .items(List.of(createParcel("CDH", "123", ParcelStatus.ACTIVATED.name()), createParcel("CFM", "124", ParcelStatus.DOWNLOADED.name())));
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);

        assertTrue(underTest.doStatusCheck(pollerObject));
    }

    @Test
    void testDoStatusCheckShouldReturnTrueOneAllParcelReachedTheDesiredStateAndOneIsInUnavailableState() throws ApiException {
        ApiParcelList parcelList = new ApiParcelList()
                .items(List.of(createParcel("CDH", "123", ParcelStatus.ACTIVATED.name()), createParcel("CFM", "124", ParcelStatus.UNAVAILABLE.name())));
        when(parcelsResourceApi.readParcels(STACK_NAME, "summary")).thenReturn(parcelList);

        assertTrue(underTest.doStatusCheck(pollerObject));
    }

    private ApiParcel createParcel(String name, String version, String stage) {
        return new ApiParcel().product(name).version(version).stage(stage);
    }
}