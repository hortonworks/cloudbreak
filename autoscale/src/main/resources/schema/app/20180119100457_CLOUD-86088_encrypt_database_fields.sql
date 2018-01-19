-- // CLOUD-86088_encrypt_database_fields
-- Migration SQL that makes the change goes here.

CREATE TABLE securityconfig_encrypted (
    id bigint NOT NULL,
    clientkey text,
    clientcert text,
    servercert text,
    cluster_id bigint NOT NULL
);

ALTER TABLE securityconfig_encrypted ALTER COLUMN id SET DEFAULT nextval ('securityconfig_id_seq');

INSERT INTO securityconfig_encrypted (clientkey, clientcert, servercert, cluster_id) SELECT encode(clientkey, 'escape'), encode(clientcert, 'escape'), encode(servercert, 'escape'), cluster_id FROM securityconfig;

DROP TABLE securityconfig;

ALTER TABLE securityconfig_encrypted RENAME TO securityconfig;

ALTER TABLE ONLY securityconfig ADD CONSTRAINT securityconfig_pkey PRIMARY KEY (id);

ALTER TABLE ONLY securityconfig ADD CONSTRAINT fk_securityconfig_cluster_id FOREIGN KEY (cluster_id) REFERENCES cluster(id);

-- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE securityconfig_decrypted (
    id bigint NOT NULL,
    clientkey bytea,
    clientcert bytea,
    servercert bytea,
    cluster_id bigint NOT NULL
);

ALTER TABLE securityconfig_decrypted ALTER COLUMN id SET DEFAULT nextval ('securityconfig_id_seq');

INSERT INTO securityconfig_decrypted (clientkey, clientcert, servercert, cluster_id) SELECT decode(clientkey, 'escape'), decode(clientcert, 'escape'), decode(servercert, 'escape'), cluster_id FROM securityconfig;

DROP TABLE securityconfig;

ALTER TABLE securityconfig_decrypted RENAME TO securityconfig;

ALTER TABLE ONLY securityconfig ADD CONSTRAINT securityconfig_pkey PRIMARY KEY (id);

ALTER TABLE ONLY securityconfig ADD CONSTRAINT fk_securityconfig_cluster_id FOREIGN KEY (cluster_id) REFERENCES cluster(id);