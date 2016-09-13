@TypeDefs({
        @TypeDef(
                name = "encrypted_string",
                typeClass = EncryptedStringType.class,
                parameters = {
                        @Parameter(name = "encryptorRegisteredName", value = "hibernateStringEncryptor")
                }
        )
})

package com.sequenceiq.cloudbreak.domain;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jasypt.hibernate4.type.EncryptedStringType;