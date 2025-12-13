package com.sequenceiq.environment.environment.service.domain;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.exception.EnvironmentServiceException;

@ExtendWith(MockitoExtension.class)
class PemBasedEnvironmentDomainProviderTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:123:user:123";

    @Mock
    private DnsManagementService dnsManagementService;

    @InjectMocks
    private PemBasedEnvironmentDomainProvider underTest;

    @Test
    void testGenerateShouldThrowServiceExceptionWhenPemReturnsWithNullAsManagedDomainName() {
        Environment environment = new Environment();
        String anEnvName = "anEnvName";
        environment.setName(anEnvName);
        when(dnsManagementService.generateManagedDomain(anyString(), eq(anEnvName))).thenReturn(null);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                assertThrows(EnvironmentServiceException.class, () -> underTest.generate(environment)));

        verify(dnsManagementService, times(1)).generateManagedDomain(anyString(), eq(anEnvName));
    }

    @Test
    void testGenerateShouldThrowServiceExceptionWhenPemReturnsWithEmptyStringAsManagedDomainName() {
        Environment environment = new Environment();
        String anEnvName = "anEnvName";
        environment.setName(anEnvName);
        when(dnsManagementService.generateManagedDomain(anyString(), eq(anEnvName))).thenReturn("");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                assertThrows(EnvironmentServiceException.class, () -> underTest.generate(environment)));

        verify(dnsManagementService, times(1)).generateManagedDomain(anyString(), eq(anEnvName));
    }

    @Test
    void testGenerateShouldThrowServiceExceptionWhenPemReturnsWithThrowExceptionDuringGenerationOfManagedDomainName() {
        Environment environment = new Environment();
        String anEnvName = "anEnvName";
        environment.setName(anEnvName);
        when(dnsManagementService.generateManagedDomain(anyString(), eq(anEnvName)))
                .thenThrow(new RuntimeException("ooo something went wrong, no domain generation from our side."));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                assertThrows(EnvironmentServiceException.class, () -> underTest.generate(environment)));

        verify(dnsManagementService, times(1)).generateManagedDomain(anyString(), eq(anEnvName));
    }

    @Test
    void testGenerateReturnManagedDomainWhenPemReturnsTheGeneratedManagedDomainName() {
        Environment environment = new Environment();
        String anEnvName = "anEnvName";
        environment.setName(anEnvName);
        String expectedDomain = anEnvName + ".mydomain.cldr";
        when(dnsManagementService.generateManagedDomain(anyString(), eq(anEnvName))).thenReturn(expectedDomain);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.generate(environment));

        assertEquals(expectedDomain, result);
        verify(dnsManagementService, times(1)).generateManagedDomain(anyString(), eq(anEnvName));
    }
}