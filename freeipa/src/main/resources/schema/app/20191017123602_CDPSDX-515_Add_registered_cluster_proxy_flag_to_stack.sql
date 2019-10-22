-- // CDPSDX-515 add flag to track whether stack has been registered
-- with the cluster-proxy.
-- Migration SQL that makes the change goes here.

ALTER TABLE stack
    ADD COLUMN clusterproxyregistered BOOLEAN;

UPDATE stack
    SET clusterproxyregistered='false';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack
    DROP COLUMN clusterproxyregistered;
