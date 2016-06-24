package com.sequenceiq.cloudbreak.service.sssdconfig;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.SssdConfig;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.SssdConfigRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@RunWith(MockitoJUnitRunner.class)
public class SsssdConfigHostServiceTypeTest {

    @InjectMocks
    private SssdConfigService underTest;

    @Mock
    private SssdConfigRepository sssdConfigRepository;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private SssdConfig sssdConfig;

    @Before
    public void setUp() {
        underTest = new SssdConfigService();
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(underTest, "sssdName", "Test SSSD Config");
        ReflectionTestUtils.setField(underTest, "sssdType", "LDAP");
        ReflectionTestUtils.setField(underTest, "sssdUrl", "ldap://domain");
        ReflectionTestUtils.setField(underTest, "sssdSchema", "RFC2307");
        ReflectionTestUtils.setField(underTest, "sssdBase", "dc=domain");
    }

    @Test
    public void testGetDefaultSssdConfigWithNoDefault() {
        when(sssdConfigRepository.findByNameInAccount(anyString(), anyString())).thenReturn(null);
        underTest.getDefaultSssdConfig(TestUtil.cbAdminUser());
        verify(sssdConfigRepository, times(2)).findByNameInAccount(anyString(), anyString());
        verify(sssdConfigRepository, times(1)).save(any(SssdConfig.class));
    }

    @Test
    public void testGetDefaultSssdConfigWithDefault() {
        when(sssdConfigRepository.findByNameInAccount(anyString(), anyString())).thenReturn(sssdConfig);
        underTest.getDefaultSssdConfig(TestUtil.cbAdminUser());
        verify(sssdConfigRepository, times(1)).findByNameInAccount(anyString(), anyString());
        verify(sssdConfigRepository, times(0)).save(any(SssdConfig.class));
    }

    @Test
    public void testCreateWithoutError() {
        when(sssdConfigRepository.save(any(SssdConfig.class))).thenReturn(sssdConfig);
        SssdConfig config = underTest.create(TestUtil.cbAdminUser(), sssdConfig);
        verify(sssdConfig, times(1)).setAccount(anyString());
        verify(sssdConfig, times(1)).setOwner(anyString());
        verify(sssdConfigRepository, times(1)).save(any(SssdConfig.class));
        assertEquals(sssdConfig, config);
    }

    @Test(expected = DuplicateKeyValueException.class)
    public void testCreateWithDuplicatedKey() {
        when(sssdConfigRepository.save(any(SssdConfig.class))).thenThrow(DataIntegrityViolationException.class);
        underTest.create(TestUtil.cbAdminUser(), sssdConfig);
        verify(sssdConfig, times(1)).setAccount(anyString());
        verify(sssdConfig, times(1)).setOwner(anyString());
        verify(sssdConfigRepository, times(1)).save(any(SssdConfig.class));
    }

    @Test
    public void testGetWithoutError() {
        when(sssdConfigRepository.findOne(anyLong())).thenReturn(sssdConfig);
        SssdConfig config = underTest.get(1L);
        verify(sssdConfigRepository, times(1)).findOne(anyLong());
        assertEquals(sssdConfig, config);
    }

    @Test(expected = NotFoundException.class)
    public void testGetWithoNotFoundError() {
        when(sssdConfigRepository.findOne(anyLong())).thenReturn(null);
        underTest.get(1L);
        verify(sssdConfigRepository, times(1)).findOne(anyLong());
    }

    @Test
    public void testRetrieveAccountConfigsForAdmin() {
        when(sssdConfigRepository.findAllInAccount(anyString())).thenReturn(Sets.newHashSet(sssdConfig));
        when(sssdConfigRepository.findPublicInAccountForUser(anyString(), anyString())).thenReturn(Sets.newHashSet(sssdConfig));
        underTest.retrieveAccountConfigs(TestUtil.cbAdminUser());
        verify(sssdConfigRepository, times(1)).findAllInAccount(anyString());
        verify(sssdConfigRepository, times(0)).findPublicInAccountForUser(anyString(), anyString());
    }

