package com.sequenceiq.cloudbreak.service.sssdconfig

import javax.inject.Inject
import javax.transaction.Transactional

import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.SssdProviderType
import com.sequenceiq.cloudbreak.api.model.SssdSchemaType
import com.sequenceiq.cloudbreak.api.model.SssdTlsReqcertType
import com.sequenceiq.cloudbreak.common.type.APIResourceType
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.NotFoundException
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.SssdConfig
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.SssdConfigRepository
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException

@Service
@Transactional
class SssdConfigService {

    @Value("${cb.sssd.name}")
    private val sssdName: String? = null
    @Value("${cb.sssd.type}")
    private val sssdType: String? = null
    @Value("${cb.sssd.url}")
    private val sssdUrl: String? = null
    @Value("${cb.sssd.schema}")
    private val sssdSchema: String? = null
    @Value("${cb.sssd.base}")
    private val sssdBase: String? = null

    @Inject
    private val sssdConfigRepository: SssdConfigRepository? = null

    @Inject
    private val clusterRepository: ClusterRepository? = null

    fun getDefaultSssdConfig(user: CbUser): SssdConfig {
        var config: SssdConfig? = sssdConfigRepository!!.findByNameInAccount(sssdName, user.account)
        if (config == null) {
            synchronized (sssdName) {
                config = sssdConfigRepository.findByNameInAccount(sssdName, user.account)
                if (config == null) {
                    config = SssdConfig()
                    config!!.isPublicInAccount = true
                    config!!.account = user.account
                    config!!.owner = user.userId
                    config!!.name = sssdName
                    config!!.providerType = SssdProviderType.valueOf(sssdType)
                    config!!.url = sssdUrl
                    config!!.schema = SssdSchemaType.valueOf(sssdSchema)
                    config!!.baseSearch = sssdBase
                    config!!.tlsReqcert = SssdTlsReqcertType.NEVER
                    sssdConfigRepository.save<SssdConfig>(config)
                }
            }
        }
        return config
    }

    @Transactional(Transactional.TxType.NEVER)
    fun create(user: CbUser, sssdConfig: SssdConfig): SssdConfig {
        sssdConfig.owner = user.userId
        sssdConfig.account = user.account
        try {
            return sssdConfigRepository!!.save(sssdConfig)
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateKeyValueException(APIResourceType.SSSDCONFIG, sssdConfig.name, ex)
        }

    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    operator fun get(id: Long?): SssdConfig? {
        val sssdConfig = sssdConfigRepository!!.findOne(id) ?: throw NotFoundException(String.format("SssdConfig '%s' not found", id))
        return sssdConfig
    }

    fun retrievePrivateConfigs(user: CbUser): Set<SssdConfig> {
        return sssdConfigRepository!!.findForUser(user.userId)
    }

    fun retrieveAccountConfigs(user: CbUser): Set<SssdConfig> {
        if (user.roles.contains(CbUserRole.ADMIN)) {
            return sssdConfigRepository!!.findAllInAccount(user.account)
        } else {
            return sssdConfigRepository!!.findPublicInAccountForUser(user.userId, user.account)
        }
    }

    fun getPrivateConfig(name: String, user: CbUser): SssdConfig {
        val sssdConfig = sssdConfigRepository!!.findByNameForUser(name, user.userId) ?: throw NotFoundException(String.format("SssdConfig '%s' not found.", name))
        return sssdConfig
    }

    fun getPublicConfig(name: String, user: CbUser): SssdConfig {
        val sssdConfig = sssdConfigRepository!!.findByNameInAccount(name, user.account) ?: throw NotFoundException(String.format("SssdConfig '%s' not found.", name))
        return sssdConfig
    }

    fun delete(id: Long?, user: CbUser) {
        val sssdConfig = get(id) ?: throw NotFoundException(String.format("SssdConfig '%s' not found.", id))
        delete(sssdConfig, user)
    }

    fun delete(name: String, user: CbUser) {
        val sssdConfig = sssdConfigRepository!!.findByNameInAccount(name, user.account) ?: throw NotFoundException(String.format("SssdConfig '%s' not found.", name))
        delete(sssdConfig, user)
    }

    private fun delete(sssdConfig: SssdConfig, user: CbUser) {
        if (clusterRepository!!.findAllClustersBySssdConfig(sssdConfig.id).isEmpty()) {
            if (user.userId != sssdConfig.owner && !user.roles.contains(CbUserRole.ADMIN)) {
                throw BadRequestException("Public SSSD configs can only be deleted by owners or account admins.")
            } else {
                sssdConfigRepository!!.delete(sssdConfig)
            }
        } else {
            throw BadRequestException(String.format(
                    "There are clusters associated with SSSD config '%s'. Please remove these before deleting the SSSD config.", sssdConfig.id))
        }
    }
}
