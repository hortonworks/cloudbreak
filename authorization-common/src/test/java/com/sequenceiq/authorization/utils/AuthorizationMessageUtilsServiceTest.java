package com.sequenceiq.authorization.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.service.ResourceNameFactoryService;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationMessageUtilsServiceTest {

    @Mock
    private ResourceNameFactoryService resourceNameFactoryService;

    @InjectMocks
    private AuthorizationMessageUtilsService underTest;

    @Test
    public void testFormatTemplate() {
        when(resourceNameFactoryService.getNames(any())).thenAnswer(i -> {
            String arg = ((Collection<String>) i.getArgument(0)).iterator().next();
            return Map.of(arg, Optional.of("RESOURCE_NAME"));
        });
        String formatted = underTest.formatTemplate("environments/describeCredential",
                "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:638e0ee7-6b1d-4127-9297-7b16d50d5a96");
        assertEquals("You have insufficient rights to perform the following action(s): " +
                "'environments/describeCredential' on a(n) 'environment' type resource with resource identifier: " +
                "[Name: 'RESOURCE_NAME', " +
                "Crn: 'crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:638e0ee7-6b1d-4127-9297-7b16d50d5a96']", formatted);
    }

    @Test
    public void testFormatTemplateNullResource() {
        String formatted = underTest.formatTemplate("environments/describeCredential", (String) null);
        assertEquals("You have insufficient rights to perform the following action(s): " +
                "'environments/describeCredential' on a(n) 'unknown' type resource with resource identifier: " +
                "['account']", formatted);
    }

    @Test
    public void testFormatTemplateWithNoName() {
        when(resourceNameFactoryService.getNames(any())).thenAnswer(i -> {
            String arg = ((Collection<String>) i.getArgument(0)).iterator().next();
            return Map.of(arg, Optional.empty());
        });
        String formatted = underTest.formatTemplate("environments/describeCredential",
                "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:638e0ee7-6b1d-4127-9297-7b16d50d5a96");
        assertEquals("You have insufficient rights to perform the following action(s): " +
                "'environments/describeCredential' on a(n) 'environment' type resource with resource identifier: " +
                "[Crn: 'crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:638e0ee7-6b1d-4127-9297-7b16d50d5a96']", formatted);
    }

    @Test
    public void testFormatTemplateWithBadCrn() {
        when(resourceNameFactoryService.getNames(any())).thenAnswer(i -> {
            String arg = ((Collection<String>) i.getArgument(0)).iterator().next();
            return Map.of(arg, Optional.empty());
        });
        String formatted = underTest.formatTemplate("environments/describeCredential",
                "BAD_CRN");
        assertEquals("You have insufficient rights to perform the following action(s): " +
                "'environments/describeCredential' on a(n) 'unknown' type resource with resource identifier: [Crn: 'BAD_CRN']", formatted);
    }
}