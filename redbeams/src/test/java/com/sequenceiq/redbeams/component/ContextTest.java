package com.sequenceiq.redbeams.component;

import static com.sequenceiq.redbeams.configuration.DatabaseConfig.JdbcConnectionProvider;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.collections.IteratorUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import com.sequenceiq.redbeams.RedbeamsApplication;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.redbeams.configuration.DatabaseConfig;
import com.sequenceiq.redbeams.controller.v4.database.DatabaseV4Controller;
import com.sequenceiq.redbeams.repository.DatabaseConfigRepository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
        "redbeams.client.secret=B61A44B418CF4A88897C86677151E844",
        "redbeams.identity.server.url=http://127.0.0.1:8089",
        "redbeams.db.port.5432.tcp.addr=localhost",
        "redbeams.db.port.5432.tcp.port=5432",
        "redbeams.client.id=redbeams",
        "server.port=8087",
        "redbeams.schema.migration.auto=true",
        "caas.url=127.0.0.1:10080",
        "vault.addr=127.0.0.1",
        "vault.root.token=s.6ZqWyBK9v5mkXelmhhc3RjXR",
        "altus.ums.host=localhost",
        "redbeams.cloudbreak.url=http://127.0.0.1:8080",
        "secret.engine=com.sequenceiq.redbeams.component.VaultMockEngine",
        "spring.main.allow-bean-definition-overriding=true"
})
@SpringBootTest(
        classes = {RedbeamsApplication.class, ContextTest.ContextConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
public class ContextTest {
    @SuppressFBWarnings(value = "UrF", justification = "Manages postgres lifecycle test-wise, but no other interaction is needed")
    @Rule
    public PostgreSQLContainer postgres = new PostgreSQLContainer<>("postgres:9.6.1-alpine");

    @Inject
    private DatabaseV4Controller databaseV4Controller;

    @Inject
    private DatabaseConfigRepository databaseConfigRepository;

    @Inject
    private ApplicationContext applicationContext;

    @Test
    public void test() {
        assertNotNull(applicationContext);
    }

    @Before
    public void before() {
        System.out.println("hello");
    }

    @Test
    public void testRegisterDb() {
        DatabaseV4Request request = new DatabaseV4Request();
        request.setConnectionPassword("password");
        request.setConnectionUserName("username");
        request.setConnectionURL("jdbc:postgresql://url/database");
        request.setDescription("my fancy stuff");
        request.setEnvironmentId("crn");
        request.setName("database1");
        request.setType("postgres");

        DatabaseV4Response response = databaseV4Controller.register(request);

        assertNotNull(response.getCrn());
        List<DatabaseConfig> foundElements = IteratorUtils.toList(databaseConfigRepository.findAll().iterator());
        assertThat(foundElements, hasSize(1));
    }

    @Configuration
    public static class ContextConfiguration {

        @Bean
        public JdbcConnectionProvider jdbcConnectionStringProvider() {
            return new JdbcConnectionProvider("jdbc:tc", "org.testcontainers.jdbc.ContainerDatabaseDriver");
        }
    }
}
