# Style Guide

The style guide of this project is the
[Google Java Style](https://google.github.io/styleguide/javaguide.html).

IMPORTANT: We use an automatic code formatter to format all our code.
Before committing, run `ant format-source` to reformat your changes
according to the style guide.

## Before Committing

Please run `ant all-checks` and check and clear any reported problems.

## Mailing List for Notifications

There is a [mailing list](https://groups.google.com/forum/#!forum/common-java-dev) with
 notifications for commits and CI failures.
It is recommended to subscribe to this list to get notified of failing builds.

You can also use this list for discussion among developers,
or use the [GitHub issue tracker](https://github.com/sosy-lab/java-common-lib/issues).

## Note for Eclipse Users

This project uses [Google AutoValue](https://github.com/google/auto/tree/master/value).
Eclipse does not pick up its annotation processor automatically.
You need to copy the file `.factorypath.template` to `.factorypath`
and adjust the path inside if your Eclipse project is not named "SoSy-Lab Common".

We recommend to install the [Eclipse Checkstyle plugin](https://checkstyle.org/eclipse-cs/)
and the [plugin with additional Checkstyle checks](https://github.com/sevntu-checkstyle/sevntu.checkstyle)
to be able to see the Checkstyle warnings in Eclipse
(the necessary configuration is included in the repository).
