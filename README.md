# ETF SoapUI Adapters & Extensions

[![European Union Public Licence 1.2](https://img.shields.io/badge/license-EUPL%201.2-blue.svg)](https://joinup.ec.europa.eu/software/page/eupl)
[![owsgtl groovydoc](http://img.shields.io/badge/groovydoc-owsgtl-green.svg)](http://etf-validator.github.io/etf-sui-ae/groovydoc/index.html?de/interactive_instruments/etf/suim/Assert.html)

The etf-sui-owsgtl bundles groovy scripts for testing geo services
and manipulating the SoapUI project and the testing sequence - from within
SoapUI.

The etf-sui-plugin extensions library can compile groovy scripts at runtime,
generate Reports, it adds a schema validation assertion and works as an adapter for the SoapUI
test driver (ETF-SUITD). The Module can also be used as plugin in SoapUI.

The etf-sui-model-mapper component is used to map the SoapUI testing domain model to
the etf testing domain model.


&copy; 2017 European Union, interactive instruments GmbH. Licensed under the EUPL.

## About ETF

ETF is an open source testing framework for validating spatial data, metadata and web services in Spatial Data Infrastructures (SDIs). For documentation about ETF, see [http://docs.etf-validator.net](http://docs.etf-validator.net/).

Please report issues [in the GitHub issue tracker of the ETF Web Application](https://github.com/etf-validator/etf-webapp/issues).

ETF component version numbers comply with the [Semantic Versioning Specification 2.0.0](http://semver.org/spec/v2.0.0.html).

## Use in the SoapUI GUI application

See [the ETF developer manual](http://docs.etf-validator.net/Developer_manuals/Developing_Executable_Test_Suites.html#_development_environment_2) for information how to install the library in the SoapUI GUI application and use it for the development of Executable Test Suites.

## Build information

The project can be build and installed by running the gradlew.sh/.bat wrapper with:
```gradle
$ gradlew build install
```
