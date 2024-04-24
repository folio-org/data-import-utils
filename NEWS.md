## 1.13.0-SNAPSHOT 2024-XX-XX
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
