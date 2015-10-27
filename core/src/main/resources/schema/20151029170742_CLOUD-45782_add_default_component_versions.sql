-- // CLOUD-45782 add default component versions
-- Migration SQL that makes the change goes here.

INSERT INTO component (componenttype,
                       name,
                       stack_id,
                       attributes)
   SELECT 'CONTAINER' AS componentType,
          'MUNCHAUSEN' AS NAME,
          s.id AS stack_id,
          '{"name":"sequenceiq/munchausen","version":"0.5.5"}' AS attributes
     FROM stack s;


INSERT INTO component (componenttype,
                       name,
                       stack_id,
                       attributes)
   SELECT 'CONTAINER' AS componentType,
          'REGISTRATOR' AS NAME,
          s.id AS stack_id,
          '{"name":"sequenceiq/registrator","version":"v5.2"}' AS attributes
     FROM stack s;


INSERT INTO component (componenttype,
                       name,
                       stack_id,
                       attributes)
   SELECT 'CONTAINER' AS componentType,
          'AMBARI_DB' AS NAME,
          s.id AS stack_id,
          '{"name":"postgres","version":"9.4.1"}' AS attributes
     FROM stack s;


INSERT INTO component (componenttype,
                       name,
                       stack_id,
                       attributes)
   SELECT 'CONTAINER' AS componentType,
          'AMBARI_SERVER' AS NAME,
          s.id AS stack_id,
          '{"name":"sequenceiq/ambari-warmup","version":"2.1.0-consul"}' AS attributes
     FROM stack s;


INSERT INTO component (componenttype,
                       name,
                       stack_id,
                       attributes)
   SELECT 'CONTAINER' AS componentType,
          'AMBARI_AGENT' AS NAME,
          s.id AS stack_id,
          '{"name":"sequenceiq/ambari-warmup","version":"2.1.0-consul"}' AS attributes
     FROM stack s;

INSERT INTO component (componenttype,
                       name,
                       stack_id,
                       attributes)
   SELECT 'CONTAINER' AS componentType,
          'CONSUL_WATCH' AS NAME,
          s.id AS stack_id,
          '{"name":"sequenceiq/docker-consul-watch-plugn","version":"2.0.0-consul"}' AS attributes
     FROM stack s;

INSERT INTO component (componenttype,
                       name,
                       stack_id,
                       attributes)
   SELECT 'CONTAINER' AS componentType,
          'LOGROTATE' AS NAME,
          s.id AS stack_id,
          '{"name":"sequenceiq/logrotate","version":"v0.5.1"}' AS attributes
     FROM stack s;


INSERT INTO component (componenttype,
                       name,
                       stack_id,
                       attributes)
   SELECT 'CONTAINER' AS componentType,
          'BAYWATCH_CLIENT' AS NAME,
          s.id AS stack_id,
          '{"name":"sequenceiq/baywatch-client","version":"v0.5.3"}' AS attributes
     FROM stack s;


INSERT INTO component (componenttype,
                       name,
                       stack_id,
                       attributes)
   SELECT 'CONTAINER' AS componentType,
          'BAYWATCH_SERVER' AS NAME,
          s.id AS stack_id,
          '{"name":"sequenceiq/baywatch","version":"v0.5.3"}' AS attributes
     FROM stack s;


UPDATE component SET name = 'IMAGE' WHERE componentType = 'IMAGE' AND name = 'image';


-- //@UNDO
-- SQL to undo the change goes here.


UPDATE component SET name = 'image' WHERE componentType = 'IMAGE' AND name = 'IMAGE';

DELETE FROM component WHERE componentType = 'CONTAINER';