package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.TargetMajorVersionToUpgradeTargetVersionConverter;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeOnCloudProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeOnCloudProviderResult;
import com.sequenceiq.cloudbreak.service.rdsconfig.DbUsernameConverterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.UpgradeDatabaseServerV4Response;

@ExtendWith(MockitoExtension.class)
class ValidateRdsUpgradeOnProviderHandlerTest {

    private static final Long STACK_ID = 42L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private RedbeamsClientService redbeamsClientService;

    @Mock
    private TargetMajorVersionToUpgradeTargetVersionConverter targetMajorVersionToUpgradeTargetVersionConverter;

    @Mock
    private HandlerEvent<ValidateRdsUpgradeOnCloudProviderRequest> event;

    @Mock
    private ExternalDatabaseService externalDatabaseService;

    @Mock
    private SecretService secretService;

    @Mock
    private DbUsernameConverterService dbUsernameConverterService;

    @InjectMocks
    private ValidateRdsUpgradeOnProviderHandler underTest;

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("VALIDATERDSUPGRADEONCLOUDPROVIDERREQUEST");
    }

    @Test
    void testDoAccept() {
        ValidateRdsUpgradeOnCloudProviderRequest request = new ValidateRdsUpgradeOnCloudProviderRequest(STACK_ID, TargetMajorVersion.VERSION_11);
        when(event.getData()).thenReturn(request);

        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.getDatabaseServerCrn()).thenReturn("dbcrn");
        initStackDto(clusterView);

        when(targetMajorVersionToUpgradeTargetVersionConverter.convert(TargetMajorVersion.VERSION_11)).thenReturn(UpgradeTargetMajorVersion.VERSION_11);
        when(redbeamsClientService.validateUpgrade(eq("dbcrn"), any(UpgradeDatabaseServerV4Request.class)))
                .thenReturn(new UpgradeDatabaseServerV4Response());

        Selectable result = underTest.doAccept(event);

        assertThat(result.selector()).isEqualTo("VALIDATERDSUPGRADEONCLOUDPROVIDERRESULT");
        assertThat(result).isInstanceOf(ValidateRdsUpgradeOnCloudProviderResult.class);
        assertThat(((ValidateRdsUpgradeOnCloudProviderResult) result).getReason()).isNullOrEmpty();
    }

    private void initStackDto(ClusterView clusterView) {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(clusterView);
    }

    @Test
    void testDoAcceptWhenValidationFailed() {
        ValidateRdsUpgradeOnCloudProviderRequest request = new ValidateRdsUpgradeOnCloudProviderRequest(STACK_ID, TargetMajorVersion.VERSION_11);
        when(event.getData()).thenReturn(request);

        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.getDatabaseServerCrn()).thenReturn("dbcrn");
        initStackDto(clusterView);
        when(targetMajorVersionToUpgradeTargetVersionConverter.convert(TargetMajorVersion.VERSION_11)).thenReturn(UpgradeTargetMajorVersion.VERSION_11);
        UpgradeDatabaseServerV4Response response = new UpgradeDatabaseServerV4Response();
        response.setReason("reason");
        when(redbeamsClientService.validateUpgrade(eq("dbcrn"), any(UpgradeDatabaseServerV4Request.class)))
                .thenReturn(response);

        Selectable result = underTest.doAccept(event);

        assertThat(result.selector()).isEqualTo("VALIDATERDSUPGRADEONCLOUDPROVIDERRESULT");
        assertThat(result).isInstanceOf(ValidateRdsUpgradeOnCloudProviderResult.class);
        assertThat(((ValidateRdsUpgradeOnCloudProviderResult) result).getReason()).isEqualTo("reason");
    }

    @Test
    void testDoAcceptWhenException() {
        ValidateRdsUpgradeOnCloudProviderRequest request = new ValidateRdsUpgradeOnCloudProviderRequest(STACK_ID, TargetMajorVersion.VERSION_11);
        when(event.getData()).thenReturn(request);
        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.getDatabaseServerCrn()).thenReturn("dbcrn");
        BadRequestException badRequestException = new BadRequestException("badrequest");
        initStackDto(clusterView);
        when(targetMajorVersionToUpgradeTargetVersionConverter.convert(TargetMajorVersion.VERSION_11)).thenReturn(UpgradeTargetMajorVersion.VERSION_11);
        doThrow(badRequestException).when(redbeamsClientService).validateUpgrade(eq("dbcrn"), any(UpgradeDatabaseServerV4Request.class));

        Selectable result = underTest.doAccept(event);

        assertThat(result).isInstanceOf(ValidateRdsUpgradeFailedEvent.class);
        assertThat(result.getException()).isEqualTo(badRequestException);
        assertThat(result.selector()).isEqualTo("VALIDATERDSUPGRADEFAILEDEVENT");
    }
}