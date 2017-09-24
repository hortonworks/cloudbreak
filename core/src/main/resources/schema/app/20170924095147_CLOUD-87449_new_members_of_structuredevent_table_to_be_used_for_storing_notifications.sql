-- // CLOUD-87449 new members of structuredevent table to be used for storing notifications
-- Migration SQL that makes the change goes here.

ALTER TABLE structuredevent ADD COLUMN eventtype VARCHAR(255);
ALTER TABLE structuredevent ADD COLUMN resourcetype VARCHAR(255);
ALTER TABLE structuredevent ADD COLUMN resourceid BIGINT;
ALTER TABLE structuredevent ADD COLUMN timestamp BIGINT;
ALTER TABLE structuredevent ADD COLUMN account VARCHAR(255);
ALTER TABLE structuredevent ADD COLUMN userid VARCHAR(255);

CREATE INDEX idx_structuredevent_userid_eventtype ON structuredevent (userid, eventtype);
CREATE INDEX idx_structuredevent_userid_eventtype_resourcetype_resourceid ON structuredevent (userid, eventtype, resourcetype, resourceid);
CREATE INDEX idx_structuredevent_userid_eventtype_timestamp ON structuredevent (userid, eventtype, timestamp);

UPDATE structuredevent SET eventtype = subq.et, resourcetype = subq.rt, resourceid = subq.rid, timestamp = subq.t, account = subq.a, userid = subq.uid
FROM (select se.id as id,
             se.structuredeventjson::json->'operation'->'eventType' as et,
             se.structuredeventjson::json->'operation'->'resourceType' as rt,
             (se.structuredeventjson::json->'operation'->>'resourceId')::bigint as rid,
             (se.structuredeventjson::json->'operation'->>'timestamp')::bigint as t,
             se.structuredeventjson::json->'operation'->'account' as a,
             se.structuredeventjson::json->'operation'->'userId' as uid
        from structuredevent se) as subq
WHERE structuredevent.id = subq.id;

INSERT INTO structuredevent (eventtype, resourcetype, resourceid, timestamp, account, userid, structuredeventjson)
    (SELECT 'NOTIFICATION', 'STACK', stackid, extract('epoch' from eventtimestamp)::bigint, account, owner,
        json_build_object(
            'type',  'StructuredNotificationEvent',
            'operation', json_build_object(
                'eventType', 'NOTIFICATION',
                'resourceType', 'STACK',
                'resourceId', stackId,
                'timestamp', extract('epoch' from eventtimestamp)::bigint,
                'account', account,
                'userId', owner,
                'cloudbreakId', '',
                'cloudbreakVersion', ''),
            'notification', json_build_object(
                'notificationType', eventtype,
                'notification', eventmessage,
                'cloud', cloud,
                'region', region,
                'availabilityZone', availabilityzone,
                'stackId', stackid,
                'stackName', stackname,
                'stackStatus', stackstatus,
                'nodeCount', nodecount,
                'instanceGroup', instancegroup,
                'clusterId', clusterid,
                'clusterName', clustername,
                'clusterStatus', clusterstatus,
                'blueprintName', blueprintname,
                'blueprintId', blueprintid)) from cloudbreakevent);

DROP TABLE IF EXISTS cloudbreakevent;
DROP SEQUENCE IF EXISTS cloudbreakevent_id_seq;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE SEQUENCE cloudbreakevent_id_seq START WITH 1
                                          INCREMENT BY 1
                                          NO MINVALUE
                                          NO MAXVALUE
                                          CACHE 1;

CREATE TABLE cloudbreakevent (
    id bigint NOT NULL  DEFAULT nextval('cloudbreakevent_id_seq'),
    account character varying(255) NOT NULL,
    blueprintid bigint NOT NULL,
    blueprintname character varying(255),
    cloud character varying(255) NOT NULL,
    eventmessage character varying(255) NOT NULL,
    eventtimestamp timestamp without time zone NOT NULL,
    eventtype character varying(255) NOT NULL,
    instancegroup character varying(255),
    nodecount integer NOT NULL,
    owner character varying(255) NOT NULL,
    region character varying(255) NOT NULL,
    availabilityzone text,
    stackid bigint NOT NULL,
    stackname character varying(255) NOT NULL,
    stackstatus character varying(255) NOT NULL,
    clusterid bigint,
    clustername character varying(255),
    clusterstatus character varying(255)
);

ALTER TABLE ONLY cloudbreakevent
    ADD CONSTRAINT cloudbreakevent_pkey PRIMARY KEY (id);

DROP INDEX idx_structuredevent_userid_eventtype;
DROP INDEX idx_structuredevent_userid_eventtype_resourcetype_resourceid;
DROP INDEX idx_structuredevent_userid_eventtype_timestamp;

ALTER TABLE structuredevent DROP COLUMN IF EXISTS eventtype;
ALTER TABLE structuredevent DROP COLUMN IF EXISTS resourcetype;
ALTER TABLE structuredevent DROP COLUMN IF EXISTS resourceid;
ALTER TABLE structuredevent DROP COLUMN IF EXISTS timestamp;
ALTER TABLE structuredevent DROP COLUMN IF EXISTS account;
ALTER TABLE structuredevent DROP COLUMN IF EXISTS userid;
