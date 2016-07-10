SixSq Build Functions and Tasks
===============================

This repository provides a collection of functions and tasks that facilitate the builds of the SixSq software.  See below for more information on how to use these functions and tasks.

Dependency Management
---------------------

As the SixSq software consists of a large number of independent modules and repositories, central management of dependency versions is necessary to avoid problems arising from incompatible versions.  The `merge-defaults` function can be used within a module to complete local, partial definitions of dependencies with globally managed information, such as dependency versions and scope.
 
A typical `build.boot` file will contain a reference to these build tools as a test dependency and will then use the `merge-defaults` and the `set-env!` functions to define the full dependencies for the build.  The first argument to `merge-defaults` is the dependency containing the defaults.  This dependency will not be a dependency of the project.

A typical `build.boot` example looks like:

    ;; define global properties and pull in SixSq build utilities
    (set-env!
     :version "3.8-SNAPSHOT"
     :dependencies '[[org.clojure/clojure "1.8.0"]
                     [sixsq/build-utils "0.1.1" :scope "test"]])
    
    (require '[sixsq.build-fns :refer [merge-defaults]])
    
    ;; set the dependencies, filling the version and other information
    ;; from the referenced defaults file
    (set-env!
     :dependencies
     #(concat % 
              (merge-defaults
               ['sixsq/default-deps (get-env :version)]
               '[[org.elasticsearch/elasticsearch nil :scope "test"]
                 [clj-time]
                 [org.clojure/core.async]])))
    
    ;; show the resolved dependencies, notice that missing information
    ;; has been pulled in from the defaults.
    (clojure.pprint/pprint (get-env :dependencies))

This will result in output like:

    $ boot show -f 
    [[org.clojure/clojure "1.8.0"]
     [sixsq/build-utils "0.1.1" :scope "test"]
     [org.elasticsearch/elasticsearch "2.3.3" :scope "test"]
     [clj-time "0.12.0"]
     [org.clojure/core.async "0.2.382" :exclusions [org.clojure/tools.reader]]]

Notice that the missing dependency information has been pulled in from the defaults.  A value such as :version can be used for the value of the version.  In this case, the value will be taken from the local environment.  This is useful for dependencies that use the same version as the software being built.

