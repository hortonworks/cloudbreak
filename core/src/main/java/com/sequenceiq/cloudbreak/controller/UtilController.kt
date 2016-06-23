package com.sequenceiq.cloudbreak.controller

import javax.validation.Valid

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.UtilEndpoint
import com.sequenceiq.cloudbreak.api.model.RDSConfigJson
import com.sequenceiq.cloudbreak.api.model.RdsTestResult
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator

@Component
class UtilController : UtilEndpoint {

    @Autowired
    private val rdsConnectionValidator: RdsConnectionValidator? = null

    override fun testRdsConnection(@Valid rdsConfigJson: RDSConfigJson): RdsTestResult {
        val rdsTestResult = RdsTestResult()
        try {
            rdsConnectionValidator!!.validateRdsConnection(rdsConfigJson)
            rdsTestResult.connectionResult = "connected"
        } catch (e: BadRequestException) {
            rdsTestResult.connectionResult = e.message
        }

        return rdsTestResult
    }
}
