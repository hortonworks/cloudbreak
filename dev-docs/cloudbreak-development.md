# Cloudbreak development

To make local development easier we introduced some environment configuration at Profile.

## Uluwatu

To configure local Uluwatu source location use ULUWATU_VOLUME_HOST variable. Please note that you need to generate your config files after variables changed. So the usual process is:
# edit Profile by adding for example ULUWATU_VOLUME_HOST
```
cbd kill
cbd regenerate
cbd start
```
If you make changes to html/css/client side js you just refresh the browser.
If you make changes in server side js, you have to restart the container
docker restart cbreak_uluwatu_1


## Database migration

By default deployer gets database migration scripts from containers, but in local development this way isn't passable. To set migration script locations please use the following variables:

- CB_SCHEMA_SCRIPTS_LOCATION: [cloudbreak-source-location]/core/src/main/resources/schema
- PERISCOPE_SCHEMA_SCRIPTS_LOCATION: [periscope-source-location]src/main/resources/schema
- UAA_SCHEMA_SCRIPTS_LOCATION: [sultans-source-location]/schema
