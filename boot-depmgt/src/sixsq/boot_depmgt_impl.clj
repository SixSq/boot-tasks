(ns sixsq.boot-depmgt-impl
  (:require [boot.core :as boot :refer [deftask]]
            [boot.pod :as pod]
            [clojure.edn :as edn]))

(defn prepend-version-key [opts]
  (if (odd? (count opts))
    (cons :version opts)
    opts))

(defn dep->entry [[pkg & opts]]
  [pkg (apply hash-map (prepend-version-key opts))])

(defn entry->dep [pkg opts]
  (let [v (:version opts)
        v (when v (vector v))]
    (->> (dissoc opts :version)
         (mapcat identity)
         (concat [pkg] v)
         vec)))

(defn read-file [file]
  (if file
    (edn/read-string (slurp file))
    []))

(defn defaults-map [deps]
  (into {} (map dep->entry deps)))

(defn read-defaults [fileset fname]
  (->> fname
       (boot/tmp-get fileset)
       boot/tmp-file
       read-file
       defaults-map))

(defn complete [defaults-map]
  (fn [dep]
    (let [[pkg opts] (dep->entry dep)
          default (get defaults-map pkg)
          result (merge default opts)]
      (entry->dep pkg result))))      

(defn merge-defaults [defaults deps]
  (map (complete defaults) deps))

(defn deps-update-function [completed-deps]
  (fn [existing-deps]
    (concat existing-deps completed-deps)))

(defn update-deps! [completed-deps]
  (boot/set-env! :dependencies (deps-update-function [completed-deps])))
