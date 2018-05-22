-- // BUG-102745
-- Migration SQL that makes the change goes here.

ALTER TABLE gateway
    ADD CONSTRAINT gateway_pkey PRIMARY KEY (id);

CREATE TABLE gateway_topology (
    id bigserial NOT NULL,
    gateway_id int8 NOT NULL,
    topologyname VARCHAR (255) NULL,
    exposedservices text NULL,
    CONSTRAINT gateway_topology_pkey PRIMARY KEY (id),
    CONSTRAINT fk_gateway_gateway_topology FOREIGN KEY (gateway_id) REFERENCES gateway(id)
);

INSERT INTO gateway_topology(gateway_id, topologyname, exposedservices)
SELECT id, topologyname, exposedservices
FROM gateway
WHERE gateway.topologyname IS NOT NULL;

-- during migration the topologies from gateways are copied to gateway_topology

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS gateway_topology;

ALTER TABLE gateway DROP CONSTRAINT gateway_pkey;

-- during undo we do not migrate topologies back to the gateway table because it's an unreleased feature and it's not worth the effort
-- code uses the gateway_topology table for topologies. topology part of gateway in code can be removed.
