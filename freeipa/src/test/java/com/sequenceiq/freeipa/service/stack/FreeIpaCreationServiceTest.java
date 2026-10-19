package com.sequenceiq.freeipa.service.stack;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.CloudStorageFolderResolverService;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.converter.stack.CreateFreeIpaRequestToStackConverter;
import com.sequenceiq.freeipa.converter.stack.StackToDescribeFreeIpaResponseConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.backup.cloud.CloudBackupFolderResolverService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.multiaz.MultiAzCalculatorService;
import com.sequenceiq.freeipa.service.multiaz.MultiAzValidator;
import com.sequenceiq.freeipa.service.recipe.FreeIpaRecipeService;
import com.sequenceiq.freeipa.service.telemetry.AccountTelemetryService;
import com.sequenceiq.freeipa.service.validation.FreeIpaHyperVGenerationValidator;
import com.sequenceiq.freeipa.service.validation.SeLinuxValidationService;
import com.sequenceiq.freeipa.service.validation.SshPublicKeyValidationService;
import com.sequenceiq.freeipa.util.CrnService;

@ExtendWith(MockitoExtension.class)
class FreeIpaCreationServiceTest {

    private static final String ACCOUNT_ID = "1234";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:1";

    @Mock
    private StackService stackService;

    @Mock
    private CreateFreeIpaRequestToStackConverter stackConverter;

    @Mock
    private StackTemplateService templateService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private ImageService imageService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private StackToDescribeFreeIpaResponseConverter stackToDescribeFreeIpaResponseConverter;

    @Mock
    private CrnService crnService;

    @Mock
    private GrpcUmsClient umsClient;

    @Mock
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Mock
    private TransactionService transactionService;

    @Mock
    private CloudStorageFolderResolverService cloudStorageFolderResolverService;

    @Mock
    private CloudBackupFolderResolverService cloudBackupFolderResolverService;

    @Mock
    private AccountTelemetryService accountTelemetryService;

    @Mock
    private MultiAzCalculatorService multiAzCalculatorService;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Mock
    private MultiAzValidator multiAzValidator;

    @Mock
    private FreeIpaRecommendationService freeIpaRecommendationService;

    @Mock
    private FreeIpaRecipeService freeIpaRecipeService;

    @Mock
    private ImageConverter imageConverter;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private SeLinuxValidationService seLinuxValidationService;

    @Mock
    private FreeIpaHyperVGenerationValidator freeIpaHyperVGenerationValidator;

    @Mock
    private SshPublicKeyValidationService sshPublicKeyValidationService;

    @InjectMocks
    private FreeIpaCreationService underTest;

    @Test
    void testLaunchFreeIpaShouldNotSaveAnythingWhenSshPublicKeyIsNotAcceptableForTheImage() {
        CreateFreeIpaRequest request = new CreateFreeIpaRequest();
        request.setEnvironmentCrn(ENV_CRN);
        Credential credential = mock(Credential.class);
        Stack stack = new Stack();
        Image image = mock(Image.class);
        when(image.getOsType()).thenReturn(OsType.RHEL9.getOsType());
        when(crnService.getUserCrn()).thenReturn(USER_CRN);
        when(credentialService.getCredentialByEnvCrn(ENV_CRN)).thenReturn(credential);
        when(stackConverter.convert(eq(request), any(), eq(ACCOUNT_ID), any(), eq(USER_CRN), any())).thenReturn(stack);
        when(imageService.fetchImageWrapperAndName(eq(stack), any())).thenReturn(Pair.of(ImageWrapper.ofFreeipaImage(image, "catalogUrl"), "imageName"));
        doThrow(new BadRequestException("RSA public key is 2047 bits, at least 2048 bits are required"))
                .when(sshPublicKeyValidationService).validateForCreation(stack, OsType.RHEL9);

        assertThatThrownBy(() -> underTest.launchFreeIpa(request, ACCOUNT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("RSA public key is 2047 bits, at least 2048 bits are required");

        verifyNoInteractions(transactionService, stackService, flowManager);
    }
}
