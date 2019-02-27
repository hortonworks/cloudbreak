-- // CB-79 show terminated clusters config
-- Migration SQL that makes the change goes here.
CREATE SEQUENCE IF NOT EXISTS showterminatedclusterspreferences_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

CREATE TABLE IF NOT EXISTS showterminatedclusterspreferences (
    id BIGINT PRIMARY KEY DEFAULT nextval('showterminatedclusterspreferences_id_seq'),
    show_terminated BOOLEAN NOT NULL,
    show_terminated_timeout_millisecs BIGINT
);
ALTER TABLE IF EXISTS userprofile ADD COLUMN IF NOT EXISTS showterminatedclusterspreferences_id BIGINT;
ALTER TABLE userprofile ADD CONSTRAINT fk_userprofile_showterminatedclusterspreferences_id FOREIGN KEY (showterminatedclusterspreferences_id) REFERENCES showterminatedclusterspreferences(id);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE IF EXISTS userprofile DROP CONSTRAINT IF EXISTS fk_userprofile_showterminatedclusterspreferences_id;
ALTER TABLE IF EXISTS userprofile DROP COLUMN IF EXISTS showterminatedclusterspreferences_id;
DROP TABLE IF EXISTS showterminatedclusterspreferences;
DROP SEQUENCE IF EXISTS showterminatedclusterspreferences_id_seq;

