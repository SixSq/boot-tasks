(ns sixsq.boot-rpm
  {:boot/export-tasks true}
  (:require [boot.core :as boot :refer [deftask]]
            [boot.pod :as pod]
            [clojure.pprint :refer [pprint]]))

(def ^:private deps
  '[[org.redline-rpm/redline "1.2.2"]
    [prismatic/schema "1.0.4"]
    [org.slf4j/slf4j-simple "1.7.13"]])

(defn- create-pod [deps]
  (-> (boot/get-env)
      (update-in [:dependencies] into deps)
      pod/make-pod
      future))

(defn- commit [fileset tmp]
  (-> fileset
      (boot/add-resource tmp)
      boot/commit!))

(defn get-output-files [fileset]
  (doall
    (->> fileset
         boot/output-files
         (map (juxt boot/tmp-path #(.getAbsolutePath (boot/tmp-file %))))
         (into (sorted-map)))))

(defn run-scanner [path]
  (org.redline_rpm.Scanner/main (into-array String [path])))

(deftask rpm-dump
  "Print all information for an RPM package."
  [r rpm PATH str "Full path to existing RPM"]
  (let [pod (create-pod deps)
        tmp (boot/tmp-dir!)]
    (fn rpm-dump-middleware [next-task]
      (fn rpm-dump-handler [fileset]
        (boot/empty-dir! tmp)
        (pod/with-call-in @pod (sixsq.boot-rpm/run-scanner ~rpm))
        (next-task (commit fileset tmp))))))

(deftask rpm-build
  "Generates an RPM package from the fileset and metadata."
  [n package-name NAME str "Set the package name to NAME."
   v package-version VER str "Set the project version to VER."
   r package-release REL str "Set the package release version to REL. Default to '1'."

   s summary SUM str "Short summary of the package contents."
   d description DESC str "Set the project description to DESC."
   _ distribution DIST str "Set the distribution for the RPM."
   g group GRP str "Set the package group for the RPM."
   l license NAME str "Declare the license used for the software."
   u url URL str "Set the package url to URL."
   _ vendor NAME str "Set the package vendor in the RPM."

   _ platform-arch ARCH kw "Supported architecture. Defaults to 'noarch'."
   _ platform-os OS kw "Target operating system. Defaults to 'linux'."

   _ build-host HOST str "Set the host. Defaults to current hostname."
   _ packager NAME str "Set the packager. Defaults to current username."

   _ prefixes PRE [str] "Conj PRE onto the list of package prefixes."

   _ provides CAP [str] "Conj CAP onto the list of provided capabilities."
   _ requires REQ [edn] "Conj value onto list of requires."
   _ conflicts CON [edn] "Conj value onto list of conflicts."

   _ symlinks LINK [edn] "Conj value onto list of symlinks to create."

   _ pre-install-program SH str "Set pre-install shell. Default is '/bin/sh'."
   _ pre-install-script PROG str "Set the pre-install script."
   _ post-install-program SH str "Set post-install shell. Default is '/bin/sh'."
   _ post-install-script PROG str "Set the post-install script."
   _ pre-uninstall-program SH str "Set pre-uninstall shell. Default is '/bin/sh'."
   _ pre-uninstall-script PROG str "Set the pre-uninstall script."
   _ post-uninstall-program SH str "Set post-uninstall shell. Default is '/bin/sh'."
   _ post-uninstall-script PROG str "Set the post-uninstall script."
   _ pre-trans-program SH str "Set pre-transaction shell. Default is '/bin/sh'."
   _ pre-trans-script PROG str "Set the pre-transaction script."
   ]
  (let [pod (create-pod deps)
        tmp (boot/tmp-dir!)
        spec (assoc *opts* :output-directory (.getAbsolutePath tmp))]
    (fn rpm-build-middleware [next-task]
      (fn rpm-build-handler [fileset]
        (boot/empty-dir! tmp)
        (let [output-files (get-output-files fileset)]
          (pod/with-call-in @pod (sixsq.boot-rpm-impl/rpm-build ~spec ~output-files))
          (-> fileset
              (boot/add-asset tmp)
              (boot/commit!)
              (next-task)))))))
