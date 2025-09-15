# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## 3.2.1
##### 2025-09-15
### Fixed
* Validation on Endpoint/TechnicalInfoUrl too strict [#2](https://github.com/holodeck-b2b/bdxr-smp-client/issues/2)

## 3.2.0
##### 2023-10-04
### Added
* Option to override the cached query result and query the server for latest version of the meta-data

### Changed
* `SMPLocatorException` now extends `SMPQueryException` to indicate that it's a problem in the query process

## 3.1.0
##### 2023-08-16
### Added
* Support for the Peppol wildcard identifier scheme in `org.holodeckb2b.bdxr.smp.peppol.DocumentID`
* Constructors in `org.holodeckb2b.bdxr.smp.peppol.DocumentID` and `org.holodeckb2b.bdxr.smp.peppol.ProcessID` that allow 
  setting of the scheme identifier.
* Add option to disable the _secure validation_ of the XML signature in the SMP response. 

## 3.0.1
##### 2023-02-27
### Changed
* Updated dependencies

## 3.0.0
##### 2022-05-03
### Added
* Methods on `ISMPClient` interface to get all _ServiceMetadata_ on a specific Service and _ServiceGroup_ of a
  Participant
* API for caching of query results, both locally and based on caching mechanism as defines in the OASIS SMP version 2.0
  standard
* Configurable number of maximum redirections to follow before throwing an exception
* Test cases for the result processors

### Changed
* Added _client_ to package names, i.e. `org.holodeckb2b.bdxr.smp.client`

## 2.0.0
##### 2021-04-15

**NOTE:** The code of this project was originally contained in the the [BDXR-Common](https://github.com/holodeck-b2b/bdxr-common) project.
To create a beter separation between generic and SMP client specific code, the SMP client code was split off into the current project.
Therefore the initial version number for this project is set to the next major version of the original project and the change log also continues from the original project.

### Added
* Support for the OASIS SMP v2 meta-data and formats
* Support for case sensitive identifiers
* Specific classes to represent PEPPOL identifiers due to different case sensitivity handling
* Test cases for the SMP Client

### Changed
* Split SMP data model definition and default implementation into separate project
  [BDXR-Common](https://github.com/holodeck-b2b/bdxr-common) project
* BDXL lookup now follows NAPTR replacements and directly uses URL of regexp
* Consistent typing of URL information elements
* Generalised `org.holodeckb2b.bdxr.smp.impl.BDXLLocator` by parameterising the
  NAPTR service name to use for finding the record holding the SMP URL

### Fixed
* Incorrect handling of redirect URL in the SMP Client
* Only verify the first signature on the SMP response document itself

### Removed
* Generic datamodel classes are now located in the [BDXR-Common](https://github.com/holodeck-b2b/bdxr-common) project
* bdxr-utils module as this is replaced by the generic utility project and the remaining SBDH
  parser is not used in the project and also not directly related to SMP/BDXL functions

## 1.1.0
##### 2019-03-06
### Added
* Support for the OASIS SMP v1 format
* Example client for looking up SMP registrations in the CEF connectivity test environment

### Fixed
* Incorrect host name generation for executing request in the PEPPOL network

## 1.0.0
##### 2018-09-19
### Added
* Initial release.

