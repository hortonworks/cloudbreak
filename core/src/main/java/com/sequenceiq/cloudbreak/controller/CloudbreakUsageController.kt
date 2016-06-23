package com.sequenceiq.cloudbreak.controller

import javax.ws.rs.core.Response

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.UsageEndpoint
import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.facade.CloudbreakUsagesFacade
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson

@Component
class CloudbreakUsageController : UsageEndpoint {

    @Autowired
    private val cloudbreakUsagesFacade: CloudbreakUsagesFacade? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    override fun getDeployer(
            since: Long?,
            filterEndDate: Long?,
            userId: String,
            accountId: String,
            cloud: String,
            zone: String): List<CloudbreakUsageJson> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val params = CbUsageFilterParameters.Builder().setAccount(accountId).setOwner(userId).setSince(since).setCloud(cloud).setRegion(zone).setFilterEndDate(filterEndDate).build()
        return cloudbreakUsagesFacade!!.getUsagesFor(params)
    }

    override fun getAccount(
            since: Long?,
            filterEndDate: Long?,
            userId: String,
            cloud: String,
            zone: String): List<CloudbreakUsageJson> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val params = CbUsageFilterParameters.Builder().setAccount(user.account).setOwner(userId).setSince(since).setCloud(cloud).setRegion(zone).setFilterEndDate(filterEndDate).build()
        return cloudbreakUsagesFacade!!.getUsagesFor(params)
    }

    override fun getUser(
            since: Long?,
            filterEndDate: Long?,
            cloud: String,
            zone: String): List<CloudbreakUsageJson> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val params = CbUsageFilterParameters.Builder().setAccount(user.account).setOwner(user.userId).setSince(since).setCloud(cloud).setRegion(zone).setFilterEndDate(filterEndDate).build()
        return cloudbreakUsagesFacade!!.getUsagesFor(params)
    }

    override fun generate(): Response {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        cloudbreakUsagesFacade!!.generateUserUsages()
        return Response.status(Response.Status.ACCEPTED).build()
    }
}
