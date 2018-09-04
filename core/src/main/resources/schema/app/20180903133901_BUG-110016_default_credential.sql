-- // BUG-110016 User should be able to store default credentials in each organization
-- Migration SQL that makes the change goes here.

CREATE TABLE userprofile_credential (
    userprofile_id bigint NOT NULL,
    defaultcredentials_id bigint NOT NULL
);

INSERT INTO userprofile_credential(
    userprofile_id,
    defaultcredentials_id
)
SELECT userprofile.id AS userprofile_id, userprofile.credential_id AS credential_id FROM userprofile WHERE credential_id IS NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS userprofile_credential;

