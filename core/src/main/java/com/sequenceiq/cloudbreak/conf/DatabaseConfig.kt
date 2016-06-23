package com.sequenceiq.cloudbreak.conf

import javax.inject.Inject
import javax.inject.Named
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

import java.util.Properties

import org.postgresql.Driver
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

    @Value("${cb.db.env.user:}")
    private val dbUser: String? = null

    @Value("${cb.db.env.pass:}")
    private val dbPassword: String? = null

    @Value("${cb.db.env.db:}")
    private val dbName: String? = null

    @Value("${cb.hbm2ddl.strategy:validate}")
    private val hbm2ddlStrategy: String? = null

    @Value("${cb.hibernate.debug:false}")
    private val debug: Boolean = false

    @Inject
    @Named("databaseAddress")
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
        jpaTransactionManager.entityManagerFactory = entityManagerFactory().`object`
        jpaTransactionManager.afterPropertiesSet()
        return jpaTransactionManager
    }

    @Bean
    fun entityManager(entityManagerFactory: EntityManagerFactory): EntityManager {
        return entityManagerFactory.createEntityManager()
    }

    @Bean
    @DependsOn("databaseUpMigration")
    fun entityManagerFactory(): LocalContainerEntityManagerFactoryBean {
        val entityManagerFactory = LocalContainerEntityManagerFactoryBean()

        entityManagerFactory.setPackagesToScan("com.sequenceiq.cloudbreak.domain")
        entityManagerFactory.dataSource = dataSource()

        entityManagerFactory.jpaVendorAdapter = jpaVendorAdapter()
        entityManagerFactory.setJpaProperties(jpaProperties())
        entityManagerFactory.afterPropertiesSet()
        return entityManagerFactory
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
        properties.setProperty("hibernate.show_sql", java.lang.Boolean.toString(debug))
        properties.setProperty("hibernate.format_sql", java.lang.Boolean.toString(debug))
        properties.setProperty("hibernate.use_sql_comments", java.lang.Boolean.toString(debug))
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
        return properties
    }
}
