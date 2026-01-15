package com.sequenceiq.environment.environment.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.CreateEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.environment.authorization.EnvironmentFiltering;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.CreateEnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.service.EnvironmentCreationService;
import com.sequenceiq.environment.environment.service.EnvironmentModificationService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentUpgradeCcmService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentApiConverter;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;

@ExtendWith(MockitoExtension.class)
class EnvironmentControllerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final Set<String> SUBNETS = Set.of("subnet1", "subnet2");

    private static final String ENV_CRN = "envCrn";

    @Mock
    private EnvironmentApiConverter environmentApiConverter;

    @Mock
    private EnvironmentFiltering environmentFiltering;

    @Mock
    private EnvironmentCreationService environmentCreationService;

    @Mock
    private EnvironmentResponseConverter environmentResponseConverter;

    @Mock
    private EnvironmentUpgradeCcmService upgradeCcmService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EnvironmentModificationService environmentModificationService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator iam;

    @InjectMocks
    private EnvironmentController underTest;

    @Test
    void testEndpointGatewayOptionsPreserved() {
        EnvironmentNetworkRequest networkRequest = setupNetworkRequestWithEndpointGatway();
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setNetwork(networkRequest);

        setupServiceResponses();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.post(environmentRequest));

        assertEquals(PublicEndpointAccessGateway.ENABLED, networkRequest.getPublicEndpointAccessGateway());
        assertEquals(SUBNETS, networkRequest.getEndpointGatewaySubnetIds());
    }

    @Test
    void testUpgradeCcmByNameCallsService() {
        underTest.upgradeCcmByName("name123");
        verify(upgradeCcmService).upgradeCcmByName("name123");
    }

    @Test
    void testUpgradeCcmByCrnCallsService() {
        underTest.upgradeCcmByCrn("crn123");
        verify(upgradeCcmService).upgradeCcmByCrn("crn123");
    }

    @Test
    void testEditByNameNotAvailable() {
        String accountId = "accountId";
        NameOrCrn environmentCRN = NameOrCrn.ofName(ENV_CRN);
        Environment env = new Environment();
        env.setStatus(EnvironmentStatus.CREATE_FAILED);
        try (MockedStatic<ThreadBasedUserCrnProvider> mockedThreadBasedUserCrnProvider = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            mockedThreadBasedUserCrnProvider.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(accountId);
            when(environmentModificationService.getEnvironment(accountId, environmentCRN)).thenReturn(env);
            try {
                underTest.editByName(ENV_CRN, null);
            } catch (BadRequestException e) {
                assertEquals("Environment status is not AVAILABLE for Edit", e.getMessage());
            }
        }
    }

    @Test
    void testEditByNameAvailable() {
        String accountId = "accountId";
        NameOrCrn environmentCRN = NameOrCrn.ofName(ENV_CRN);
        Environment env = new Environment();
        env.setStatus(EnvironmentStatus.AVAILABLE);
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder().build();
        EnvironmentDto environmentDto = EnvironmentDto.builder().withName(ENV_CRN).build();
        environmentDto.setName("environmentName");
        try (MockedStatic<ThreadBasedUserCrnProvider> mockedThreadBasedUserCrnProvider = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            mockedThreadBasedUserCrnProvider.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(accountId);
            when(environmentModificationService.getEnvironment(accountId, environmentCRN)).thenReturn(env);
            when(environmentApiConverter.initEditDto(env, null)).thenReturn(environmentEditDto);
            when(environmentModificationService.edit(env, environmentEditDto)).thenReturn(environmentDto);
            when(environmentResponseConverter.dtoToDetailedResponse(environmentDto))
                    .thenReturn(DetailedEnvironmentResponse.builder().build());
            DetailedEnvironmentResponse response = underTest.editByName(ENV_CRN, null);
            assertEquals(DetailedEnvironmentResponse.class, response.getClass());
        }
    }

    @Test
    void testChangeCredentialByEnvironmentNameNotAvailable() {
        String accountId = "accountId";
        NameOrCrn environmentCRN = NameOrCrn.ofName(ENV_CRN);
        Environment env = new Environment();
        env.setStatus(EnvironmentStatus.CREATE_FAILED);
        try (MockedStatic<ThreadBasedUserCrnProvider> mockedThreadBasedUserCrnProvider = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            mockedThreadBasedUserCrnProvider.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(accountId);
            when(environmentModificationService.getEnvironment(accountId, environmentCRN)).thenReturn(env);
            try {
                underTest.changeCredentialByEnvironmentName(ENV_CRN, null);
            } catch (BadRequestException e) {
                assertEquals("Environment status is not AVAILABLE for Edit", e.getMessage());
            }
        }
    }

    @Test
    void testChangeCredentialByEnvironmentNameAvailable() {
        String accountId = "accountId";
        NameOrCrn environmentCRN = NameOrCrn.ofName(ENV_CRN);
        Environment env = new Environment();
        env.setStatus(EnvironmentStatus.AVAILABLE);
        EnvironmentChangeCredentialDto environmentChangeCredentialDto = EnvironmentChangeCredentialDto.builder().build();
        EnvironmentDto environmentDto = EnvironmentDto.builder().withName(ENV_CRN).build();
        environmentDto.setName("environmentName");
        try (MockedStatic<ThreadBasedUserCrnProvider> mockedThreadBasedUserCrnProvider = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            mockedThreadBasedUserCrnProvider.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(accountId);
            when(environmentModificationService.getEnvironment(accountId, environmentCRN)).thenReturn(env);
            when(environmentApiConverter.convertEnvironmentChangeCredentialDto(null)).thenReturn(environmentChangeCredentialDto);
            when(environmentModificationService.changeCredentialByEnvironmentName(accountId, ENV_CRN, environmentChangeCredentialDto))
                    .thenReturn(environmentDto);
            when(environmentResponseConverter.dtoToDetailedResponse(environmentDto))
                    .thenReturn(DetailedEnvironmentResponse.builder().build());
            DetailedEnvironmentResponse response = underTest.changeCredentialByEnvironmentName(ENV_CRN, null);
            assertEquals(DetailedEnvironmentResponse.class, response.getClass());
        }
    }

    @ParameterizedTest
    @MethodSource("ccmScenarios")
    void isUpgradeCcmAvailable(Tunnel tunnel, int notUpgradedCount, boolean expectedResult) {
        EnvironmentDto env = new EnvironmentDto();
        ExperimentalFeatures experimentalFeatures = new ExperimentalFeatures();
        experimentalFeatures.setTunnel(tunnel);
        env.setExperimentalFeatures(experimentalFeatures);
        when(environmentService.internalGetByCrn(ENV_CRN)).thenReturn(env);
        lenient().when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(iam);
        lenient().when(stackV4Endpoint.getNotCcmUpgradedStackCount(anyLong(), eq(ENV_CRN), any())).thenReturn(notUpgradedCount);
        boolean result = underTest.isUpgradeCcmAvailable(ENV_CRN);
        assertThat(result).isEqualTo(expectedResult);
    }

    private EnvironmentNetworkRequest setupNetworkRequestWithEndpointGatway() {
        EnvironmentNetworkRequest networkRequest = new EnvironmentNetworkRequest();
        networkRequest.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        networkRequest.setEndpointGatewaySubnetIds(SUBNETS);
        return networkRequest;
    }

    private void setupServiceResponses() {
        when(environmentApiConverter.initCreationDto(any())).thenReturn(EnvironmentCreationDto.builder().build());
        when(environmentCreationService.create(any()))
                .thenReturn(CreateEnvironmentDto.builder().build());
        when(environmentResponseConverter.dtoToCreateResponse(any())).thenReturn(new CreateEnvironmentResponse());
    }

    public static Stream<Arguments> ccmScenarios() {
        return Stream.of(
                Arguments.of(Tunnel.DIRECT, 0, false),
                Arguments.of(Tunnel.DIRECT, 1, false),
                Arguments.of(Tunnel.DIRECT, 2, false),
                Arguments.of(Tunnel.CLUSTER_PROXY, 0, false),
                Arguments.of(Tunnel.CLUSTER_PROXY, 1, false),
                Arguments.of(Tunnel.CLUSTER_PROXY, 2, false),
                Arguments.of(Tunnel.CCM, 0, true),
                Arguments.of(Tunnel.CCM, 1, true),
                Arguments.of(Tunnel.CCM, 2, true),
                Arguments.of(Tunnel.CCMV2, 0, true),
                Arguments.of(Tunnel.CCMV2, 1, true),
                Arguments.of(Tunnel.CCMV2, 2, true),
                Arguments.of(Tunnel.CCMV2_JUMPGATE, 0, false),
                Arguments.of(Tunnel.CCMV2_JUMPGATE, 1, true),
                Arguments.of(Tunnel.CCMV2_JUMPGATE, 2, true)
        );
    }

    @Test
    void testListEndpointReturnsSimpleEnvironmentResponses() {
        String remoteCrn = "remoteCrn";
        EnvironmentDto envDto = new EnvironmentDto();
        List<EnvironmentDto> envDtoList = List.of(envDto);
        SimpleEnvironmentResponse simpleResponse = new SimpleEnvironmentResponse();

        when(environmentFiltering.filterEnvironments(any(), eq(Optional.of(remoteCrn)))).thenReturn(envDtoList);
        when(environmentResponseConverter.dtoToSimpleResponse(envDto, true, true)).thenReturn(simpleResponse);

        SimpleEnvironmentResponses result = underTest.list(remoteCrn);

        assertThat(result.getResponses()).containsExactly(simpleResponse);
        verify(environmentFiltering).filterEnvironments(any(), eq(Optional.of(remoteCrn)));
        verify(environmentResponseConverter).dtoToSimpleResponse(envDto, true, true);
    }

}
