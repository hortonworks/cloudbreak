package com.sequenceiq.cloudbreak.service.publicendpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.dns.LegacyEnvironmentNameBasedDomainNameProvider;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class EnvironmentBasedDomainNameProviderTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:ac5ba74b-c35e-45e9-9f47-123456789876";

    @Mock
    private LegacyEnvironmentNameBasedDomainNameProvider legacyEnvironmentNameBasedDomainNameProvider;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @InjectMocks
    private EnvironmentBasedDomainNameProvider underTest;

    @Test
    void testGetCommonNameWhenTheEndpointNameIsLessThan17AndEnvNameLessThan8Chars() {
        String endpointName = "test-cl-master0";
        String envName = "shrt-nv";
        String accountName = "xcu2-8y8x";
        String rootDomain = "wl.cloudera.site";
        String environmentDomain = envName + "." + accountName + "." + rootDomain;
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();

        String commonName = underTest.getCommonName(endpointName, environment);

        String expected = String.format("9fd767c45167db77.%s", environmentDomain);
        assertEquals(expected, commonName);
        verifyNoInteractions(grpcUmsClient);
        verifyNoInteractions(legacyEnvironmentNameBasedDomainNameProvider);
    }

    @Test
    void testGetCommonNameWhenTheEndpointNameIsLongerThan17AndEnvNameLongerThan8CharsAndEnvironmentDomainIsNotPresentShouldUseLegacyDomainProvider() {
        String endpointName = "test-cl-longyloooooooooooong-name-master0";
        String envName = "notashort-env-name-as28chars";
        String rootDomain = "wl.cloudera.site";
        String environmentDomain = envName + "." + rootDomain;
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withCrn(ENV_CRN)
                .build();
        String accountWorkloadSubdomain = "aWorkloadSubdomain";
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
                .setWorkloadSubdomain(accountWorkloadSubdomain)
                .build();
        when(grpcUmsClient.getAccountDetails(anyString(), any())).thenReturn(umsAccount);
        when(legacyEnvironmentNameBasedDomainNameProvider.getDomainName(anyString(), anyString())).thenReturn(environmentDomain);

        String commonName = underTest.getCommonName(endpointName, environment);

        String expected = String.format("5e8a4beefa5d7f90.%s", environmentDomain);
        assertEquals(expected, commonName);
        verify(grpcUmsClient, times(1)).getAccountDetails(anyString(), any());
        verify(legacyEnvironmentNameBasedDomainNameProvider, times(1)).getDomainName(anyString(), anyString());
    }
}