    @Test
    public void testRetrieveAccountConfigsForNonAdmin() {
        when(sssdConfigRepository.findAllInAccount(anyString())).thenReturn(Sets.newHashSet(sssdConfig));
        when(sssdConfigRepository.findPublicInAccountForUser(anyString(), anyString())).thenReturn(Sets.newHashSet(sssdConfig));
        underTest.retrieveAccountConfigs(TestUtil.cbUser());
        verify(sssdConfigRepository, times(0)).findAllInAccount(anyString());
        verify(sssdConfigRepository, times(1)).findPublicInAccountForUser(anyString(), anyString());
    }

    @Test
    public void testGetPrivateConfigWithoutError() {
        when(sssdConfigRepository.findByNameForUser(anyString(), anyString())).thenReturn(sssdConfig);
        SssdConfig config = underTest.getPrivateConfig("name", TestUtil.cbAdminUser());
        verify(sssdConfigRepository, times(1)).findByNameForUser(anyString(), anyString());
        assertEquals(sssdConfig, config);
    }

    @Test(expected = NotFoundException.class)
    public void testGetPrivateConfigWithoNotFoundError() {
        when(sssdConfigRepository.findByNameForUser(anyString(), anyString())).thenReturn(null);
        underTest.getPrivateConfig("name", TestUtil.cbAdminUser());
        verify(sssdConfigRepository, times(1)).findByNameForUser(anyString(), anyString());
    }

    @Test
    public void testGetPublicConfigWithoutError() {
        when(sssdConfigRepository.findByNameInAccount(anyString(), anyString())).thenReturn(sssdConfig);
        SssdConfig config = underTest.getPublicConfig("name", TestUtil.cbAdminUser());
        verify(sssdConfigRepository, times(1)).findByNameInAccount(anyString(), anyString());
        assertEquals(sssdConfig, config);
    }

    @Test(expected = NotFoundException.class)
    public void testGetPublicConfigWithoNotFoundError() {
        when(sssdConfigRepository.findByNameInAccount(anyString(), anyString())).thenReturn(null);
        underTest.getPublicConfig("name", TestUtil.cbAdminUser());
        verify(sssdConfigRepository, times(1)).findByNameInAccount(anyString(), anyString());
    }

    @Test
    public void testDeleteWithoutError() {
        CbUser user = TestUtil.cbAdminUser();
        sssdConfig.setOwner(user.getUserId());
        sssdConfig.setAccount(user.getAccount());
        when(sssdConfigRepository.findOne(anyLong())).thenReturn(sssdConfig);
        when(clusterRepository.findAllClustersBySssdConfig(anyLong())).thenReturn(Collections.emptySet());
        underTest.delete(1L, user);
        verify(sssdConfigRepository, times(1)).findOne(anyLong());
        verify(clusterRepository, times(1)).findAllClustersBySssdConfig(anyLong());
        verify(sssdConfig, times(1)).getOwner();
        verify(sssdConfigRepository, times(1)).delete(any(SssdConfig.class));
    }

    @Test(expected = BadRequestException.class)
    public void testDeleteWithPermissionError() {
        CbUser user = TestUtil.cbUser();
        sssdConfig.setOwner("owner");
        sssdConfig.setAccount("account");
        when(sssdConfigRepository.findOne(anyLong())).thenReturn(sssdConfig);
        when(clusterRepository.findAllClustersBySssdConfig(anyLong())).thenReturn(Collections.emptySet());
        try {
            underTest.delete(1L, user);
        } catch (Exception e) {
            verify(sssdConfigRepository, times(0)).delete(any(SssdConfig.class));
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void testDeleteWithUsedError() {
        CbUser user = TestUtil.cbUser();
        sssdConfig.setOwner("owner");
        sssdConfig.setAccount("account");
        when(sssdConfigRepository.findOne(anyLong())).thenReturn(sssdConfig);
        when(clusterRepository.findAllClustersBySssdConfig(anyLong())).thenReturn(Collections.singleton(new Cluster()));
        try {
            underTest.delete(1L, user);
        } catch (Exception e) {
            verify(sssdConfig, times(0)).getOwner();
            verify(sssdConfigRepository, times(0)).delete(any(SssdConfig.class));
            throw e;
        }
    }
}
