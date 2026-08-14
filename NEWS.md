## 3.0.0 In Progress
### Breaking changes
* Renamed the production artifact id from `data-import-utils` to `data-import-support`; consumers must update their dependency coordinates to `org.folio:data-import-support`
* Renamed `OkapiConnectionParams` to `ConnectionParams` with removal of deprecated methods
* Removed the `GenericHandlerAnswer` test utility and `StubObject` along with the `mockito-core` and `byte-buddy` dependencies

### Features
#### Test support
* Added the `data-import-test-support` module with shared integration-test infrastructure: PostgreSQL and Kafka Testcontainer bootstrap, JUnit 5 extensions (`PostgresExtension`, `KafkaExtension`), an abstract raml-module-builder integration-test base class (`BaseRestTest`), a Kafka producer/consumer harness, and tenant-enabling helpers including Enhanced Consortia Support (ECS)
* Added a shared WireMock server and RestAssured `spec`/`given()`/`postEntity()`/`putEntity()`/`getEntity()`/`deleteEntity()` helpers to `BaseRestTest` for stubbing calls to other modules and exercising the deployed module's API
* Added `postRequest()`/`putRequest()`/`getRequest()`/`deleteRequest()` helpers (with query-parameter overloads) to `BaseRestTest` that return a RestAssured `ValidatableResponse` for custom status/body assertions

### Bug fixes
* Description ([ISSUE](https://folio-org.atlassian.net/browse/ISSUE))

### Tech Dept
* Restructured the repository into a multi-module Maven project: the root is now an aggregator/parent POM (`data-import-utils-parent`) and the production code moved to the new `data-import-support` module
* Migrated tests from JUnit 4 to JUnit 5/6 (Jupiter)
* Enforced Checkstyle rules (`folio-java-checkstyle`) across the codebase and fixed resulting violations
* Added case-insensitive header handling test coverage for `RestUtil` and `ConnectionParams`

### Dependencies
* Bump `vertx` from `5.0.6` to `5.1.5`
* Bump `junit` from JUnit `4.13.2` to JUnit Jupiter `6.1.2`
* Bump `com.github.tomakehurst:wiremock-jre8` to `3.0.1`
* Bump `maven-compiler-plugin` to `3.15.0`
* Bump `maven-surefire-plugin` to `3.5.6`
* Bump `maven-source-plugin` to `3.4.0`
* Bump `maven-javadoc-plugin` to `3.12.0`
* Bump `maven-release-plugin` to `3.3.1`
* Bump `maven-enforcer-plugin` to `3.6.3`
* Add `maven-checkstyle-plugin 3.6.0` with `folio-java-checkstyle 1.2.0` and `checkstyle 13.7.0`
* Remove `mockito-core`, `net.bytebuddy:byte-buddy` and `maven-shade-plugin`

## 2.0.0 2026-04-10
* [MODDATAIMP-1208](https://issues.folio.org/browse/MODDATAIMP-1208) Change SYSTEM_USER_ENABLED configuration reading
* [MODDATAIMP-1248](https://issues.folio.org/browse/MODDATAIMP-1248) Upgrade data-import-utils to Vert.x 5.0

## 1.14.0 2025-03-07
* [MODDATAIMP-1125](https://folio-org.atlassian.net/browse/MODDATAIMP-1125) Remove dependency on mod-configuration
* [MODDATAIMP-1175](https://folio-org.atlassian.net/browse/MODDATAIMP-1175) Update to Java 21 data-import-utils library Sunflower R1 2025

## 1.13.0 2024-10-29
* [MODDATAIMP-1048](https://folio-org.atlassian.net/browse/MODDATAIMP-1048) Increase timeout default value for the requests in the data-import-utils module

## 1.12.1 2024-03-19
* [MODDICORE-398](https://issues.folio.org/browse/MODDICORE-398) Upgrade data-import-utils to jdk 17, RMB 35.2.0, Vert.x 4.5.4

## 1.11.0 2023-03-02
* [MODDATAIMP-785](https://issues.folio.org/browse/MODDATAIMP-785) Upgrade data-import-utils to RMB 35.0.6, Vert.x 4.3.8, config-client 5.9.1

## 1.10.0 2022-04-07
* [MODDATAIMP-665](https://issues.folio.org/browse/MODDATAIMP-665) Update dependencies (RMB, Vertx, log4j, ...) (CVE-2021-44228)

## 1.9.0 2021-06-18
* [MODDATAIMP-403](https://issues.folio.org/browse/MODDATAIMP-403) Fixed record type determination by leader

## 1.8.0 2021-02-05
* [MODDATAIMP-365](https://issues.folio.org/browse/MODDATAIMP-365) Upgrade data-import-utils to RAML Module Builder 32.x

## 1.7.0 2021-01-12
* [MODDATAIMP-351](https://issues.folio.org/browse/MODDATAIMP-351) Upgrade data-import-utils to Java 11.

## 1.6.0 2020-06-09
* Updated RAML Module Builder version to 30.0.2

## 1.5.0 2019-11-25
* Updated RAML Module Builder version to 27.1.1
* Updated mod-configuration-client version to 5.1.0

## 1.4.0 2019-09-09
* Fixed logic that determines whether http response contains json body
* Removed validation for partial success case using 500 code and json body

## 1.3.0 2019-07-19
* Added partial success case for async result validation
* Added util methods for exceptions handling under async methods calls and response status checking

## 1.2.0 2019-06-12
* Added MARC Record Analyzer
* Added exception for http 409 status code

## 1.1.0 2019-03-11
 * Implemented GenericHandlerAnswer

## 1.0.0 2019-02-05
 * Initial module setup
 * Implemented RestUtil, ConfigurationUtil and DaoUtil
