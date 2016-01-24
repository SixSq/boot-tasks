(ns sixsq.boot-rpm-test
  (:require [clojure.test :refer :all]
            [boot.core :as boot]
            [sixsq.boot-rpm :refer :all]))

(def full-rpm-spec
  {:package-name "my-test-package"
   :package-version "0.0.1"
   :package-release "1"

   :summary "short summary"
   :description "long description"
   :distribution "CentOS"
   :group "software"
   :license "Apache 2.0"
   :url "http://my-test-package.example.org"
   :vendor "developer"

   :prefixes ["/opt/package" "/var/package"]
   :provides ["pkg-a" "pkg-b"]
   :requires [["req-a"] ["req-b" :gt "0.0.5"]]
   :conflicts [["con-a"] ["con-b" :lt "1.5.0"]]
   :symlinks [["/src1" "/dest1"] ["/src2" "/dest2"]]

   :pre-install-script "echo 'pre-install'"
   :post-install-script "echo 'post-install'"
   :pre-uninstall-script "echo 'pre-uninstall'"
   :post-uninstall-script "echo 'post-uninstall'"
   :pre-trans-script "echo 'pre-trans'"})

(defn run-boot []
  (boot/boot (apply rpm-build (mapcat identity full-rpm-spec))))

;; FIXME: Fails on boot/tmp-dir!, but runs fine from the REPL.
(deftest check-tasks
  (println (seq (.getURLs (java.lang.ClassLoader/getSystemClassLoader))))
  (is (run-boot)))
