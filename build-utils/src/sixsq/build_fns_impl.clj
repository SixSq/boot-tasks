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
(ns sixsq.build-fns-impl
  (:require
    [boot.core :as boot]
    [boot.pod :as pod]
    [boot.util :as butil]
    [boot.task.built-in]
    [clojure.edn :as edn]
    [clojure.java.io :as io]))

(defn fixed-map-as-dep
  "Returns the given dependency vector with :project and :version put at
  index 0 and 1 respectively and modifiers (eg. :scope, :exclusions,
  etc) next.

  This implementation changes 'flatten to mapcat/identity' to avoid
  flattening values that are themselves collections (e.g. exclusions)."
  [{:keys [project version] :as dep-map}]
  (let [kvs (remove #(or (some #{:project :version} %)
                         (= [:scope "compile"] %)) dep-map)]
    (vec (remove nil? (into [project version] (mapcat identity kvs))))))

(defn protected-dep-as-map
  "Uses the standard boot utility to convert a dependency specification
   to a map.  However, if an exception is thrown in the conversion
   (usually from an invalid spec like ['myproject :scope \"test\"]),
   then a modified exception will be thrown showing the problematic
   specification."
  [dep]
  (try
    (butil/dep-as-map dep)
    (catch Exception _
      (throw (ex-info (str "invalid dependency specification: '" dep "'\n") {})))))

(defn maybe-strip-scope
  "Strip the generated scope field unless it was explicitly defined."
  [dep m]
  (if (some #{:scope} dep)
    m
    (dissoc m :scope)))

(defn get-env
  "Defaults to using the global environment when available.  If
   not, then it will use the pod environment.  If neither exists,
   then nil will be returned."
  [k]
  (when-let [env (or (boot/get-env) pod/env)]
    (get env k)))

(defn defaults-map
  "Accepts a list of dependency specifications and returns a
   map where the values are the map representation of the dependency
   and the key is the value of :project in the dependency."
  [deps]
  (->> deps
       (map protected-dep-as-map)
       (remove nil?)
       (map (juxt :project identity))
       (into {})))

(defn read-default-deps
  "Reads a list of dependency specifications from the resource
   'default-deps.edn' which must be available on the classpath.
   Returns nil if the file cannot be found."
  ([]
   (read-default-deps nil))
  ([defaults-filename]
   (let [defaults-filename (or defaults-filename "default-deps.edn")]
     (when-let [f (io/resource defaults-filename)]
       (-> f
           slurp
           edn/read-string)))))

(defn remove-nil-values
  "Removes all keys in the map that have nil values."
  [m]
  (into {} (filter second m)))

(defn resolve-version
  "Looks up the value of :version key in the environment. If
   the value exists, it replaces the value in the map.  If not,
   the value is unchanged."
  [dep-map]
  (if-let [version (:version dep-map)]
    (assoc dep-map :version (or (get-env version) version))
    dep-map))

(defn complete-dep
  "Completes the information in the given dependency with
   the information from the defaults."
  [defaults dep]
  (let [{:keys [project] :as dep-map} (maybe-strip-scope dep (protected-dep-as-map dep))]
    (->> dep-map
         remove-nil-values
         (merge (get defaults project))
         resolve-version
         fixed-map-as-dep)))

(defn complete-deps
  "Iterates over the list of dependencies, completing values
   from the defaults for each dependency."
  [defaults deps]
  (vec (map (partial complete-dep defaults) deps)))

(defn merge-deps [deps]
  (complete-deps (defaults-map (read-default-deps nil)) deps))

(defn generate-lein-project-file! [& {:keys [keep-project]
                                      :or {keep-project true}}]
  (require 'clojure.java.io)
  (let [pfile ((resolve 'clojure.java.io/file) "project.clj")
        ; Only works when pom options are set using task-options!
        {:keys [project version]} (:task-options (meta #'boot.task.built-in/pom))
        prop #(when-let [x (get-env %2)] [%1 x])
        head (list* 'defproject (or project 'boot-project) (or version "0.0.0-SNAPSHOT")
                    (concat
                      (prop :url :url)
                      (prop :license :license)
                      (prop :description :description)
                      [:dependencies (get-env :dependencies)
                       :source-paths (vec (concat (get-env :source-paths)
                                                  (get-env :resource-paths)))]))
        proj (butil/pp-str head)]
    (if-not keep-project (.deleteOnExit pfile))
    (spit pfile proj)))
