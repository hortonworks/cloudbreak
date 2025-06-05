package com.sequenceiq.distrox.v1.distrox.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialViewResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;

@ExtendWith(MockitoExtension.class)
public class EnvironmentServiceDecoratorTest {

    @InjectMocks
    private EnvironmentServiceDecorator underTest;

    @Mock
    private EnvironmentService environmentClientService;

    @Test
    void testPrepareEnvironmentsAndCredentialNameWhenEnvironmentCrnProvidedThenShouldCallGetEnvironmentByCrn() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        NameOrCrn nameOrCrn = mock(NameOrCrn.class);
        Set<StackViewV4Response> stackViewResponses = Set.of(
                new StackViewV4Response());

        when(nameOrCrn.hasCrn()).thenReturn(true);
        when(nameOrCrn.getCrn()).thenReturn("crn");
        when(detailedEnvironmentResponse.getCredential()).thenReturn(credentialResponse);
        when(credentialResponse.getName()).thenReturn("credential-name");
        when(detailedEnvironmentResponse.getName()).thenReturn("env-name");
        when(environmentClientService.getByCrn(any())).thenReturn(detailedEnvironmentResponse);

        underTest.prepareEnvironmentsAndCredentialName(stackViewResponses, nameOrCrn);

        verify(environmentClientService, times(1)).getByCrn(any());
        assertEquals(stackViewResponses.iterator().next().getEnvironmentName(), "env-name");
        assertEquals(stackViewResponses.iterator().next().getCredentialName(), "credential-name");
        assertEquals(stackViewResponses.iterator().next().isGovCloud(), false);
    }

    @Test
    void testPrepareEnvironmentsAndCredentialEnvironmentNameWhenNameProvidedThenShouldCallGetEnvironmentByName() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        NameOrCrn nameOrCrn = mock(NameOrCrn.class);
        Set<StackViewV4Response> stackViewResponses = Set.of(
                new StackViewV4Response());

        when(nameOrCrn.hasCrn()).thenReturn(false);
        when(nameOrCrn.hasName()).thenReturn(true);
        when(nameOrCrn.getName()).thenReturn("name");
        when(detailedEnvironmentResponse.getCredential()).thenReturn(credentialResponse);
        when(credentialResponse.getName()).thenReturn("credential-name");
        when(detailedEnvironmentResponse.getName()).thenReturn("env-name");
        when(environmentClientService.getByName(any())).thenReturn(detailedEnvironmentResponse);

        underTest.prepareEnvironmentsAndCredentialName(stackViewResponses, nameOrCrn);

        verify(environmentClientService, times(1)).getByName(any());
        assertEquals(stackViewResponses.iterator().next().getEnvironmentName(), "env-name");
        assertEquals(stackViewResponses.iterator().next().getCredentialName(), "credential-name");
        assertEquals(stackViewResponses.iterator().next().isGovCloud(), false);
    }

    @Test
    void testPrepareEnvironmentsAndCredentialWithoutNameAndCrnWhenNameProvidedThenShouldCallGetEnvironmentList() {
        SimpleEnvironmentResponses simpleEnvironmentResponses = new SimpleEnvironmentResponses();
        SimpleEnvironmentResponse simpleEnvironmentResponse = new SimpleEnvironmentResponse();
        CredentialViewResponse credentialViewResponse = new CredentialViewResponse();
        NameOrCrn nameOrCrn = mock(NameOrCrn.class);
        StackViewV4Response stackViewV4Response = new StackViewV4Response();
        stackViewV4Response.setEnvironmentCrn("env-crn");
        Set<StackViewV4Response> stackViewResponses = Set.of(stackViewV4Response);

        when(nameOrCrn.hasCrn()).thenReturn(false);
        when(nameOrCrn.hasName()).thenReturn(false);
        credentialViewResponse.setName("credential-name");
        simpleEnvironmentResponse.setCredential(credentialViewResponse);
        simpleEnvironmentResponse.setName("env-name");
        simpleEnvironmentResponse.setCrn("env-crn");
        simpleEnvironmentResponses.setResponses(Set.of(simpleEnvironmentResponse));
        when(environmentClientService.list()).thenReturn(simpleEnvironmentResponses);

        underTest.prepareEnvironmentsAndCredentialName(stackViewResponses, nameOrCrn);

        verify(environmentClientService, times(1)).list();
        assertEquals(stackViewResponses.iterator().next().getEnvironmentName(), "env-name");
        assertEquals(stackViewResponses.iterator().next().getCredentialName(), "credential-name");
        assertEquals(stackViewResponses.iterator().next().isGovCloud(), false);
    }

}