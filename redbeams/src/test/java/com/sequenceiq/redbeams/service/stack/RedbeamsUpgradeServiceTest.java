package com.sequenceiq.redbeams.service.stack;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.redbeams.domain.stack.DBStack;

@ExtendWith(MockitoExtension.class)
public class RedbeamsUpgradeServiceTest {

    private static final String SERVER_CRN_STRING = "ServerCrn";

    @Mock
    private DBStackService dbStackService;

    @InjectMocks
    private RedbeamsUpgradeService underTest;

    @Test
    void testUpgradeDatabaseServer() {
        when(dbStackService.getByCrn(SERVER_CRN_STRING)).thenReturn(new DBStack());

        underTest.upgradeDatabaseServer(SERVER_CRN_STRING, "majorVersion");

        verify(dbStackService).getByCrn(SERVER_CRN_STRING);
    }
}
