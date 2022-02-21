# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

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
* BDXL lookup now follows NAPTR replacements and directly uses the URL of regexp
* Consistent typing of URL information elements
* Generalised `org.holodeckb2b.bdxr.smp.impl.BDXLLocator` by parameterising the 
  NAPTR service name to use for finding the record holding the SMP URL 
* Refactored package structure to have a better separation between API and implementations. 
  Moved reference implementations of locators to sub projects

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

