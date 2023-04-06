package com.sequenceiq.cloudbreak.service.environment.marketplace;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.api.v1.marketplace.endpoint.AzureMarketplaceTermsEndpoint;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsResponse;

@ExtendWith(MockitoExtension.class)
class AzureMarketplaceTermsClientServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final String INTERNAL_ACTOR = "crn:altus:iam:us-west-1:altus:user:__internal__actor__";

    @InjectMocks
    private AzureMarketplaceTermsClientService underTest;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private AzureMarketplaceTermsEndpoint azureMarketplaceTermsEndpoint;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @BeforeEach
    void setUp() {
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_ACTOR);
    }

    @Test
    void getAccepted() {
        when(azureMarketplaceTermsEndpoint.getInAccount(any())).thenReturn(new AzureMarketplaceTermsResponse(Boolean.TRUE));
        assertTrue(ThreadBasedUserCrnProvider.doAs(INTERNAL_ACTOR, () -> underTest.getAccepted()));
    }

    @Test
    void getAcceptedThrowsException() {
        when(azureMarketplaceTermsEndpoint.getInAccount(any())).thenThrow(new WebApplicationException());
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> ThreadBasedUserCrnProvider.doAs(INTERNAL_ACTOR, () -> underTest.getAccepted()));
        assertEquals("Failed to GET Azure Marketplace Terms acceptance setting for account id: altus, due to: 'HTTP 500 Internal Server Error'",
                exception.getMessage());
    }

    @Test
    void getAcceptedNotFound() {
        when(azureMarketplaceTermsEndpoint.getInAccount(any())).thenThrow(new NotFoundException());
        assertFalse(ThreadBasedUserCrnProvider.doAs(INTERNAL_ACTOR, () -> underTest.getAccepted()));
    }
}