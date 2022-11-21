-- // CB-18190 Update custom blueprints with default value of GA for upgrade option
-- Migration SQL that makes the change goes here.

update blueprint
set blueprintupgradeoption = 'GA',
    lastupdated = 1671641400
where
    status = 'USER_MANAGED'
    and blueprintupgradeoption is null;

-- //@UNDO
-- SQL to undo the change goes here.

update blueprint
set blueprintupgradeoption = null
where
    status = 'USER_MANAGED'
    and blueprintupgradeoption = 'GA'
    and lastupdated = 1671641400;