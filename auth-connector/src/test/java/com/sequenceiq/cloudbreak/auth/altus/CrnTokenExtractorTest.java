package com.sequenceiq.cloudbreak.auth.altus;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.springframework.security.core.Authentication;

import com.google.common.collect.Iterators;

public class CrnTokenExtractorTest {

    private String exampleCrn = "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private CrnTokenExtractor classUnderTest = new CrnTokenExtractor();

    @Test
    public void whenNoValidCrnIsPresentedExtractBearerToken() {
        Set<String> mySet = new HashSet<>();
        mySet.add("Bearer abcd");
        Enumeration<String> headers = Iterators.asEnumeration(mySet.iterator());
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(CrnTokenExtractor.CRN_HEADER)).thenReturn("dps@gmail.com");
        when(request.getHeaders("Authorization")).thenReturn(headers);
        Authentication auth = classUnderTest.extract(request);
        assertEquals("abcd", auth.getPrincipal());
    }

    @Test
    public void whenValidCrnIsPresentedExtractCrnToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(CrnTokenExtractor.CRN_HEADER)).thenReturn(exampleCrn);
        Authentication auth = classUnderTest.extract(request);
        assertEquals(exampleCrn, auth.getPrincipal());
    }
}