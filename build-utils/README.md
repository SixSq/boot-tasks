SixSq Build Functions and Tasks
===============================

This repository provides a collection of functions and tasks that facilitate the builds of the SixSq software.  See below for more information on how to use these functions and tasks.

Dependency Management
---------------------

As the SixSq software consists of a large number of independent modules and repositories, central management of dependency versions is necessary to avoid problems arising from incompatible versions.  The `merge-defaults` function can be used within a module to complete local, partial definitions of dependencies with globally managed information, such as dependency versions and scope.
 
A typical `build.boot` file will contain a reference to these build tools as a test dependency and will then use the `merge-defaults` and the `set-env!` functions to define the full dependencies for the build.  A typical example looks like:

```
alpha beta gamma
```

