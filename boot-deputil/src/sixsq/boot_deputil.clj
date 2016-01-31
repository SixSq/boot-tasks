(ns sixsq.boot-deputil
  {:boot/export-tasks true}
  (:require [boot.core :refer [deftask]]
            [sixsq.boot-deputil-impl :as impl]))

(deftask set-deps!
  "Add the dependencies defined in a list and/or in the 'deps.edn' source
   file to the :dependencies key of the boot environment.  The version and
   other options (e.g. scope) will be merged with the information given in
   the 'default-deps.edn' file.

   The usual usage is to define the dependencies in the source file
   'deps.edn' for a project and then use the `checkout` task to pull in
   a jar file containing a centrally-managed set of dependencies.

   Note that this task (or a pipeline containing this task) should be run
   as part of the compilation of the 'build.boot' script.  If run as part
   of a task, the defined dependencies will not be transmitted to other
   tasks in the pipeline."
  [d deps DEPS edn "Define DEPS, the vector of dependencies to set"
   _ default-deps-file FILE str "FILE (default-deps.edn) with dependency defaults"
   _ deps-file FILE str "FILE (deps.edn) with project dependencies"]
  (fn deputil-middleware [next-task]
    (fn deputil-handler [fileset]
      (let [default-deps-file (or default-deps-file "default-deps.edn")
            deps-file (or deps-file "deps.edn")
            defaults (impl/read-defaults fileset default-deps-file)
            file-deps (impl/read-deps fileset deps-file)
            deps (concat deps file-deps)]
        (impl/update-deps! defaults deps)
        (next-task fileset)))))
