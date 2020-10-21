package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

@ExtendWith(MockitoExtension.class)
class RedbeamsClientServiceTest {

    @Mock
    private DatabaseServerV4Endpoint redbeamsServerEndpoint;

    @InjectMocks
    private RedbeamsClientService underTest;

    @Test
    void deleteByCrnNotFoundIsRethrownAsIs() {
        when(redbeamsServerEndpoint.deleteByCrn(any(), anyBoolean())).thenThrow(new NotFoundException("not found"));
        assertThatThrownBy(() -> underTest.deleteByCrn("crn", true)).isExactlyInstanceOf(NotFoundException.class);
    }
}
