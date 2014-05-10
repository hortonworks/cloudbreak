environments {
    jenkins {
        sonar_host_url = "http://$System.env.SONAR_PORT_9000_TCP_ADDR:9000"
        sonar_jdbc_url = "jdbc:mysql://$System.env.SONAR_DB_PORT_3306_TCP_ADDR:3306/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true"
    }

    local {
        sonar_host_url = "http://localhost:9000"
        sonar_jdbc_url = "jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true"
    }
}