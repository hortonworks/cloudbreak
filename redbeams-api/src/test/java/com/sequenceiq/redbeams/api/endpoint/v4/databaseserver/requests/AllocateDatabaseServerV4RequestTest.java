package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.sequenceiq.redbeams.api.endpoint.v4.stacks.AwsDBStackV4Parameters;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4Request;

import org.junit.Before;
import org.junit.Test;

public class AllocateDatabaseServerV4RequestTest {

    private AllocateDatabaseServerV4Request request;

    @Before
    public void setUp() throws Exception {
        request = new AllocateDatabaseServerV4Request();
    }

    @Test
    public void testGettersAndSetters() {
        request.setName("myallocation");
        assertEquals("myallocation", request.getName());

        request.setEnvironmentId("myenv");
        assertEquals("myenv", request.getEnvironmentId());

        NetworkV4Request network = new NetworkV4Request();
        request.setNetwork(network);
        assertEquals(network, request.getNetwork());

        DatabaseServerV4Request server = new DatabaseServerV4Request();
        request.setDatabaseServer(server);
        assertEquals(server, request.getDatabaseServer());
    }

    @Test
    public void testAwsParameters() {
        assertNull(request.getAws());

        AwsDBStackV4Parameters parameters = request.createAws();
        assertNotNull(parameters);

        parameters = new AwsDBStackV4Parameters();
        request.setAws(parameters);
        assertEquals(parameters, request.createAws());
        assertEquals(parameters, request.getAws());
    }

}
