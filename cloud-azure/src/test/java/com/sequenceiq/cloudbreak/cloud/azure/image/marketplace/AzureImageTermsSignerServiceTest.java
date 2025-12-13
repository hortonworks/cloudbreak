package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.rest.AzureRestOperationsService;
import com.sequenceiq.cloudbreak.cloud.azure.rest.AzureRestResponseException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@ExtendWith(MockitoExtension.class)
public class AzureImageTermsSignerServiceTest {

    private static final String ACCESS_TOKEN = "accessToken";

    private static final String AZURE_SUBSCRIPTION_ID = "azureSubscriptionId";

    @Mock
    private AzureRestOperationsService azureRestOperationsService;

    @InjectMocks
    private AzureImageTermsSignerService underTest;

    @Mock
    private AzureClient azureClient;

    private final AzureMarketplaceImage azureMarketplaceImage = new AzureMarketplaceImage("cloudera", "my-offer", "my-plan", "my-version");

    static Object[][] exceptionsFromHttpMethod() {
        return new Object[][]{
                {new AzureRestResponseException("myMessage", new HttpClientErrorException(HttpStatus.PAYMENT_REQUIRED)), "myMessage"},
                {new RestClientException("myMessage"), "myMessage"},
                {new AzureRestResponseException("myMessage"), "myMessage"}
        };
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testIsSignedReturnsSignedFromRestResponseBody(boolean signedFromRestResponse) {
        when(azureClient.getAccessToken()).thenReturn(Optional.of(ACCESS_TOKEN));
        when(azureRestOperationsService.httpGet(any(), any(), anyString())).thenReturn(setupAzureImageTerms(signedFromRestResponse));

        boolean signedFromService = underTest.getImageTermStatus(AZURE_SUBSCRIPTION_ID, azureMarketplaceImage, azureClient).getAsBoolean();

        assertEquals(signedFromService, signedFromRestResponse);
        verify(azureClient).getAccessToken();
        verify(azureRestOperationsService).httpGet(any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
    }

    @Test
    void testIsSignedWhenNoToken() {
        when(azureClient.getAccessToken()).thenReturn(Optional.empty());

        Exception exception = assertThrows(CloudConnectorException.class,
                () -> underTest.getImageTermStatus(AZURE_SUBSCRIPTION_ID, azureMarketplaceImage, azureClient));

        assertEquals("Error when retrieving if marketplace image terms and conditions are signed for " +
                        "AzureMarketplaceImage{publisherId='cloudera', offerId='my-offer', planId='my-plan', version='my-version'}. " +
                        "Reason: could not get access token. Please try again.",
                exception.getMessage());
        verify(azureClient).getAccessToken();
    }

    @Test
    void testIsSignedWhenGetTermsThrowsError() {
        when(azureClient.getAccessToken()).thenReturn(Optional.of(ACCESS_TOKEN));
        when(azureRestOperationsService.httpGet(any(), any(), anyString())).thenThrow(new RestClientException("myMessage"));

        Exception exception = assertThrows(CloudConnectorException.class,
                () -> underTest.getImageTermStatus(AZURE_SUBSCRIPTION_ID, azureMarketplaceImage, azureClient));

        assertEquals("Error when retrieving if marketplace image terms and conditions are signed for " +
                        "AzureMarketplaceImage{publisherId='cloudera', offerId='my-offer', planId='my-plan', version='my-version'}. " +
                        "Reason: myMessage. Please try again.",
                exception.getMessage());
        verify(azureClient).getAccessToken();
        verify(azureRestOperationsService).httpGet(any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
    }

    @Test
    void testIsSignedWhenGetTermsThrowsPermissionError() {
        when(azureClient.getAccessToken()).thenReturn(Optional.of(ACCESS_TOKEN));
        when(azureRestOperationsService.httpGet(any(), any(), anyString())).thenThrow(new AzureRestResponseException("Missing permission"));

        AzureImageTermStatus imageTermStatus = underTest.getImageTermStatus(AZURE_SUBSCRIPTION_ID, azureMarketplaceImage, azureClient);

        assertEquals(AzureImageTermStatus.NON_READABLE, imageTermStatus);
        verify(azureClient).getAccessToken();
        verify(azureRestOperationsService).httpGet(any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
    }

    @Test
    void testSignHappyPath() {
        when(azureClient.getAccessToken()).thenReturn(Optional.of(ACCESS_TOKEN));
        AzureImageTerms azureImageTerms = setupAzureImageTerms();
        when(azureRestOperationsService.httpGet(any(), any(), anyString())).thenReturn(azureImageTerms);
        when(azureRestOperationsService.httpPut(any(), any(), any(), anyString())).thenReturn(azureImageTerms);

        underTest.sign(AZURE_SUBSCRIPTION_ID, azureMarketplaceImage, azureClient);

        verify(azureClient).getAccessToken();
        ArgumentCaptor<URI> argumentCaptor = ArgumentCaptor.forClass(URI.class);
        InOrder inOrder = inOrder(azureRestOperationsService);
        inOrder.verify(azureRestOperationsService).httpGet(any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
        inOrder.verify(azureRestOperationsService).httpPut(argumentCaptor.capture(), any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
        URI signURI = argumentCaptor.getValue();
        assertEquals("https://management.azure.com/subscriptions/azureSubscriptionId/providers/Microsoft.MarketplaceOrdering/" +
                "offerTypes/virtualmachine/publishers/cloudera/offers/my-offer/plans/my-plan/agreements/current?api-version=2021-01-01", signURI.toString());
    }

    @Test
    void testSignWhenNoToken() {
        when(azureClient.getAccessToken()).thenReturn(Optional.empty());

        Exception exception = assertThrows(CloudConnectorException.class,
                () -> underTest.sign(AZURE_SUBSCRIPTION_ID, azureMarketplaceImage, azureClient));

        assertEquals("Error when signing marketplace image terms and conditions for " +
                "AzureMarketplaceImage{publisherId='cloudera', offerId='my-offer', planId='my-plan', version='my-version'}. " +
                "Reason: could not get access token. Please try again. " +
                "Alternatively you can also sign it manually, please refer to Azure documentation at " +
                "https://docs.microsoft.com/en-us/cli/azure/vm/image/terms?view=azure-cli-latest. " +
                "If it still does not work, please open a Microsoft Azure support ticket!", exception.getMessage());
        verify(azureClient).getAccessToken();
        verify(azureRestOperationsService, never()).httpGet(any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
        verify(azureRestOperationsService, never()).httpPut(any(), any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
    }

    @ParameterizedTest
    @MethodSource("exceptionsFromHttpMethod")
    void testSignWhenGetTermsThrowsException(Exception restException, String customErrorMessage) {
        when(azureClient.getAccessToken()).thenReturn(Optional.of(ACCESS_TOKEN));
        when(azureRestOperationsService.httpGet(any(), any(), anyString())).thenThrow(restException);

        Exception exception = assertThrows(CloudConnectorException.class,
                () -> underTest.sign(AZURE_SUBSCRIPTION_ID, azureMarketplaceImage, azureClient));

        assertEquals(String.format("Error when signing marketplace image terms and conditions for " +
                        "AzureMarketplaceImage{publisherId='cloudera', offerId='my-offer', planId='my-plan', version='my-version'}. " +
                        "Reason: error when signing vm image terms and conditions, message is '%s'. Please try again. " +
                        "Alternatively you can also sign it manually, please refer to Azure documentation at " +
                        "https://docs.microsoft.com/en-us/cli/azure/vm/image/terms?view=azure-cli-latest. " +
                        "If it still does not work, please open a Microsoft Azure support ticket!", customErrorMessage),
                exception.getMessage());
        verify(azureClient).getAccessToken();
        verify(azureRestOperationsService).httpGet(any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
        verify(azureRestOperationsService, never()).httpPut(any(), any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
    }

    @ParameterizedTest
    @MethodSource("exceptionsFromHttpMethod")
    void testSignWhenPutTermsThrowsException(Exception restException, String customErrorMessage) {
        when(azureClient.getAccessToken()).thenReturn(Optional.of(ACCESS_TOKEN));
        AzureImageTerms azureImageTerms = setupAzureImageTerms();
        when(azureRestOperationsService.httpGet(any(), any(), anyString())).thenReturn(azureImageTerms);
        when(azureRestOperationsService.httpPut(any(), any(), any(), anyString())).thenThrow(restException);

        Exception exception = assertThrows(CloudConnectorException.class,
                () -> underTest.sign(AZURE_SUBSCRIPTION_ID, azureMarketplaceImage, azureClient));

        assertEquals(String.format("Error when signing marketplace image terms and conditions for " +
                        "AzureMarketplaceImage{publisherId='cloudera', offerId='my-offer', planId='my-plan', version='my-version'}. " +
                        "Reason: error when signing vm image terms and conditions, message is '%s'. Please try again. " +
                        "Alternatively you can also sign it manually, please refer to Azure documentation at " +
                        "https://docs.microsoft.com/en-us/cli/azure/vm/image/terms?view=azure-cli-latest. " +
                        "If it still does not work, please open a Microsoft Azure support ticket!", customErrorMessage),
                exception.getMessage());
        verify(azureClient).getAccessToken();
        verify(azureRestOperationsService).httpGet(any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
        verify(azureRestOperationsService).httpPut(any(), any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
    }

    @Test
    void testSignWhenGetTermsThrowsForbiddenException() {
        when(azureClient.getAccessToken()).thenReturn(Optional.of(ACCESS_TOKEN));
        when(azureRestOperationsService.httpGet(any(), any(), anyString())).thenThrow(
                new AzureRestResponseException("myMessage", new HttpClientErrorException(HttpStatus.FORBIDDEN)));

        AzureImageTermStatus actual = underTest.sign(AZURE_SUBSCRIPTION_ID, azureMarketplaceImage, azureClient);

        assertEquals(AzureImageTermStatus.NON_READABLE, actual);
        verify(azureClient).getAccessToken();
        verify(azureRestOperationsService).httpGet(any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
        verify(azureRestOperationsService, never()).httpPut(any(), any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
    }

    @Test
    void testSignWhenPutTermsThrowsForbiddenException() {
        when(azureClient.getAccessToken()).thenReturn(Optional.of(ACCESS_TOKEN));
        when(azureRestOperationsService.httpGet(any(), any(), anyString())).thenThrow(
                new AzureRestResponseException("myMessage", new HttpClientErrorException(HttpStatus.FORBIDDEN)));

        AzureImageTermStatus actual = underTest.sign(AZURE_SUBSCRIPTION_ID, azureMarketplaceImage, azureClient);

        assertEquals(AzureImageTermStatus.NON_READABLE, actual);
        verify(azureClient).getAccessToken();
        verify(azureRestOperationsService).httpGet(any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
        verify(azureRestOperationsService, never()).httpPut(any(), any(), eq(AzureImageTerms.class), eq(ACCESS_TOKEN));
    }

    private AzureImageTerms setupAzureImageTerms() {
        return setupAzureImageTerms(false);
    }

    private AzureImageTerms setupAzureImageTerms(boolean signed) {
        AzureImageTerms azureImageTerms = new AzureImageTerms();
        AzureImageTerms.TermsProperties termProperties = new AzureImageTerms.TermsProperties();
        termProperties.setAccepted(signed);
        azureImageTerms.setProperties(termProperties);
        return azureImageTerms;
    }
}