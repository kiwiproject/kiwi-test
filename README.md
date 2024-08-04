### Kiwi Test
[![Build](https://github.com/kiwiproject/kiwi-test/workflows/build/badge.svg)](https://github.com/kiwiproject/kiwi-test/actions?query=workflow%3Abuild)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_kiwi-test&metric=alert_status)](https://sonarcloud.io/dashboard?id=kiwiproject_kiwi-test)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_kiwi-test&metric=coverage)](https://sonarcloud.io/dashboard?id=kiwiproject_kiwi-test)
[![CodeQL](https://github.com/kiwiproject/kiwi-test/actions/workflows/codeql.yml/badge.svg)](https://github.com/kiwiproject/kiwi-test/actions/workflows/codeql.yml)
[![javadoc](https://javadoc.io/badge2/org.kiwiproject/kiwi-test/javadoc.svg)](https://javadoc.io/doc/org.kiwiproject/kiwi-test)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://img.shields.io/maven-central/v/org.kiwiproject/kiwi-test)](https://central.sonatype.com/artifact/org.kiwiproject/kiwi-test/)

Kiwi Test is a simple library that contains a variety of testing utilities that we have found useful over time in
various projects.

Almost all the dependencies in the POM have _provided_ scope, so that we don't bring in a ton of required dependencies.
This downside to this is that you must specifically add any required dependencies to your own POM in order to use a
specific feature in Kiwi Test.

The only required dependencies are guava, kiwi, and slf4j-api. Note that kiwi-test also marks most dependencies as
_provided_ scope, so in some cases you might need to add additional dependencies.  
