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
(ns sixsq.build-fns
  {:boot/export-tasks true}
  (:require
    [boot.core :as boot :refer [deftask]]
    [boot.pod :as pod]
    [sixsq.build-fns-impl]))

(defn merge-defaults
  "This function merges version, scope, and other information provided
   in a list of default dependency specifications with the partial
   dependency specifications in the second argument. The default
   dependency information is taken from the 'default-deps-dep'
   specification.  The referenced package must provide a file called
   'default-deps.edn' at the root of the jar file.  See the README
   for more information."
  [default-deps-dep deps]
  (-> (update-in (boot/get-env) [:dependencies] conj default-deps-dep)
      (pod/make-pod)
      (pod/with-call-in (sixsq.build-fns-impl/merge-deps ~deps))))

(defn sixsq-nexus-url
  "Returns the appropriate SixSq nexus repository URL.  This pulls the
   version and edition from the environment.  Both must be defined."
  []
  (let [version (boot.core/get-env :version)
        edition (boot.core/get-env :edition)]
    (if (and version edition)
      (let [nexus-url "http://nexus.sixsq.com/content/repositories/"
            repo-type (if (re-find #"SNAPSHOT" version)
                        "snapshots"
                        "releases")]
        (str nexus-url repo-type "-" edition "-rhel7"))
      (throw (ex-info "both :version and :edition must be defined in environment" {})))))

(deftask lein-generate
         "Generate a leiningen `project.clj` file.
          This task generates a leiningen `project.clj` file based on
          the boot environment configuration, including project name
          and version (generated if not present), dependencies, and
          source paths. Additional keys may be added to the generated
          `project.clj` file by specifying a `:lein` key in the boot
          environment whose value is a map of keys-value pairs to add
          to `project.clj`.

          This is taken from the boot wiki page dealing with cursive
          integration."
         []
         (sixsq.build-fns-impl/generate-lein-project-file! :keep-project true))

