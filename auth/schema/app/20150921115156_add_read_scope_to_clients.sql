-- // add read scope to clients
-- Migration SQL that makes the change goes here.

UPDATE oauth_client_details SET scope = 'cloudbreak.blueprints,cloudbreak.credentials,cloudbreak.events,cloudbreak.recipes,cloudbreak.stacks,cloudbreak.templates,cloudbreak.usages.account,cloudbreak.usages.global,cloudbreak.usages.user,openid,password.write,periscope.cluster,cloudbreak.blueprints.read,cloudbreak.templates.read,cloudbreak.credentials.read,cloudbreak.recipes.read,cloudbreak.networks.read,cloudbreak.securitygroups.read,cloudbreak.stacks.read' WHERE client_id='uluwatu';
UPDATE oauth_client_details SET scope = 'cloudbreak.templates,cloudbreak.blueprints,cloudbreak.credentials,cloudbreak.stacks,cloudbreak.events,cloudbreak.usages.global,cloudbreak.usages.account,cloudbreak.usages.user,cloudbreak.recipes,openid,password.write,cloudbreak.blueprints.read,cloudbreak.templates.read,cloudbreak.credentials.read,cloudbreak.recipes.read,cloudbreak.networks.read,cloudbreak.securitygroups.read,cloudbreak.stacks.read' WHERE client_id='cloudbreak_shell';

-- //@UNDO
-- SQL to undo the change goes here.
