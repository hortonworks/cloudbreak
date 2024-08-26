package com.sequenceiq.redbeams.service.stack;

import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent.REDBEAMS_SSL_CERT_ROTATE_EVENT;
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

import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateRedbeamsEvent;

@ExtendWith(MockitoExtension.class)
class RedbeamsRotateSslServiceTest {

    @Mock
    private DBStackService dbStackService;

    @Mock
    private RedbeamsFlowManager flowManager;

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
}