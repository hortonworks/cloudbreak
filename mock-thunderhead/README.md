## Testing CDL - DH integration

We need to mock CDL functionality in order to be able to provision DH with mocked CDL.
Problematic parts are the DB server CRN, related remote data context and other details like Ranger FQDN.
In order to supply everything we need for that, we need to invoke an API of thunderhead-mock, example command can be seen below.

The corresponding HMS database should be set up the same way as we set it up during DH creation (HMS table structure and initial data is created).
The Ranger FQDN should point to a working Ranger instance, preferably to another DL's working Ranger.

Step-by-step guide to create mocked CDL for an environment:
- Create a CDP environment.
- Setup DB server and properly configured HMS database in it (one way is to use HMS database of another VM form based DL).
- Using the CRN of the environment, create a CDL "entry" in thunderhead-mock (note: the generated entry is stored only in an in-memory database, after restart of mock, the entry will disappear and need to be created again):

```
cdp datalake create-aws-datalake --datalake-name examplecdl --environment-name crn:cdp:environments:us-west-1:cloudera:environment:a4fb7420-efa7-4eb8-94de-3fcddb274906 --cloud-provider-configuration instanceProfile=arn:aws:iam::12345678:instance-profile/fake-assumer,storageBucketLocation=s3a://fakelocation --scale CONTAINERIZED --runtime 7.2.18 --profile localhost
```
(Note: as a workaround, we are using `environmentName` field for environment CRN to set up mocked CDL and look up it later based on environment CRN in memory, this is a design gap of the public API, since request should use CRN.)

After CDL entry creation, database related data should be supplied as well using thunderhead-mock API call:
```
curl -H 'Content-Type: application/json' -X POST http://localhost:8080/api/v1/cdl/addDatabaseConfig --data-binary "@mockcdldb.json"
```

Content should look like this:
```
{
    "crn": "crn:cdp:sdxsvc:us-west-1:cloudera:instance:0f6a46c3-97d1-4927-a404-2a8ae8f67b69",
    "databaseServerCrn":"databaseServerCrn",
    "hmsDatabaseHost":"hmsDatabaseHost",
    "hmsDatabaseUser":"hmsDatabaseUser",
    "hmsDatabasePassword":"hmsDatabasePassword",
    "hmsDatabaseName":"hmsDatabaseName",
    "rangerFqdn": "rangerFqdn"
 }
```

After setting up mock CDL and related database (database server and HMS database), you can create a DH, and it will use mocked CDL based on the environment.
```
cdp datahub create-aws-cluster --cluster-definition '7.2.18 - Data Engineering Spark3 for AWS' --cluster-name exampledh --environment-name exampleenv --profile localhost
```

Note: do not forget to enable CDL by setting `auth.mock.cdl.enabled=true` for thunderhead-mock and `sdx.cdl.enabled=true` for environment and cloudbreak service.
