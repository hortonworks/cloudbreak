-- // CB-29528 adding deprecated flag to the 7.3.2 Nifi1 templates
-- Migration SQL that makes the change goes here.

update clustertemplate set featurestate='DEPRECATED' where name='7.3.2 - Flow Management Light Duty for AWS';
update clustertemplate set featurestate='DEPRECATED' where name='7.3.2 - Flow Management Heavy Duty for AWS';

update clustertemplate set featurestate='DEPRECATED' where name='7.3.2 - Flow Management Light Duty for Azure';
update clustertemplate set featurestate='DEPRECATED' where name='7.3.2 - Flow Management Heavy Duty for Azure';

update clustertemplate set featurestate='DEPRECATED' where name='7.3.2 - Flow Management Light Duty for Google Cloud';
update clustertemplate set featurestate='DEPRECATED' where name='7.3.2 - Flow Management Heavy Duty for Google Cloud';

update clustertemplate set featurestate='DEPRECATED' where name='7.3.2 - Flow Management Light Duty for YCloud';
update clustertemplate set featurestate='DEPRECATED' where name='7.3.2 - Flow Management Heavy Duty for YCloud';

-- //@UNDO
-- SQL to undo the change goes here.


