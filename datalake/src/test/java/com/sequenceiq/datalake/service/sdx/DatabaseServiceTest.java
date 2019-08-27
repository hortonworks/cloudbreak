package com.sequenceiq.datalake.service.sdx;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.client.RedbeamsServiceCrnClient;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
public class DatabaseServiceTest {

    @Captor
    public ArgumentCaptor<AllocateDatabaseServerV4Request> captor = ArgumentCaptor.forClass(AllocateDatabaseServerV4Request.class);

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private RedbeamsServiceCrnClient redbeamsClient;

    @Mock
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Mock
    private SdxNotificationService notificationService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private Map<SdxClusterShape, DatabaseConfig> dbConfigs;

    @InjectMocks
    private DatabaseService underTest;

    @Mock
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Test
    public void shouldSetDbConfigBasedOnClusterShape() {
        SdxCluster cluster = new SdxCluster();
        cluster.setClusterName("NAME");
        cluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        DatabaseConfig databaseConfig = getDatabaseConfig();

        when(databaseServerV4Endpoint.create(any())).thenThrow(BadRequestException.class);
        when(dbConfigs.get(eq(SdxClusterShape.LIGHT_DUTY))).thenReturn(databaseConfig);

        Assertions.assertThrows(BadRequestException.class, () -> {
            underTest.create(cluster, env, "ID");
        });

        verify(databaseServerV4Endpoint).create(captor.capture());
        AllocateDatabaseServerV4Request dbRequest = captor.getValue();
        assertThat(dbRequest.getDatabaseServer().getInstanceType(), is("instanceType"));
        assertThat(dbRequest.getDatabaseServer().getDatabaseVendor(), is("vendor"));
        assertThat(dbRequest.getDatabaseServer().getStorageSize(), is(100L));
        verifyZeroInteractions(sdxClusterRepository);
        verifyZeroInteractions(sdxStatusService);
        verifyZeroInteractions(notificationService);
    }

    private DatabaseConfig getDatabaseConfig() {
        return new DatabaseConfig("instanceType", "vendor", 100);
    }

}