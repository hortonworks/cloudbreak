package com.sequenceiq.cloudbreak.service.externaldatabase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@ExtendWith(MockitoExtension.class)
class ExternalDbVersionCollectorTest {

    private static final String INTERNAL = "veryInternal";

    private static final String DB_CRN = "dbCrn";

    @Mock
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @InjectMocks
    private ExternalDbVersionCollector underTest;

    @Test
    public void testNullVersionReturned() {
        when(databaseServerV4Endpoint.getByCrn(DB_CRN)).thenReturn(new DatabaseServerV4Response());

        Optional<String> result = underTest.collectDbVersion(DB_CRN);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testVersionReturned() {
        DatabaseServerV4Response response = new DatabaseServerV4Response();
        response.setMajorVersion(MajorVersion.VERSION_10);
        when(databaseServerV4Endpoint.getByCrn(DB_CRN)).thenReturn(response);

        Optional<String> result = underTest.collectDbVersion(DB_CRN);

        assertTrue(result.isPresent());
        assertEquals("10", result.get());
    }
}