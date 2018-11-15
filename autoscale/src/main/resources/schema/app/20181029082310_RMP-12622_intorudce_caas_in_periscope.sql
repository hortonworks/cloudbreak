-- // RMP-12622_intorudce-caas_in_periscope
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS user_id;
ALTER TABLE history DROP COLUMN IF EXISTS user_id;
DROP TABLE IF EXISTS periscope_user;

CREATE TABLE IF NOT EXISTS clusterpertain (
    id bigserial NOT NULL,
    tenant character varying(255) NOT NULL,
    workspaceid bigint NOT NULL,
    userid character varying(255) NOT NULL
);

ALTER TABLE ONLY clusterpertain
    ADD CONSTRAINT clusterpertain_pkey PRIMARY KEY (id);

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS clusterpertain_id bigint NOT NULL;

ALTER TABLE cluster
    ADD CONSTRAINT fk_cluster_pertain FOREIGN KEY (clusterpertain_id) REFERENCES clusterpertain(id);

-- //@UNDO
-- SQL to undo the change goes here.


