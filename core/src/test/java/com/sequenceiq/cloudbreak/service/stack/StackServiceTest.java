package com.sequenceiq.cloudbreak.service.stack;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.user.UserService;

@RunWith(MockitoJUnitRunner.class)
public class StackServiceTest {

    private static final Long STACK_ID = 1L;

    private static final Long ORGANIZATION_ID = 1L;

    private static final String INSTANCE_ID = "instanceId";

    private static final String INSTANCE_ID2 = "instanceId2";

    private static final String INSTANCE_PUBLIC_IP = "2.2.2.2";

    private static final String INSTANCE_PUBLIC_IP2 = "3.3.3.3";

    private static final String OWNER = "1234567";

    private static final String USER_ID = OWNER;

    private static final String VARIANT_VALUE = "VARIANT_VALUE";

    private static final String IMAGE_CATALOG = "IMAGE_CATALOG";

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private StackService underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private StackDownscaleValidatorService downscaleValidatorService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private Stack stack;

    @Mock
    private InstanceMetaData instanceMetaData;

    @Mock
    private InstanceMetaData instanceMetaData2;

    @Mock
    private IdentityUser user;

    @Mock
    private ServiceProviderConnectorAdapter connector;

    @Mock
    private Variant variant;

    @Mock
    private StackAuthentication stackAuthentication;

    @Mock
    private ComponentConfigProvider componentConfigProvider;

    @Mock
    private InstanceGroupRepository instanceGroupRepository;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private SecurityConfig securityConfig;

    @Mock
    private SecurityConfigRepository securityConfigRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private PlatformParameters parameters;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private TransactionService transactionService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private UserService userService;

    private final Organization organization = new Organization();

    @Before
    public void setup() throws TransactionExecutionException {
        organization.setName("Top Secret FBI");
        organization.setId(ORGANIZATION_ID);
        when(stack.getOrganization()).thenReturn(organization);
    }

    // TODO: have to write new tests

    @Test
    public void testWhenStackCouldNotFindByItsIdThenExceptionWouldThrown() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.ofNullable(null));
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format("Stack '%d' not found", STACK_ID));
        underTest.getById(STACK_ID);
    }

    @Test
    public void testCreateFailsWithInvalidImageId() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(connector.checkAndGetPlatformVariant(stack)).thenReturn(variant);
        when(variant.value()).thenReturn(VARIANT_VALUE);

        when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        when(stackAuthentication.passwordAuthenticationRequired()).thenReturn(false);

        when(stackRepository.save(stack)).thenReturn(stack);

        when(tlsSecurityService.storeSSHKeys()).thenReturn(securityConfig);
        when(connector.getPlatformParameters(stack)).thenReturn(parameters);

        expectedException.expectCause(org.hamcrest.Matchers.any(CloudbreakImageNotFoundException.class));

        String platformString = "AWS";
        doThrow(new CloudbreakImageNotFoundException("Image not found"))
                .when(imageService)
                .create(eq(stack), eq(platformString), eq(parameters), nullable(StatedImage.class));

        try {
            stack = underTest.create(user, stack, platformString, mock(StatedImage.class));
        } finally {
            verify(stack, times(1)).setPlatformVariant(eq(VARIANT_VALUE));
            verify(securityConfig, times(1)).setSaltPassword(anyObject());
            verify(securityConfig, times(1)).setSaltBootPassword(anyObject());
            verify(securityConfig, times(1)).setKnoxMasterSecret(anyObject());
            verify(securityConfig, times(1)).setStack(stack);
            verify(securityConfigRepository, times(1)).save(securityConfig);
        }
    }

    @Test
    public void testCreateImageFoundNoStackStatusUpdate() {
        when(connector.checkAndGetPlatformVariant(stack)).thenReturn(variant);
        when(variant.value()).thenReturn(VARIANT_VALUE);

        when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        when(stackAuthentication.passwordAuthenticationRequired()).thenReturn(false);

        when(stackRepository.save(stack)).thenReturn(stack);

        when(tlsSecurityService.storeSSHKeys()).thenReturn(securityConfig);
        when(connector.getPlatformParameters(stack)).thenReturn(parameters);

        try {
            stack = underTest.create(user, stack, "AWS", mock(StatedImage.class));
        } finally {
            verify(stack, times(1)).setPlatformVariant(eq(VARIANT_VALUE));
            verify(securityConfig, times(1)).setSaltPassword(anyObject());
            verify(securityConfig, times(1)).setSaltBootPassword(anyObject());
            verify(securityConfig, times(1)).setKnoxMasterSecret(anyObject());
            verify(securityConfig, times(1)).setStack(stack);
            verify(securityConfigRepository, times(1)).save(securityConfig);

            verify(stackUpdater, times(0)).updateStackStatus(eq(Long.MAX_VALUE), eq(DetailedStackStatus.PROVISION_FAILED), anyString());
        }
    }
}
