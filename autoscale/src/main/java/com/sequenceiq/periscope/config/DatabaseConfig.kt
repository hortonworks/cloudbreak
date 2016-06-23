package com.sequenceiq.periscope.config

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

import java.util.Properties

import org.postgresql.Driver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.JpaVendorAdapter
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.Database
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
class DatabaseConfig {

    @Value("${periscope.db.user:postgres}")
    private val dbUser: String? = null

    @Value("${periscope.db.pass:}")
    private val dbPassword: String? = null

    @Value("${periscope.db.name:postgres}")
    private val dbName: String? = null

    @Value("${periscope.db.hbm2ddl.strategy:validate}")
    private val hbm2ddlStrategy: String? = null

    @Autowired
    @Qualifier("databaseAddress")
    private val databaseAddress: String? = null

    @Bean
    fun dataSource(): DataSource {
        val dataSource = SimpleDriverDataSource()
        dataSource.setDriverClass(Driver::class.java)
        dataSource.url = String.format("jdbc:postgresql://%s/%s", databaseAddress, dbName)
        dataSource.username = dbUser
        dataSource.password = dbPassword
        return dataSource
    }

    @Bean
    @Throws(Exception::class)
    fun transactionManager(): PlatformTransactionManager {
        val jpaTransactionManager = JpaTransactionManager()
        jpaTransactionManager.entityManagerFactory = entityManagerFactory()
        jpaTransactionManager.afterPropertiesSet()
        return jpaTransactionManager
    }

    @Bean
    fun entityManager(entityManagerFactory: EntityManagerFactory): EntityManager {
        return entityManagerFactory.createEntityManager()
    }

    @Bean
    @DependsOn("databaseUpMigration")
    fun entityManagerFactory(): EntityManagerFactory {
        val entityManagerFactory = LocalContainerEntityManagerFactoryBean()

        entityManagerFactory.setPackagesToScan("com.sequenceiq.periscope.domain")
        entityManagerFactory.dataSource = dataSource()

        entityManagerFactory.jpaVendorAdapter = jpaVendorAdapter()
        entityManagerFactory.setJpaProperties(jpaProperties())
        entityManagerFactory.afterPropertiesSet()
        return entityManagerFactory.`object`
    }

    @Bean
    fun jpaVendorAdapter(): JpaVendorAdapter {
        val hibernateJpaVendorAdapter = HibernateJpaVendorAdapter()
        hibernateJpaVendorAdapter.setShowSql(true)
        hibernateJpaVendorAdapter.setDatabase(Database.POSTGRESQL)
        return hibernateJpaVendorAdapter
    }

    private fun jpaProperties(): Properties {
        val properties = Properties()
        properties.setProperty("hibernate.hbm2ddl.auto", hbm2ddlStrategy)
        properties.setProperty("hibernate.show_sql", "false")
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
        return properties
    }

}
