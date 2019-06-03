package com.sequenceiq.freeipa.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.service.stack.FreeIpaCreationService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDeletionService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDescribeService;
import com.sequenceiq.freeipa.service.stack.FreeIpaListService;
import com.sequenceiq.freeipa.service.stack.FreeIpaRootCertificateService;
import com.sequenceiq.freeipa.util.CrnService;

@ExtendWith(MockitoExtension.class)
class FreeIpaV1ControllerTest {

    private static final String ENVIRONMENT_CRN = "test:environment:crn";

    private static final String ACCOUNT_ID = "accountId";

    @InjectMocks
    private FreeIpaV1Controller underTest;

    @Mock
    private FreeIpaDeletionService deletionService;

    @Mock
    private FreeIpaCreationService creationService;

    @Mock
    private FreeIpaDescribeService describeService;

    @Mock
    private FreeIpaListService freeIpaListService;

    @Mock
    private FreeIpaRootCertificateService rootCertificateService;

    @Mock
    private CrnService crnService;

    @BeforeEach
    void init() {
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
    }

    @Test
    void create() {
        CreateFreeIpaRequest freeIpaRequest = new CreateFreeIpaRequest();
        assertNull(underTest.create(freeIpaRequest));

        verify(creationService, times(1)).launchFreeIpa(freeIpaRequest, ACCOUNT_ID);
    }

    @Test
    void describe() {
        assertNull(underTest.describe(ENVIRONMENT_CRN));
    }

    @Test
    void list() {
        List<ListFreeIpaResponse> responseList = Collections.singletonList(new ListFreeIpaResponse());
        when(freeIpaListService.list(ACCOUNT_ID)).thenReturn(responseList);

        List<ListFreeIpaResponse> actual = underTest.list();

        assertEquals(responseList, actual);
        verify(crnService).getCurrentAccountId();
        verify(freeIpaListService).list(ACCOUNT_ID);
    }

    @Test
    void getRootCertificate() throws Exception {
        underTest.getRootCertificate(ENVIRONMENT_CRN);
        verify(rootCertificateService, times(1)).getRootCertificate(ENVIRONMENT_CRN, ACCOUNT_ID);
    }

    @Test
    void delete() {
        underTest.delete(ENVIRONMENT_CRN);

        verify(deletionService, times(1)).delete(ENVIRONMENT_CRN, ACCOUNT_ID);
    }
}