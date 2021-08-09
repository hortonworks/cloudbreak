# CloudBreak Structured Events Service

A _Structured Event_ in CloudBreak is a <!-- todo: define what a structured event is -->

The structured events service provides <!-- todo: what does this service do, in a single sentence? -->

Structured events are written out to a number of backing "persistent" services through implementations of [CDPStructuredEventSenderService](./src/main/java/com/sequenceiq/cloudbreak/structuredevent/event/cdp/CDPStructuredEventSenderService.java).
* Database
* Kafka
* Audit
* Telemetry
* File

See [the wiki doc](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/1951170896/Integrate+to+CDP+Structured+Events+WIP) on using this module.

# Parts
Structured events are created by integrating with the service by way of Java APIs.
Structured events are retrieved from the UI by updating controller and repositories to make sure that events can be retrieved.

---
The core entrypoint of this module is the [`CDPStructuredEventFilter`](structuredevent-service-cdp/src/main/java/com/sequenceiq/cloudbreak/structuredevent/rest/filter/CDPStructuredEventFilter.java).

The filter is registered with another modules `EndpointConfig`, and turned on and off with the `${cdp.structuredevent.rest.contentlogging}` configuration value.

The filter intercepts all requests and responses, then checks if the URL matches an implementation of `CDPRestUrlParser`. If there is a matching parser, it populates a `Map<String, String>` and triggers creation of a Structured Event.

Additional information about the event is looked up using the appropriate `Repository` implementation. Repositories are looked up with the `CDPRepositoryLookupService`. Repository information is associated with a URL path using `RepositoryBasedDataCollector`, which looks for `Controller` classes annotated with `@AccountEntityType()`.

## `CDPRestUrlParser` implementation
The regular expression `Pattern` used in the implementation should match the URL pattern defined by the Swagger `*Endpoint` you want to match with.
e.g: [`FreeipaUrlParser`](freeipa/src/main/java/com/sequenceiq/freeipa/events/FreeipaUrlParser.java) matches on the requests sent to [`FreeIpaV1Endpoint`](freeipa-api/src/main/java/com/sequenceiq/freeipa/api/v1/freeipa/stack/FreeIpaV1Endpoint.java).

To make sure that the Repository look up service can find the repository for your Entity, be sure to annotate the repository with `@EntityType(entityClass = YOUR_ENTITY.class)`.

