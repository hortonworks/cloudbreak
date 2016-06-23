package com.sequenceiq.cloudbreak.api.model

enum class RDSDatabase private constructor(val dbName: String, val dbDriver: String, val ambariDbOption: String) {
    POSTGRES("postgres", "org.postgresql.Driver", "Existing PostgreSQL Database"),
    MYSQL("mysql", "com.mysql.jdbc.Driver", "Existing MySQL / MariaDB Database")
}
