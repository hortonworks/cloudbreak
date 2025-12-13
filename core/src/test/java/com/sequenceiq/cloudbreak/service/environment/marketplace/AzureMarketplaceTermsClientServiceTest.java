package com.sequenceiq.cloudbreak.service.environment.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.api.v1.marketplace.endpoint.AzureMarketplaceTermsEndpoint;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsResponse;

@ExtendWith(MockitoExtension.class)
class AzureMarketplaceTermsClientServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:aa-00-bbb-111-ccc:environment:aa-00-bbb-111-ccc";

    private static final String INTERNAL_ACTOR = "crn:altus:iam:us-west-1:altus:user:__internal__actor__";

    @InjectMocks
    private AzureMarketplaceTermsClientService underTest;

    @Mock
    private AzureMarketplaceTermsEndpoint azureMarketplaceTermsEndpoint;

    @Test
    void getAccepted() {
        when(azureMarketplaceTermsEndpoint.getInAccount(any())).thenReturn(new AzureMarketplaceTermsResponse(Boolean.TRUE));
        assertTrue(ThreadBasedUserCrnProvider.doAs(INTERNAL_ACTOR, () -> underTest.getAccepted(ENVIRONMENT_CRN)));
    }

    @Test
    void getAcceptedThrowsException() {
        when(azureMarketplaceTermsEndpoint.getInAccount(any())).thenThrow(new WebApplicationException());
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> ThreadBasedUserCrnProvider.doAs(INTERNAL_ACTOR, () -> underTest.getAccepted(ENVIRONMENT_CRN)));
        assertEquals("Failed to GET Azure Marketplace Terms acceptance setting for account id: aa-00-bbb-111-ccc, due to: 'HTTP 500 Internal Server Error'",
                exception.getMessage());
    }

    @Test
    void getAcceptedNotFound() {
        when(azureMarketplaceTermsEndpoint.getInAccount(any())).thenThrow(new NotFoundException());
        assertFalse(ThreadBasedUserCrnProvider.doAs(INTERNAL_ACTOR, () -> underTest.getAccepted(ENVIRONMENT_CRN)));
    }
}
