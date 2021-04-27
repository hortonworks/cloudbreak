package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@ExtendWith(MockitoExtension.class)
public class AzureImageTermsSignerServiceTest {

    private static final String ACCESS_TOKEN = "accessToken";

    @Mock
    private AzureRestOperationsService azureRestOperationsService;

    @InjectMocks
    private AzureImageTermsSignerService underTest;

    @Mock
    private AzureClient azureClient;

    private AzureMarketplaceImage azureMarketplaceImage = new AzureMarketplaceImage("cloudera", "my-offer", "my-plan", "my-version");

    @BeforeEach
    void setup() {

    }

    @Test
    void testSignHappyPath() {
        when(azureClient.getAccessToken()).thenReturn(Optional.of(ACCESS_TOKEN));
        AzureImageTerms azureImageTerms = setupAzureImageTerms();
        when(azureRestOperationsService.httpGet(any(), any(), anyString())).thenReturn(azureImageTerms);
        when(azureRestOperationsService.httpPut(any(), any(), any(), anyString())).thenReturn(azureImageTerms);

        underTest.sign("subscriptionId", azureMarketplaceImage, azureClient);

        verify(azureClient).getAccessToken();
        ArgumentCaptor<AzureImageTerms> argumentCaptor = ArgumentCaptor.forClass(AzureImageTerms.class);
        InOrder inOrder = Mockito.inOrder(azureRestOperationsService);
        inOrder.verify(azureRestOperationsService).httpGet(any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
        inOrder.verify(azureRestOperationsService).httpPut(any(), argumentCaptor.capture(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
        AzureImageTerms azureImageTermsWithAccept = argumentCaptor.getValue();
        assertTrue(azureImageTermsWithAccept.getProperties().isAccepted());
    }

    @Test
    void testSignWhenNoToken() {
        when(azureClient.getAccessToken()).thenReturn(Optional.empty());
        AzureImageTerms azureImageTerms = setupAzureImageTerms();

        Exception exception = Assertions.assertThrows(CloudConnectorException.class,
                () -> underTest.sign("subscriptionId", azureMarketplaceImage, azureClient));

        assertEquals("Could not get access token when trying to sign terms and conditions.", exception.getMessage());
        verify(azureClient).getAccessToken();
        ArgumentCaptor<AzureImageTerms> argumentCaptor = ArgumentCaptor.forClass(AzureImageTerms.class);
        verify(azureRestOperationsService, never()).httpGet(any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
        verify(azureRestOperationsService, never()).httpPut(any(), any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
    }

//    @Test
//    void testSignWhenGetTermsIsNot2xx() {
//        when(azureClient.getAccessToken()).thenReturn(Optional.empty());
//        AzureImageTerms azureImageTerms = setupAzureImageTerms();
//
//        Exception exception = Assertions.assertThrows(CloudConnectorException.class,
//                () -> underTest.sign("subscriptionId", azureMarketplaceImage, azureClient));
//
//        assertEquals("Could not get access token when trying to sign terms and conditions.", exception.getMessage());
//        verify(azureClient).getAccessToken();
//        ArgumentCaptor<AzureImageTerms> argumentCaptor = ArgumentCaptor.forClass(AzureImageTerms.class);
//        verify(azureRestOperationsService, never()).httpGet(any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
//        verify(azureRestOperationsService, never()).httpPut(any(), any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
//    }

    private AzureImageTerms setupAzureImageTerms() {
        AzureImageTerms azureImageTerms = new AzureImageTerms();
        AzureImageTerms.TermsProperties termProperties = new AzureImageTerms.TermsProperties();
        termProperties.setAccepted(false);
        azureImageTerms.setProperties(termProperties);
        return azureImageTerms;
    }
}
