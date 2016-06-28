# SUI adapter and extensions

[![Apache License 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![owsgtl groovydoc](http://img.shields.io/badge/groovydoc-owsgtl-green.svg)](http://interactive-instruments.github.io/etf-sui-ae/etf-sui-owsgtl/doc/)
[![Build Status](https://services.interactive-instruments.de/etfdev-ci/buildStatus/icon?job=etf-bsx-ae)](https://services.interactive-instruments.de/etfdev-ci/job/etf-bsx-ae/)
[![Latest version](http://img.shields.io/badge/latest%20version-1.3.8-blue.svg)](https://services.interactive-instruments.de/etfdev-af/etf-public-releases/de/interactive-instruments/etf/etf-owsgtl-1.3.8.zip)

The etf-sui-owsgtl bundles some simple groovy scripts for testing geo services
and manipulating the SoapUI project and the testing sequence - from within
SoapUI.

The etf-sui-plugin extensions library can compile groovy scripts at runtime,
generate Reports, it adds a schema validation assertion and works as an adapter for the SoapUI
test driver (ETF-SUITD). The Module can also be used as plugin in SoapUI.

The etf-sui-model-mapper component is used to map the SoapUI testing domain model to
the etf testing domain model.

Please use the [etf-webapp project](https://github.com/interactive-instruments/etf-webapp) for
reporting [issues](https://github.com/interactive-instruments/etf-webapp/issues) or
[further documentation](https://github.com/interactive-instruments/etf-webapp/wiki).


The project can be build and installed by running the gradlew.sh/.bat wrapper with:
```gradle
$ gradlew build install
```

ETF component version numbers comply with the [Semantic Versioning Specification 2.0.0](http://semver.org/spec/v2.0.0.html).

ETF is an open source test framework developed by [interactive instruments](http://www.interactive-instruments.de/en) for testing geo network services and data.
