# Holodeck SMP Client
This project provides a library of classes for developing SMP Clients based on BDXL and SMP specifications from the OASIS BDXR Technical Committee and their PEPPOL predecessor. The library provides a customisable SMP client that can be tailored for use with existing _Service Metadata Locators_ and _Publishers_ in a network to retrieve the meta-data of participants. It uses the generic data model classes from the [BDXR-Common](https://github.com/holodeck-b2b/bdxr-common) project for the participant meta-data maintained by the <i>Service Metadata Publishers</i> that abstracts from the actual specification. It includes implementations for both the OASIS specifications and the ones in use in the PEPPOL eDelivery Network.

__________________
Lead developer: Sander Fieten  
Code hosted at https://github.com/holodeck-b2b/bdxr-smp-client  
Issue tracker https://github.com/holodeck-b2b/bdxr-smp-client/issues  

##  Using
SMP client need to be built using the `org.holodeckb2b.bdxr.smp.client.api.SMPClientBuilder`. An example of a basic SMP client, suitable for use in the PEPPOL network, can be found in [peppol-smp/src/test/java/org/holodeckb2b/bdxr/smp/examples/PEPPOLSMPClient.java](peppol-smp/src/test/java/org/holodeckb2b/bdxr/smp/examples/PEPPOLSMPClient.java).
The example is a simple application that can query for meta-data in the PEPPOL acceptance environment. As shown in the example you need to configure the client so it fits to your specific setup.

The library is available on Maven Central. You just need to include the dependency/dependencies for the version of the SMP specification you need to support. Including any of these will also include the generic SMP client code. The artificats are:

_OASIS SMP Version 2.0_
```xml
<dependency>
    <groupId>org.holodeckb2b.bdxr.smp.client</groupId>
    <artifactId>oasis-smp2-client</artifactId>
    <version>3.1.0</version>
</dependency>
```

_OASIS SMP Version 1.0_
```xml
<dependency>
    <groupId>org.holodeckb2b.bdxr.smp.client</groupId>
    <artifactId>oasis-smp-client</artifactId>
    <version>3.1.0</version>
</dependency>
```

_PEPPOL SMP_
```xml
<dependency>
    <groupId>org.holodeckb2b.bdxr.smp.client</groupId>
    <artifactId>peppol-smp-client</artifactId>
    <version>3.1.0</version>
</dependency>
```

## Contributing
We are using the simplified Github workflow to accept modifications which means you should:
* create an issue related to the problem you want to fix or the function you want to add (good for traceability and cross-reference)
* fork the repository
* create a branch (optionally with the reference to the issue in the name)
* write your code
* commit incrementally with readable and detailed commit messages
* update the changelog with a short description of the changes including a reference to the issues fixed
* submit a pull request *against the 'next' branch* of this repository

If your contribution is more than a patch, please contact us beforehand to discuss how you can best submit the pull request.

### Submitting bugs
You can report issues directly on the [project Issue Tracker](https://github.com/holodeck-b2b/bdxr-smp-client/issues).
Please document the steps to reproduce your problem in as much detail as you can (if needed and possible include screenshots).

## Versioning
Version numbering follows the [Semantic versioning](http://semver.org/) approach.

## Licence
This software is licensed under the Lesser General Public License V3 (LGPLv3) which is included in the [LICENSE](LICENSE) file in the root of the project.

## Support
Commercial support is provided by Chasquis Consulting. Visit [Chasquis-Consulting.com](http://chasquis-consulting.com/holodeck-b2b-support/) for more information.
