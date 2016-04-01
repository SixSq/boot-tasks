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
(ns sixsq.boot-deputil-impl
  (:require [boot.core :as boot :refer [deftask]]
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

(defn lookup [kw]
  (if (keyword? kw)
    (boot/get-env kw kw)
    kw))

(defn replace-entry [[k v]]
  [k (lookup v)])

(defn lookup-keywords [m]
  (into {} (map replace-entry m)))

(defn complete [defaults-map]
  (fn [dep]
    (let [[pkg opts] (dep->entry dep)
          default (get defaults-map pkg)
          result (lookup-keywords (merge default opts))]
      (entry->dep pkg result))))

(defn merge-defaults [defaults deps]
  (vec (map (complete defaults) deps)))

(defn deps-update-function [completed-deps]
  (fn [existing-deps]
    (vec (concat existing-deps completed-deps))))

(defn update-deps! [completed-deps]
  (boot/set-env! :dependencies (deps-update-function completed-deps)))
