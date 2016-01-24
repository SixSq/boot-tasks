(ns sixsq.boot-rpm-impl
  (:require
    [clojure.java.io :refer [file]]
    [boot.core :as boot]
    [boot.util :as util]
    [schema.core :as s]
    [clojure.string :as str])
  (:import (org.redline_rpm Builder)
           (org.redline_rpm.header Flags Architecture RpmType Os)
           (org.redline_rpm.payload Directive)
           (java.io RandomAccessFile)))

(def op->int {:gt Flags/GREATER
              :ge (bit-or Flags/GREATER Flags/EQUAL)
              :eq Flags/EQUAL
              :le (bit-or Flags/LESS Flags/EQUAL)
              :lt Flags/LESS})

(def kw->directive {:config     Directive/CONFIG
                    :doc        Directive/DOC
                    :exclude    Directive/EXCLUDE
                    :ghost      Directive/GHOST
                    :icon       Directive/ICON
                    :license    Directive/LICENSE
                    :missing-ok Directive/MISSINGOK
                    :none       Directive/NONE
                    :no-replace Directive/NOREPLACE
                    :policy     Directive/POLICY
                    :pubkey     Directive/PUBKEY
                    :readme     Directive/README
                    :specfile   Directive/SPECFILE
                    :unpatched  Directive/UNPATCHED})

(defn architecture
  [kw]
  (try
    (-> kw name str/upper-case Architecture/valueOf)
    (catch Exception _
      (throw (Exception. (str "invalid architecture: " (name kw)))))))

(defn os
  [kw]
  (try
    (-> kw name str/upper-case Os/valueOf)
    (catch Exception _
      (throw (Exception. (str "invalid OS name: " (name kw)))))))

(def op
  (apply s/enum (keys op->int)))

(def directive
  (apply s/enum (keys kw->directive)))

(def constraint
  (s/if #(= 1 (count %))
    [(s/one s/Str "package")]
    [(s/one s/Str "package") (s/one op "comparison") (s/one s/Str "version")]))

(def link
  [(s/one s/Str "source") (s/one s/Str "target")])

(def rpm-spec-schema
  {:output-directory                        s/Str

   :package-name                            s/Str
   :package-version                         s/Str
   :package-release                         s/Str

   :summary                                 s/Str
   :description                             s/Str
   :distribution                            s/Str
   :group                                   s/Str
   :license                                 s/Str
   :url                                     s/Str
   :vendor                                  s/Str

   :platform-arch                           s/Keyword       ;; uses :noarch by default
   :platform-os                             s/Keyword       ;; uses :linux by default

   :build-host                              s/Str           ;; uses local machine name if absent
   :packager                                s/Str           ;; uses current username if absent

   (s/optional-key :prefixes)               [s/Str]
   (s/optional-key :provides)               [s/Str]
   (s/optional-key :requires)               [constraint]
   (s/optional-key :conflicts)              [constraint]

   (s/optional-key :symlinks)               [link]

   (s/optional-key :pre-install-program)    s/Str
   (s/optional-key :pre-install-script)     s/Str
   (s/optional-key :post-install-program)   s/Str
   (s/optional-key :post-install-script)    s/Str
   (s/optional-key :pre-uninstall-program)  s/Str
   (s/optional-key :pre-uninstall-script)   s/Str
   (s/optional-key :post-uninstall-program) s/Str
   (s/optional-key :post-uninstall-script)  s/Str
   (s/optional-key :pre-trans-program)      s/Str
   (s/optional-key :pre-trans-script)       s/Str})

(def rpm-spec-defaults
  {:build-host             (.. java.net.InetAddress getLocalHost getHostName)
   :packager               (System/getProperty "user.name")
   :package-release        "1"
   :platform-arch          :noarch
   :platform-os            :linux

   :pre-install-program    "/bin/sh"
   :post-install-program   "/bin/sh"
   :pre-uninstall-program  "/bin/sh"
   :post-uninstall-program "/bin/sh"
   :pre-trans-program      "/bin/sh"})

(defn- create-directories
  [builder dirs]
  (when (seq dirs)
    (util/info "create-directories")
    (doseq [[path mode user group] dirs]
      (util/info "->" path mode user group)
      (.addDirectory builder path mode (Directive.) user group))))

;; FIXME: add processing of mode, directive, user, and group
(defn add-output-file [builder [path file]]
  (.addFile builder path file))

(defn add-output-files [builder output-files]
  (map #(add-output-file builder %) output-files))

(defn add-symlink [builder [source target]]
  (.addLink builder target source))                         ;; pay attention to the order here!

(defn add-symlinks [builder links]
  (map #(add-symlink builder %) links))

(defn add-provides [builder provides]
  (map #(.addProvides builder %) provides))

(defn add-require [builder [name op version]]
  (.addDependency builder name (op->int op 0) version))

(defn add-requires [builder conflicts]
  (map #(add-require builder %) conflicts))

(defn add-conflict [builder [name op version]]
  (.addConflicts builder name (op->int op 0) version))

(defn add-conflicts [builder conflicts]
  (map #(add-conflict builder %) conflicts))

;; FIXME: Need to make OS independent path.
(defn rpm-path
  [{:keys [output-directory package-name package-version package-release] :as spec}]
  (str output-directory "/" package-name "-" package-version "-" package-release ".rpm"))

(defn check-schema
  "check rpm spec schema and provide shorter error message if invalid"
  [spec]
  (try
    (s/validate rpm-spec-schema spec)
    (catch Exception e
      (let [error (:error (ex-data e))]
        (throw (Exception. (str "Invalid RPM specification: " error)))))))

(defn rpm-build
  "Java based RPM generator"
  [rpm-spec output-files]
  (let [spec (merge rpm-spec-defaults rpm-spec)]
    (check-schema spec)
    (let [file-channel (-> spec rpm-path file (RandomAccessFile. "rw") .getChannel)
          builder (Builder.)]
      (doto builder
        (.setBuildHost (:build-host spec))
        (.setDescription (:description spec))
        (.setDistribution (:distribution spec))
        (.setGroup (:group spec))
        (.setLicense (:name (:license spec)))
        (.setPackage (:package-name spec) (:package-version spec) (:package-release spec))
        (.setPackager (:packager spec))
        (.setPlatform ^Architecture (architecture (:platform-arch spec)) ^Os (os (:platform-os spec)))
        (.setSummary (:summary spec))
        (.setType RpmType/BINARY)
        (.setUrl (:url spec))
        (.setVendor (:vendor spec))

        (.setPreInstallProgram (:pre-install-program spec))
        (.setPreInstallScript ^String (:pre-install-script spec))
        (.setPostInstallProgram (:post-install-program spec "/bin/sh"))
        (.setPostInstallScript ^String (:post-install-script spec))
        (.setPreUninstallProgram (:pre-uninstall-program spec "/bin/sh"))
        (.setPreUninstallScript ^String (:pre-uninstall-script spec))
        (.setPostUninstallProgram (:post-uninstall-program spec "/bin/sh"))
        (.setPostUninstallScript ^String (:post-uninstall-script spec))
        (.setPreTransProgram (:pre-trans-program spec "/bin/sh"))
        (.setPreTransScript ^String (:pre-trans-script spec))
        (.setPrefixes (into-array (:prefixes spec [""])))
        (add-provides (:provides spec))
        (add-requires (:requires spec))
        (add-conflicts (:conflicts spec))
        (add-output-files output-files)
        (create-directories (:directories spec))
        (add-symlinks (:symlinks spec))
        (.build file-channel))
      (util/info "Created RPM package."))))
