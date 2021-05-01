package com.sequenceiq.cloudbreak.cloud.gcp.client;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

@ExtendWith(MockitoExtension.class)
public class GcpHttpClientConfigTest {

    @InjectMocks
    private GcpHttpClientConfig underTest;

    @Test
    public void testHttpTransport() throws GeneralSecurityException, IOException {
        NetHttpTransport expected = GoogleNetHttpTransport.newTrustedTransport();
        Assert.assertEquals(expected.getClass(), underTest.httpTransport().getClass());
    }

}