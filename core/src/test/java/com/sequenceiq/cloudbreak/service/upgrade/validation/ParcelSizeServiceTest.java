package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;

import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

class ParcelSizeServiceTest {

    private ParcelSizeService underTest = new ParcelSizeService();

    @Test
    void testGetAllParcelSizeShouldReturnTheSizeOfTheParcels() {
        Response response1 = mock(Response.class);
        when(response1.getHeaderString(CONTENT_LENGTH)).thenReturn("10000");
        Response response2 = mock(Response.class);
        when(response2.getHeaderString(CONTENT_LENGTH)).thenReturn("10000");

        long actual = underTest.getRequiredFreeSpace(Set.of(response1, response2));

        assertEquals(3145747L, actual);
    }

    @Test
    void testGetAllParcelSizeShouldReturnTheSizeOfTheParcelsWhenAContentLengthIsNotAvailable() {
        Response response1 = mock(Response.class);
        when(response1.getHeaderString(CONTENT_LENGTH)).thenReturn("10000");
        Response response2 = mock(Response.class);

        long actual = underTest.getRequiredFreeSpace(Set.of(response1, response2));

        assertEquals(3145737L, actual);
    }

}
