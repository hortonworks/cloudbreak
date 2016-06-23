package com.sequenceiq.cloudbreak.service.sssdconfig

import org.junit.Assert.assertEquals
import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.util.Collections

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.util.ReflectionTestUtils

import com.google.common.collect.Sets
import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.NotFoundException
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.SssdConfig
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.SssdConfigRepository
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException

@RunWith(MockitoJUnitRunner::class)
class SsssdConfigHostServiceTypeTest {

    @InjectMocks
    private var underTest: SssdConfigService? = null

    @Mock
    private val sssdConfigRepository: SssdConfigRepository? = null

    @Mock
    private val clusterRepository: ClusterRepository? = null

    @Mock
    private val sssdConfig: SssdConfig? = null

    @Before
    fun setUp() {
        underTest = SssdConfigService()
        MockitoAnnotations.initMocks(this)
        ReflectionTestUtils.setField(underTest, "sssdName", "Test SSSD Config")
        ReflectionTestUtils.setField(underTest, "sssdType", "LDAP")
        ReflectionTestUtils.setField(underTest, "sssdUrl", "ldap://domain")
        ReflectionTestUtils.setField(underTest, "sssdSchema", "RFC2307")
        ReflectionTestUtils.setField(underTest, "sssdBase", "dc=domain")
    }

    @Test
    fun testGetDefaultSssdConfigWithNoDefault() {
        `when`(sssdConfigRepository!!.findByNameInAccount(anyString(), anyString())).thenReturn(null)
        underTest!!.getDefaultSssdConfig(TestUtil.cbAdminUser())
        verify(sssdConfigRepository, times(2)).findByNameInAccount(anyString(), anyString())
        verify(sssdConfigRepository, times(1)).save(any<SssdConfig>(SssdConfig::class.java))
    }

    @Test
    fun testGetDefaultSssdConfigWithDefault() {
        `when`(sssdConfigRepository!!.findByNameInAccount(anyString(), anyString())).thenReturn(sssdConfig)
        underTest!!.getDefaultSssdConfig(TestUtil.cbAdminUser())
        verify(sssdConfigRepository, times(1)).findByNameInAccount(anyString(), anyString())
        verify(sssdConfigRepository, times(0)).save(any<SssdConfig>(SssdConfig::class.java))
    }

    @Test
    fun testCreateWithoutError() {
        `when`(sssdConfigRepository!!.save(any<SssdConfig>(SssdConfig::class.java))).thenReturn(sssdConfig)
        val config = underTest!!.create(TestUtil.cbAdminUser(), sssdConfig)
        verify<SssdConfig>(sssdConfig, times(1)).account = anyString()
        verify<SssdConfig>(sssdConfig, times(1)).owner = anyString()
        verify(sssdConfigRepository, times(1)).save(any<SssdConfig>(SssdConfig::class.java))
        assertEquals(sssdConfig, config)
    }

    @Test(expected = DuplicateKeyValueException::class)
    fun testCreateWithDuplicatedKey() {
        `when`(sssdConfigRepository!!.save(any<SssdConfig>(SssdConfig::class.java))).thenThrow(DataIntegrityViolationException::class.java)
        underTest!!.create(TestUtil.cbAdminUser(), sssdConfig)
        verify<SssdConfig>(sssdConfig, times(1)).account = anyString()
        verify<SssdConfig>(sssdConfig, times(1)).owner = anyString()
        verify(sssdConfigRepository, times(1)).save(any<SssdConfig>(SssdConfig::class.java))
    }

    @Test
    fun testGetWithoutError() {
        `when`(sssdConfigRepository!!.findOne(anyLong())).thenReturn(sssdConfig)
        val config = underTest!!.get(1L)
        verify(sssdConfigRepository, times(1)).findOne(anyLong())
        assertEquals(sssdConfig, config)
    }

    @Test(expected = NotFoundException::class)
    fun testGetWithoNotFoundError() {
        `when`(sssdConfigRepository!!.findOne(anyLong())).thenReturn(null)
        underTest!!.get(1L)
        verify(sssdConfigRepository, times(1)).findOne(anyLong())
    }

    @Test
    fun testRetrieveAccountConfigsForAdmin() {
        `when`(sssdConfigRepository!!.findAllInAccount(anyString())).thenReturn(Sets.newHashSet<SssdConfig>(sssdConfig))
        `when`(sssdConfigRepository.findPublicInAccountForUser(anyString(), anyString())).thenReturn(Sets.newHashSet<SssdConfig>(sssdConfig))
        underTest!!.retrieveAccountConfigs(TestUtil.cbAdminUser())
        verify(sssdConfigRepository, times(1)).findAllInAccount(anyString())
        verify(sssdConfigRepository, times(0)).findPublicInAccountForUser(anyString(), anyString())
    }

