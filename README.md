# SoSy-Lab Common

[![Build Status](https://travis-ci.org/sosy-lab/java-common-lib.svg "Build Status")](https://travis-ci.org/sosy-lab/java-common-lib)
[![Apache 2.0 License](https://img.shields.io/badge/license-Apache--2-brightgreen.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

A collection of utilities for Java.

### [Java-Config](https://sosy-lab.github.io/java-common-lib/api/org/sosy_lab/common/configuration/package-summary.html)

  - Library for configuration options injection.

### [Java-Rationals](https://sosy-lab.github.io/java-common-lib/api/org/sosy_lab/common/rationals/package-summary.html)

  - Working with rationals and extended rationals, plus linear expressions.


[Javadoc documentation](https://sosy-lab.github.io/java-common-lib/) for entire project.

## Installation

### Using ANT and Ivy

If we use ANT with Ivy in your build process, you can download latest version of
SoSy-Lab Common from our repositories directly.
The updates to the Ivy repository are very frequent, and the latest version can
be easily found.

The dependency is:

```xml
<dependency org="org.sosy_lab" name="common" rev="0.3000" conf="core->runtime; contrib->sources"/>
```

And the Ivy repository URL is:

```xml
http://www.sosy-lab.org/ivy
```

### From Maven Central

The Common library is also published to Maven Central, however the volume of
updates is less frequent.
If you use Maven, the dependency is:

```xml
<dependency>
  <groupId>org.sosy-lab</groupId>
  <artifactId>common</artifactId>
  <version>0.3000</version>
</dependency>
```

Or for Gradle:

```
dependencies {
  compile 'org.sosy-lab:common:0.3000'
}
```

### Manually

The latest JAR can be downloaded directly from the Ivy repository, served at

```
http://www.sosy-lab.org/ivy/org.sosy_lab/common/
```

This option is least recommended, as the required dependencies (namely,
Guava and AutoValue) would need to be downloaded manually.
Download the `.ivy` file corresponding to the obtained `jar` to see
the dependencies and their location in the repository.
