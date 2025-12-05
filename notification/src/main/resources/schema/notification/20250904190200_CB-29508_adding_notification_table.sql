-- // CB-29508 adding notification table
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE notification_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS notification (
    id                  BIGINT PRIMARY KEY NOT NULL DEFAULT nextval('notification_id_seq'),
    severity            varchar(255) NOT NULL,
    type                varchar(255) NOT NULL,
    channelType         varchar(255) NOT NULL,
    resourcecrn         varchar(255) NOT NULL,
    resourceName        varchar(255) NOT NULL,
    name                varchar(255) NOT NULL,
    accountId           varchar(255) NOT NULL,
    message             text,
    createdAt           BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000),
    sentAt              BIGINT,
    sent                bool DEFAULT false,
    formFactor          varchar(255) NOT NULL
    );

CREATE INDEX IF NOT EXISTS notification_name_idx ON notification USING btree (name);
CREATE INDEX IF NOT EXISTS notification_accountid_idx ON notification USING btree (accountid);
CREATE INDEX IF NOT EXISTS notification_id_accountid_idx ON notification USING btree (id, accountid);
CREATE INDEX IF NOT EXISTS notification_resourcecrn_idx ON notification USING btree (resourcecrn);
CREATE INDEX IF NOT EXISTS notification_resourcecrn_type_sent_idx ON notification USING btree (resourcecrn, type, sent)

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS notification_id_idx;
DROP INDEX IF EXISTS notification_name_idx;
DROP INDEX IF EXISTS notification_accountid_idx;
DROP INDEX IF EXISTS notification_id_accountid_idx;
DROP INDEX IF EXISTS notification_resourcecrn_idx;
DROP INDEX IF EXISTS notification_resourcecrn_type_sent_idx;

DROP TABLE IF EXISTS notification;