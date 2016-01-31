(ns sixsq.boot-depmgt-impl
  (:require [boot.core :as boot :refer [deftask]]
            [boot.pod :as pod]
            [boot.util :as util]
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

(defn defaults-map [deps]
  (into {} (map dep->entry deps)))

(defn read-deps [fileset fname]
  (if-let [f (boot/tmp-get fileset fname)]
    (->> f
         boot/tmp-file
         slurp
         edn/read-string)
    []))

(defn read-defaults [fileset fname]
  (defaults-map (read-deps fileset fname)))

(defn complete [defaults-map]
  (fn [dep]
    (let [[pkg opts] (dep->entry dep)
          default (get defaults-map pkg)
          result (merge default opts)]
      (entry->dep pkg result))))      

(defn merge-defaults [defaults deps]
  (vec (map (complete defaults) deps)))

(defn deps-update-function [completed-deps]
  (fn [existing-deps]
    (vec (concat existing-deps completed-deps))))

(defn update-deps! [completed-deps]
  (boot/set-env! :dependencies (deps-update-function completed-deps)))
