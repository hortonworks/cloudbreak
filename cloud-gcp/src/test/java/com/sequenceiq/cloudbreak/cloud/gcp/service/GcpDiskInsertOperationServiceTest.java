package com.sequenceiq.cloudbreak.cloud.gcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskInsertOperationService.GcpDiskInsertOutcome;

@ExtendWith(MockitoExtension.class)
class GcpDiskInsertOperationServiceTest {

    private static final String PROJECT_ID = "test-project";

    private static final String ZONE = "us-central1-a";

    private static final String DISK_NAME = "d0";

    @InjectMocks
    private GcpDiskInsertOperationService underTest;

    @Mock
    private Compute compute;

    @Mock
    private Compute.Disks disks;

    @Mock
    private Compute.Disks.Get get;

    @Mock
    private Compute.Disks.Insert insert;

    @Test
    void testInsertsWhenDiskIsAbsent() throws Exception {
        Disk disk = new Disk().setName(DISK_NAME);
        when(compute.disks()).thenReturn(disks);
        when(disks.get(eq(PROJECT_ID), eq(ZONE), eq(DISK_NAME))).thenReturn(get);
        when(get.execute()).thenThrow(googleException(HttpStatus.SC_NOT_FOUND, "Not Found"));
        when(disks.insert(eq(PROJECT_ID), eq(ZONE), eq(disk))).thenReturn(insert);
        when(insert.execute()).thenReturn(new Operation().setName("op-1"));

        GcpDiskInsertOutcome outcome = underTest.insertDiskIfAbsent(compute, PROJECT_ID, ZONE, disk, DISK_NAME);

        assertTrue(outcome.operation().isPresent());
        assertEquals("op-1", outcome.operation().get().getName());
        assertTrue(outcome.existingDisk().isEmpty());
    }

    @Test
    void testReturnsExistingDiskWithoutInsertingWhenPresent() throws Exception {
        Disk disk = new Disk().setName(DISK_NAME);
        Disk existing = new Disk().setName(DISK_NAME);
        when(compute.disks()).thenReturn(disks);
        when(disks.get(eq(PROJECT_ID), eq(ZONE), eq(DISK_NAME))).thenReturn(get);
        when(get.execute()).thenReturn(existing);

        GcpDiskInsertOutcome outcome = underTest.insertDiskIfAbsent(compute, PROJECT_ID, ZONE, disk, DISK_NAME);

        assertTrue(outcome.existingDisk().isPresent());
        assertEquals(existing, outcome.existingDisk().get());
        assertTrue(outcome.operation().isEmpty());
        verify(disks, never()).insert(eq(PROJECT_ID), eq(ZONE), eq(disk));
    }

    @Test
    void testThrowsWhenInsertOperationReturnsHttpError() throws Exception {
        Disk disk = new Disk().setName(DISK_NAME);
        when(compute.disks()).thenReturn(disks);
        when(disks.get(eq(PROJECT_ID), eq(ZONE), eq(DISK_NAME))).thenReturn(get);
        when(get.execute()).thenThrow(googleException(HttpStatus.SC_NOT_FOUND, "Not Found"));
        when(disks.insert(eq(PROJECT_ID), eq(ZONE), eq(disk))).thenReturn(insert);
        when(insert.execute()).thenReturn(new Operation().setName("op-1").setHttpErrorStatusCode(400).setHttpErrorMessage("BAD REQUEST"));

        GcpResourceException exception = assertThrows(GcpResourceException.class,
                () -> underTest.insertDiskIfAbsent(compute, PROJECT_ID, ZONE, disk, DISK_NAME));
        assertTrue(exception.getMessage().contains("BAD REQUEST"));
    }

    @Test
    void testReusesDiskWhenInsertConflicts() throws Exception {
        Disk disk = new Disk().setName(DISK_NAME);
        Disk existing = new Disk().setName(DISK_NAME);
        when(compute.disks()).thenReturn(disks);
        when(disks.get(eq(PROJECT_ID), eq(ZONE), eq(DISK_NAME))).thenReturn(get);
        // first fetch: absent -> insert; second fetch (after 409): the disk created by the concurrent winner
        when(get.execute()).thenThrow(googleException(HttpStatus.SC_NOT_FOUND, "Not Found")).thenReturn(existing);
        when(disks.insert(eq(PROJECT_ID), eq(ZONE), eq(disk))).thenReturn(insert);
        when(insert.execute()).thenThrow(googleException(HttpStatus.SC_CONFLICT, "Conflict"));

        GcpDiskInsertOutcome outcome = underTest.insertDiskIfAbsent(compute, PROJECT_ID, ZONE, disk, DISK_NAME);

        assertTrue(outcome.existingDisk().isPresent());
        assertEquals(existing, outcome.existingDisk().get());
        assertTrue(outcome.operation().isEmpty());
    }

    @Test
    void testRethrowsConflictWhenDiskStillAbsentAfterRefetch() throws Exception {
        Disk disk = new Disk().setName(DISK_NAME);
        when(compute.disks()).thenReturn(disks);
        when(disks.get(eq(PROJECT_ID), eq(ZONE), eq(DISK_NAME))).thenReturn(get);
        when(get.execute()).thenThrow(googleException(HttpStatus.SC_NOT_FOUND, "Not Found"));
        when(disks.insert(eq(PROJECT_ID), eq(ZONE), eq(disk))).thenReturn(insert);
        when(insert.execute()).thenThrow(googleException(HttpStatus.SC_CONFLICT, "Conflict"));

        assertThrows(GoogleJsonResponseException.class, () -> underTest.insertDiskIfAbsent(compute, PROJECT_ID, ZONE, disk, DISK_NAME));
    }

    @Test
    void testThrowsWhenFetchFailsWithNonNotFoundError() throws Exception {
        Disk disk = new Disk().setName(DISK_NAME);
        when(compute.disks()).thenReturn(disks);
        when(disks.get(eq(PROJECT_ID), eq(ZONE), eq(DISK_NAME))).thenReturn(get);
        when(get.execute()).thenThrow(googleException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Server Error"));

        assertThrows(GcpResourceException.class, () -> underTest.insertDiskIfAbsent(compute, PROJECT_ID, ZONE, disk, DISK_NAME));
        verify(disks, never()).insert(eq(PROJECT_ID), eq(ZONE), eq(disk));
    }

    private GoogleJsonResponseException googleException(int statusCode, String statusMessage) {
        GoogleJsonError details = new GoogleJsonError();
        details.setCode(statusCode);
        return new GoogleJsonResponseException(new HttpResponseException.Builder(statusCode, statusMessage, new HttpHeaders()), details);
    }
}
