package com.sequenceiq.redbeams.service.stack;

import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent.REDBEAMS_SSL_CERT_ROTATE_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationEventSelectors.REDBEAMS_SSL_MIGRATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateRedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationEvent;
import com.sequenceiq.redbeams.service.sslcertificate.SslConfigService;

@ExtendWith(MockitoExtension.class)
class RedbeamsRotateSslServiceTest {

    @Mock
    private DBStackService dbStackService;

    @Mock
    private RedbeamsFlowManager flowManager;

    @Mock
    private SslConfigService sslConfigService;

    @InjectMocks
    private RedbeamsRotateSslService redbeamsRotateSslService;

    @Test
    public void testRotateDatabaseServerSslCert() {
        DBStack dbStack = mock(DBStack.class);
        String crn = "crn:test";
        when(dbStackService.getByCrn(crn)).thenReturn(dbStack);
        when(dbStack.getId()).thenReturn(1L);

        redbeamsRotateSslService.rotateDatabaseServerSslCert(crn);

        verify(dbStackService).getByCrn(crn);
        verify(flowManager).notify(eq(REDBEAMS_SSL_CERT_ROTATE_EVENT.selector()),
                argThat(event -> event instanceof SslCertRotateRedbeamsEvent
                        && !((SslCertRotateRedbeamsEvent) event).isOnlyCertificateUpdate()));
    }

    @Test
    public void testUpdateToLatestDatabaseServerSslCert() {
        DBStack dbStack = mock(DBStack.class);
        String crn = "crn:test";
        when(dbStackService.getByCrn(crn)).thenReturn(dbStack);
        when(dbStack.getId()).thenReturn(1L);

        redbeamsRotateSslService.updateToLatestDatabaseServerSslCert(crn);

        verify(dbStackService).getByCrn(crn);
        verify(flowManager).notify(eq(REDBEAMS_SSL_CERT_ROTATE_EVENT.selector()),
                argThat(event -> event instanceof SslCertRotateRedbeamsEvent
                        && ((SslCertRotateRedbeamsEvent) event).isOnlyCertificateUpdate()));
    }

    @Test
    void testMigrateDatabaseServerSslCertFromNonSslToSsl() {
        DBStack dbStack = mock(DBStack.class);
        String crn = "crn:test";
        when(dbStackService.getByCrn(crn)).thenReturn(dbStack);

        DBStack result = redbeamsRotateSslService.migrateDatabaseServerSslCertFromNonSslToSsl(crn);

        verify(sslConfigService).createSslConfig(eq(SslMode.ENABLED), eq(dbStack));
        assertEquals(dbStack, result);
    }

    @Test
    void testTurnOnSsl() throws Exception {
        DBStack dbStack = mock(DBStack.class);
        when(dbStack.getCloudPlatform()).thenReturn("AWS");
        String crn = "crn:test";
        when(dbStackService.getByCrn(crn)).thenReturn(dbStack);
        when(dbStack.getId()).thenReturn(1L);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "1");
        when(flowManager.notify(any(), any())).thenReturn(flowIdentifier);
        FlowIdentifier result = redbeamsRotateSslService.turnOnSsl(crn);

        verify(sslConfigService).createSslConfig(eq(SslMode.ENABLED), eq(dbStack));
        verify(flowManager).notify(eq(REDBEAMS_SSL_MIGRATION_EVENT.selector()), any(RedbeamsSslMigrationEvent.class));
        assertEquals(flowIdentifier, result);
    }

    @Test
    void testTurnOnSslException() throws Exception {
        DBStack dbStack = mock(DBStack.class);
        when(dbStack.getCloudPlatform()).thenReturn("GCP");
        String crn = "crn:test";
        when(dbStackService.getByCrn(crn)).thenReturn(dbStack);
        RedbeamsException exception = assertThrows(RedbeamsException.class, () -> redbeamsRotateSslService.turnOnSsl(crn));

        assertEquals(exception.getMessage(), "SSL DB migration is not supported for the cloud platform: GCP");
    }
}