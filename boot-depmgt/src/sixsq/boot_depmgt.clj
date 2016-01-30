(ns sixsq.boot-depmgt
  {:boot/export-tasks true}
  (:require [boot.core :refer [deftask]]
            [sixsq.boot-depmgt-impl :as impl]))

(deftask set-deps!
  "Accepts a vector of dependencies, merges definitions from
  default-deps.edn (by default), and concatenates the dependencies to
  :dependencies in the boot environment."
  [d deps DEPS edn "Define DEPS, the vector of dependencies to set"
   _ deps-file FILE str "FILE (default-deps.edn) with dependency defaults"]
  (fn depmgt-middleware [next-task]
    (fn depmgt-handler [fileset]
      (let [deps-file (or deps-file "default-deps.edn")]
        (-> fileset
            (impl/read-defaults deps-file)
            (impl/merge-defaults deps)
            impl/update-deps!)
        (next-task fileset)))))
