(ns console-test
  "Tests"
  (:require [octatrack.domain.banks :as banks]
            [octatrack.application.console :as console]
            [octatrack.infrastructure.utils :as utils]
            [octatrack.infrastructure.bank-files :as bank-files]
            [octatrack.infrastructure.project-folders :as project-folders]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest use-fixtures is testing run-tests]]
            [clojure.spec.test.alpha :as stest]))

;; ------------------------------------------------------------------------------------
;; Setup

(def test-folder-path "test/data/")
(def test-project-folder-path (str test-folder-path "test_source_project"))
(def test-config-path (str test-folder-path "test-config"))

(defn create-empty-bank-files [bank-number]
  (io/make-parents test-project-folder-path)
  (.mkdir (io/file test-project-folder-path))
  (let [bank-name (bank-files/format-bank-name bank-number)]
    (spit (str test-project-folder-path "/" bank-name ".strd") "BANK CONTENTS")
    (spit (str test-project-folder-path "/" bank-name ".work") "BANK CONTENTS")))

(defn create-test-data []
  (utils/create-directory test-folder-path)
  (spit test-config-path "1=16\n2=5\n3=2\n4=9\n5=3\n6=4\n7=12\n8=11\n9=13\n10=10\n11=0\n12=0\n13=0\n14=15\n15=0\n16=0")
  (doall (map #(create-empty-bank-files (inc %))
              (range banks/bank-count)))
  (doall (map #(spit (str test-project-folder-path "/" % ".work") nil)
              project-folders/other-project-file-prefixes))
  (println "Test Setup: Test banks & config created\n"))

(defn delete-test-data []
  (io/delete-file test-config-path)
  (utils/clear-directory test-folder-path)
  (println "\nTest Cleanup: Test banks & config cleared"))

(defn wrap-tests [test-fn]
  ;; `instrument` globally checks pre/post conditions of all function specs during runtime
  (stest/instrument)
  (create-test-data)
  (test-fn)
  (delete-test-data)
  (stest/unstrument))

;; Setup test data once before all tests run, and clean up test data after
;; This only runs with `run-tests`
(use-fixtures :once wrap-tests)


;; ------------------------------------------------------------------------------------
;; Helpers

(defmethod clojure.test/report
  ;; attach print out of test names when running tests
  :begin-test-var [m]
  (println "\nRunning Test:" (-> m :var meta :name)) "\n\n")

(defn test-commands
  "Conveniently wrap test paths + project names into command calls"
  [& options]
  (println "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
  (console/run-console (concat ["-s" test-project-folder-path] options)))


;; ------------------------------------------------------------------------------------
;; Unit Tests

(deftest valid-config-inputs
  (testing "Accept some valid inputs from config"
    (is (nil? (test-commands "-c" test-config-path)))))

(deftest valid-cli-inputs
  (testing "Accept some valid inputs from CLI options"
    (is (nil? (test-commands "-b" "16=1 15=2 14=0 13=7 12=3")))
    (test-commands "-h")))

(deftest invalid-cli-inputs 
  (testing "Reject some invalid inputs" 
    ;; missing required input
    (is (thrown? Exception (console/run-console "invalid type")))
    (is (thrown? Exception (console/run-console ["missing -s arg"])))
    (is (thrown? Exception (console/run-console ["missing -s arg" "-b" "1=2"])))
    ;; invalid input
    (is (thrown? Exception (test-commands "-b" "12")))
    (is (thrown? Exception (test-commands "-b" "1=four")))
    (is (thrown? Exception (test-commands "-b" "1=54")))
    (is (thrown? Exception (test-commands "-b" "1=2 1=3")))
    (is (thrown? Exception (test-commands "-b" "3=7 4=7")))))

;; Uncomment to run from REPL
;;(run-tests)