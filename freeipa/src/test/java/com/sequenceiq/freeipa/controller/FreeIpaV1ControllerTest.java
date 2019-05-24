package com.sequenceiq.freeipa.controller;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.service.stack.FreeIpaCreationService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDeletionService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDescribeService;
import com.sequenceiq.freeipa.service.stack.FreeIpaRootCertificateService;

@ExtendWith(MockitoExtension.class)
class FreeIpaV1ControllerTest {

    private static final String ENVIRONMENT_CRN = "test:environment:crn";

    @InjectMocks
    private FreeIpaV1Controller underTest;

    @Mock
    private FreeIpaDeletionService deletionService;

    @Mock
    private FreeIpaCreationService creationService;

    @Mock
    private FreeIpaDescribeService describeService;

    @Mock
    private FreeIpaRootCertificateService rootCertificateService;

    @Test
    void create() {
        CreateFreeIpaRequest freeIpaRequest = new CreateFreeIpaRequest();
        assertNull(underTest.create(freeIpaRequest));

        verify(creationService, times(1)).launchFreeIpa(freeIpaRequest, "test_account");
    }

    @Test
    void describe() {
        assertNull(underTest.describe(ENVIRONMENT_CRN));
    }

    @Test
    void getRootCertificate() throws Exception {
        underTest.getRootCertificate(ENVIRONMENT_CRN);
        verify(rootCertificateService, times(1)).getRootCertificate(ENVIRONMENT_CRN);
    }

    @Test
    void delete() {
        underTest.delete(ENVIRONMENT_CRN);

        verify(deletionService, times(1)).delete(ENVIRONMENT_CRN);
    }
}