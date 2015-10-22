# Cloudbreak development

To make local development easier we introduced some environment configuration at Profile.

## Uluwatu

To configure local Uluwatu source location use ULUWATU_VOLUME_HOST variable. Please note that you need to regenerate your config files after variable changed. So the usual process is:
# edit Profile by adding the line below:
```
export ULUWATU_VOLUME_HOST=/path/of/uluwatu/source
```
and than restart Cloudbreak:
```
cbd kill
cbd regenerate
cbd start
```

If you make changes to html/css/client side js you just refresh the browser.
If you make changes in server side js, you have to restart the container
docker restart cbreak_uluwatu_1

## Sultans

Sultans like Uluwatu has it's own variable named SULTANS_VOLUME_HOST. Please note that you need to reregenerate your config files after variable changed. So the usual process is:
# edit Profile by adding the line below:
```
export SULTANS_VOLUME_HOST=/path/of/sultans/source
```
and than restart Cloudbreak:
```
cbd kill
cbd regenerate
cbd start
```
If you make changes to html/css/client side js you just refresh the browser.
If you make changes in server side js, you have to restart the container
docker restart cbreak_sultans_1

## Database migration

By default deployer gets database migration scripts from containers, but in local development this way isn't passable. To set migration script locations please use the following variables:

- CB_SCHEMA_SCRIPTS_LOCATION: [cloudbreak-source-location]/core/src/main/resources/schema
- PERISCOPE_SCHEMA_SCRIPTS_LOCATION: [periscope-source-location]src/main/resources/schema
- UAA_SCHEMA_SCRIPTS_LOCATION: [sultans-source-location]/schema
