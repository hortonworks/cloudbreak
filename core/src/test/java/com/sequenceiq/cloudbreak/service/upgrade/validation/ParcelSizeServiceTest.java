package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;

import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ParcelSizeServiceTest {

    @InjectMocks
    private ParcelSizeService underTest;

    @Test
    public void testGetAllParcelSizeShouldReturnTheSizeOfTheParcels() {
        Response response1 = Mockito.mock(Response.class);
        when(response1.getHeaderString(CONTENT_LENGTH)).thenReturn("10000");
        Response response2 = Mockito.mock(Response.class);
        when(response2.getHeaderString(CONTENT_LENGTH)).thenReturn("10000");

        long actual = underTest.getRequiredFreeSpace(Set.of(response1, response2));

        assertEquals(3145747L, actual);
    }

    @Test
    public void testGetAllParcelSizeShouldReturnTheSizeOfTheParcelsWhenAContentLengthIsNotAvailable() {
        Response response1 = Mockito.mock(Response.class);
        when(response1.getHeaderString(CONTENT_LENGTH)).thenReturn("10000");
        Response response2 = Mockito.mock(Response.class);

        long actual = underTest.getRequiredFreeSpace(Set.of(response1, response2));

        assertEquals(3145737L, actual);
    }

}
