# data-import-utils

Copyright (C) 2018–2026 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

Utilities for DataImport modules.

## Modules

This repository is a multi-module Maven project. The repository root is an
aggregator/parent POM (`org.folio:data-import-utils-parent`, packaging `pom`) that
centralises shared dependency and plugin management for the submodules:

* **[data-import-support](data-import-support)** — shared production utilities for FOLIO
  DataImport modules. Published as `org.folio:data-import-support`.
* **[data-import-test-support](data-import-test-support)** — shared integration-test
  infrastructure: PostgreSQL and Kafka Testcontainer bootstrap, a Kafka producer/consumer
  harness, JUnit 5 extensions (`PostgresExtension`, `KafkaExtension`), an abstract
  raml-module-builder integration-test base class (`BaseIntegrationTest`), and
  tenant-enabling helpers (including Enhanced Consortia Support). Published as
  `org.folio:data-import-test-support` and intended to be consumed with `test` scope.

### Using the test-support module

```xml
<dependency>
  <groupId>org.folio</groupId>
  <artifactId>data-import-test-support</artifactId>
  <version>${data-import-support.version}</version>
  <scope>test</scope>
</dependency>
```

The `PostgresExtension` and `KafkaExtension` start a single shared container per JVM. Register
them as `static` fields so the containers start once for the whole test run and stop
automatically when it finishes:

```java
@RegisterExtension
static PostgresExtension postgres = new PostgresExtension();

@RegisterExtension
static KafkaExtension kafka = new KafkaExtension();
```

The underlying helpers (`PostgresTestSupport`, `KafkaTestSupport`, `TenantTestSupport`,
`EcsTenantSupport`, `FolioHeaders`) can also be used directly for tests that do not use the
extensions.

For a full raml-module-builder integration test, extend `BaseIntegrationTest`: it starts the
shared PostgreSQL and Kafka containers, deploys the standard raml-module-builder `RestVerticle` on
a random free port, runs the Tenant API before the tests, and starts a per-class WireMock server
used to stub calls the module under test makes to other modules. Subclasses only supply the
module id and can immediately use the pre-built RestAssured `spec` and helper methods:

```java
class MyModuleIT extends AbstractRestVerticleTest {

  @Override
  protected String getModuleName() {
    return "mod-my-module-1.0.0";
  }

  @Test
  void shouldExposeApi() {
    stubGetJson("/users.*", "{\"users\":[]}"); // stub a call to mod-users
    MyEntity created = postEntity("/my-entities", new MyEntity(), 201, MyEntity.class);
    getEntity("/my-entities/" + created.getId(), 200, MyEntity.class);

    // for assertions on the raw response, use the ValidatableResponse-returning variants instead
    getRequest("/my-entities", Map.of("query", "name==foo"))
      .statusCode(200)
      .body("totalRecords", is(1));
  }
}
```

`BaseIntegrationTest` exposes:

* `mockServerUrl()` / `stubGetJson(urlPattern, jsonResponseBody)` — the base URL of the shared
  WireMock server, and a shortcut to stub a `GET` request with a JSON response. The module under
  test automatically receives this URL via the `X-Okapi-Url` header, so any outgoing calls it
  makes through the Okapi URL are routed to WireMock.
* `given()` — a RestAssured request builder pre-configured with the module's base URI, tenant and
  token headers.
* `postEntity`/`putEntity`/`getEntity`/`deleteEntity` — generic RestAssured helpers that assert an
  explicit expected HTTP status and deserialize the response body.
* `postRequest`/`putRequest`/`getRequest`/`deleteRequest` — generic RestAssured helpers that return
  a `ValidatableResponse` for callers to chain their own status/body assertions or extraction on.
  Each has an overload taking a `Map<String, ?>` of query parameters, e.g.
  `getRequest(path, Map.of("query", cql, "limit", 10))`.

## Additional information

* See project [MODDATAIMP](https://issues.folio.org/browse/MODDATAIMP)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

* Other FOLIO Developer documentation is at [dev.folio.org](https://dev.folio.org/)
