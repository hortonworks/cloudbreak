package com.sequenceiq.cloudbreak.controller.validation.rds

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.RDSConfigJson
import com.sequenceiq.cloudbreak.controller.BadRequestException

@Component
class RdsConnectionValidator {

    fun validateRdsConnection(rdsConfigJson: RDSConfigJson?) {
        if (rdsConfigJson != null) {
            val connectionProps = Properties()
            connectionProps.put("user", rdsConfigJson.connectionUserName)
            connectionProps.put("password", rdsConfigJson.connectionPassword)
            try {
                val conn = DriverManager.getConnection(rdsConfigJson.connectionURL, connectionProps)
                conn.close()
            } catch (e: SQLException) {
                throw BadRequestException("Failed to connect to RDS: " + e.message, e)
            }

        }
    }
}
