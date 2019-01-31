package com.sequenceiq.cloudbreak.startup;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;

public class GovCloudFlagMigratorTest {

    @Mock
    private CredentialService credentialService;

    @InjectMocks
    private GovCloudFlagMigrator underTest;

    @Captor
    private ArgumentCaptor<Iterable<Credential>> captor;

    @Before
    public void setup() {
        initMocks(this);
        when(credentialService.saveAll(any())).thenReturn(Lists.newArrayList());
    }

    @Test
    public void testGovCloudFlagMigration() {
        when(credentialService.findAll()).thenReturn(Lists.newArrayList(
                createCredential("cred1", true),
                createCredential("cred2", false)));

        underTest.run();

        verify(credentialService).saveAll(captor.capture());
        assertEquals(2, Lists.newArrayList(captor.getValue()).size());
        assertEquals(Boolean.TRUE, getGovCloudFlagFromCaptor("cred1"));
        assertEquals(Boolean.FALSE, getGovCloudFlagFromCaptor("cred2"));
    }

    private Boolean getGovCloudFlagFromCaptor(String credentialName) {
        return Lists.newArrayList(captor.getValue())
                .stream()
                .filter(credential -> StringUtils.equals(credential.getName(), credentialName))
                .findFirst()
                .get()
                .getGovCloud();
    }

    private Credential createCredential(String name, Boolean govCloud) {
        Credential credential = new Credential();
        credential.setName(name);
        if (govCloud != null) {
            String json = govCloud ? "{\"govCloud\":true}" :  "{\"govCloud\":false}";
            credential.setAttributes(json);
        }
        credential.setGovCloud(Boolean.FALSE);
        return credential;
    }

}
