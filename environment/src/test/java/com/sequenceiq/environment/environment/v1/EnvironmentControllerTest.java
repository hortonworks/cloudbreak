package com.sequenceiq.environment.environment.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentCreationService;
import com.sequenceiq.environment.environment.service.EnvironmentUpgradeCcmService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentApiConverter;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;

@ExtendWith(MockitoExtension.class)
class EnvironmentControllerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final Set<String> SUBNETS = Set.of("subnet1", "subnet2");

    @Mock
    private EnvironmentApiConverter environmentApiConverter;

    @Mock
    private EnvironmentCreationService environmentCreationService;

    @Mock
    private EnvironmentResponseConverter environmentResponseConverter;

    @Mock
    private EnvironmentUpgradeCcmService upgradeCcmService;

    @InjectMocks
    private EnvironmentController underTest;

    @Test
    void testEndpointGatewayOptionsPreserved() {
        EnvironmentNetworkRequest networkRequest = setupNetworkRequestWithEndpointGatway();
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setNetwork(networkRequest);

        setupServiceResponses();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.post(environmentRequest));

        assertEquals(PublicEndpointAccessGateway.ENABLED, networkRequest.getPublicEndpointAccessGateway());
        assertEquals(SUBNETS, networkRequest.getEndpointGatewaySubnetIds());
    }

    @Test
    void testUpgradeCcmByNameCallsService() {
        underTest.upgradeCcmByName("name123");
        verify(upgradeCcmService).upgradeCcmByName("name123");
    }

    @Test
    void testUpgradeCcmByCrnCallsService() {
        underTest.upgradeCcmByCrn("crn123");
        verify(upgradeCcmService).upgradeCcmByCrn("crn123");
    }

    private EnvironmentNetworkRequest setupNetworkRequestWithEndpointGatway() {
        EnvironmentNetworkRequest networkRequest = new EnvironmentNetworkRequest();
        networkRequest.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        networkRequest.setEndpointGatewaySubnetIds(SUBNETS);
        return networkRequest;
    }

    private void setupServiceResponses() {
        when(environmentApiConverter.initCreationDto(any())).thenReturn(EnvironmentCreationDto.builder().build());
        when(environmentCreationService.create(any())).thenReturn(EnvironmentDto.builder().build());
        when(environmentResponseConverter.dtoToDetailedResponse(any())).thenReturn(new DetailedEnvironmentResponse());
    }
}
