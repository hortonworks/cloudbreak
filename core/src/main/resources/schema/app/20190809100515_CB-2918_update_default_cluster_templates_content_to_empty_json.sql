-- // CB-2918 update default cluster templates content to empty json
-- Migration SQL that makes the change goes here.

update clustertemplate set templatecontent = 'e30=' where status = 'DEFAULT'

-- //@UNDO
-- SQL to undo the change goes here.


