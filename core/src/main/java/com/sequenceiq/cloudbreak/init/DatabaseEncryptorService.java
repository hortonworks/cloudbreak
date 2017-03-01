package com.sequenceiq.cloudbreak.init;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.repository.BlueprintRepository;

@Component
public class DatabaseEncryptorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseEncryptorService.class);

    @Inject
    private javax.sql.DataSource dataSource;

    @Inject
    private PBEStringCleanablePasswordEncryptor encryptor;

    @Inject
    private BlueprintRepository blueprintRepository;

    @PostConstruct
    public void init() throws SQLException {
        encryptBlueprintTexts();
    }

    private void encryptBlueprintTexts() throws SQLException {
        try {
            blueprintRepository.findFirstByIdGreaterThan(0L);
        } catch (EncryptionOperationNotPossibleException e) {
            LOGGER.info("Encrypting blueprint texts");
            try (Statement select = dataSource.getConnection().createStatement(); Statement update = dataSource.getConnection().createStatement()) {
                select.execute("SELECT id, blueprinttext FROM blueprint");
                try (ResultSet rs = select.getResultSet()) {
                    while (rs.next()) {
                        Long id = rs.getLong("id");
                        String text = rs.getString("blueprinttext");
                        update.executeUpdate("UPDATE blueprint SET blueprinttext = '" + encryptor.encrypt(text) + "' WHERE id = " + id);
                    }
                }
            }
        }
    }
}
