package com.sequenceiq.redbeams.domain.stack;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;

public class DBStackStatusTest {

    private static final DBStack STACK = new DBStack();

    private DBStackStatus status;

    @Before
    public void setUp() {
        status = new DBStackStatus();
    }

    @Test
    public void testGettersAndSetters() {
        status.setId(1L);
        assertEquals(1L, status.getId().longValue());

        status.setDBStack(STACK);
        assertEquals(STACK, status.getDBStack());

        status.setStatus(Status.AVAILABLE);
        assertEquals(Status.AVAILABLE, status.getStatus());

        status.setStatusReason("because");
        assertEquals("because", status.getStatusReason());

        status.setDetailedDBStackStatus(DetailedDBStackStatus.PROVISIONED);
        assertEquals(DetailedDBStackStatus.PROVISIONED, status.getDetailedDBStackStatus());

        long now = System.currentTimeMillis();
        status.setCreated(now);
        assertEquals(now, status.getCreated().longValue());
    }

    @Test
    public void testConstructionFromDetailedStatus() {
        long now = System.currentTimeMillis();
        status = new DBStackStatus(STACK, DetailedDBStackStatus.PROVISION_REQUESTED, now);
        assertEquals(DetailedDBStackStatus.PROVISION_REQUESTED.getStatus(), status.getStatus());
    }

}