    @Test
    fun testRetrieveAccountConfigsForNonAdmin() {
        `when`(sssdConfigRepository!!.findAllInAccount(anyString())).thenReturn(Sets.newHashSet<SssdConfig>(sssdConfig))
        `when`(sssdConfigRepository.findPublicInAccountForUser(anyString(), anyString())).thenReturn(Sets.newHashSet<SssdConfig>(sssdConfig))
        underTest!!.retrieveAccountConfigs(TestUtil.cbUser())
        verify(sssdConfigRepository, times(0)).findAllInAccount(anyString())
        verify(sssdConfigRepository, times(1)).findPublicInAccountForUser(anyString(), anyString())
    }

    @Test
    fun testGetPrivateConfigWithoutError() {
        `when`(sssdConfigRepository!!.findByNameForUser(anyString(), anyString())).thenReturn(sssdConfig)
        val config = underTest!!.getPrivateConfig("name", TestUtil.cbAdminUser())
        verify(sssdConfigRepository, times(1)).findByNameForUser(anyString(), anyString())
        assertEquals(sssdConfig, config)
    }

    @Test(expected = NotFoundException::class)
    fun testGetPrivateConfigWithoNotFoundError() {
        `when`(sssdConfigRepository!!.findByNameForUser(anyString(), anyString())).thenReturn(null)
        underTest!!.getPrivateConfig("name", TestUtil.cbAdminUser())
        verify(sssdConfigRepository, times(1)).findByNameForUser(anyString(), anyString())
    }

    @Test
    fun testGetPublicConfigWithoutError() {
        `when`(sssdConfigRepository!!.findByNameInAccount(anyString(), anyString())).thenReturn(sssdConfig)
        val config = underTest!!.getPublicConfig("name", TestUtil.cbAdminUser())
        verify(sssdConfigRepository, times(1)).findByNameInAccount(anyString(), anyString())
        assertEquals(sssdConfig, config)
    }

    @Test(expected = NotFoundException::class)
    fun testGetPublicConfigWithoNotFoundError() {
        `when`(sssdConfigRepository!!.findByNameInAccount(anyString(), anyString())).thenReturn(null)
        underTest!!.getPublicConfig("name", TestUtil.cbAdminUser())
        verify(sssdConfigRepository, times(1)).findByNameInAccount(anyString(), anyString())
    }

    @Test
    fun testDeleteWithoutError() {
        val user = TestUtil.cbAdminUser()
        sssdConfig!!.owner = user.userId
        sssdConfig.account = user.account
        `when`(sssdConfigRepository!!.findOne(anyLong())).thenReturn(sssdConfig)
        `when`(clusterRepository!!.findAllClustersBySssdConfig(anyLong())).thenReturn(emptySet<Cluster>())
        underTest!!.delete(1L, user)
        verify(sssdConfigRepository, times(1)).findOne(anyLong())
        verify(clusterRepository, times(1)).findAllClustersBySssdConfig(anyLong())
        verify(sssdConfig, times(1)).owner
        verify(sssdConfigRepository, times(1)).delete(any<SssdConfig>(SssdConfig::class.java))
    }

    @Test(expected = BadRequestException::class)
    fun testDeleteWithPermissionError() {
        val user = TestUtil.cbUser()
        sssdConfig!!.owner = "owner"
        sssdConfig.account = "account"
        `when`(sssdConfigRepository!!.findOne(anyLong())).thenReturn(sssdConfig)
        `when`(clusterRepository!!.findAllClustersBySssdConfig(anyLong())).thenReturn(emptySet<Cluster>())
        try {
            underTest!!.delete(1L, user)
        } catch (e: Exception) {
            verify(sssdConfigRepository, times(0)).delete(any<SssdConfig>(SssdConfig::class.java))
            throw e
        }

    }

    @Test(expected = BadRequestException::class)
    fun testDeleteWithUsedError() {
        val user = TestUtil.cbUser()
        sssdConfig!!.owner = "owner"
        sssdConfig.account = "account"
        `when`(sssdConfigRepository!!.findOne(anyLong())).thenReturn(sssdConfig)
        `when`(clusterRepository!!.findAllClustersBySssdConfig(anyLong())).thenReturn(setOf<Cluster>(Cluster()))
        try {
            underTest!!.delete(1L, user)
        } catch (e: Exception) {
            verify(sssdConfig, times(0)).owner
            verify(sssdConfigRepository, times(0)).delete(any<SssdConfig>(SssdConfig::class.java))
            throw e
        }

    }
}
