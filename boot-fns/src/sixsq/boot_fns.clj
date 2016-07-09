;; Copyright 2016, SixSq Sarl
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;; http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
(ns sixsq.boot-fns
  {:boot/export-tasks true}
  (:require [boot.core :refer [deftask]]
            [sixsq.boot-fns-impl :as impl]))

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
            deps (concat deps file-deps)
            completed-deps (impl/merge-defaults defaults deps)]
        (impl/update-deps! completed-deps)
        (next-task fileset)))))
