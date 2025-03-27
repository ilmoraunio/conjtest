(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.string :as str]
            [clojure.tools.build.api :as b]))

(def build-folder "target")
(def jar-content (str build-folder "/classes"))     ; folder where we collect files to pack in a jar

(def lib-name 'com.github.ilmoraunio/conjtest)
(def version (-> (slurp "resources/CONJTEST_LIB_VERSION")
                 str/trim))
(def is-release (Boolean/parseBoolean (System/getenv "RELEASE")))
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file-name (format "%s/%s.jar" build-folder (name lib-name)))

(defn clean
  [_]
  (b/delete {:path build-folder})
  (println (format "Build folder \"%s\" removed" build-folder)))

(defn jar
  [_]
  (clean nil)
  (b/copy-dir {:src-dirs   ["src"]
               :target-dir jar-content})
  (b/write-pom {:class-dir jar-content
                :lib       lib-name
                :version   (if is-release
                             version
                             (str version "-SNAPSHOT"))
                :basis     basis
                :src-dirs  ["src"]
                :scm {:url "https://github.com/ilmoraunio/conjtest"
                      :connection "scm:git:git://github.com/ilmoraunio/conjtest.git"
                      :tag (if is-release
                             version
                             (b/git-process {:git-args "rev-parse HEAD"}))}
                :pom-data [[:licenses
                            [:license
                             [:name "The MIT License (MIT)"]
                             [:url "https://mit-license.org/"]
                             [:distribution "repo"]]]]})
  (b/jar {:class-dir jar-content
          :jar-file jar-file-name})
  (println (format "Jar file created: \"%s\"" jar-file-name)))
