(ns octatrack.application.console
  "Application layer for command line interface"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.spec.alpha :as s]
            [octatrack.application.api :as api]
            [octatrack.infrastructure.logger :as log])
  ;; Shim to generate java class when using AOT compilation, defaults to `:main true`
  (:gen-class))

;; Specification
(s/fdef run-console
  :args (s/cat :args (s/coll-of (s/and string?
                                       (complement str/blank?)))))

;; Implementation
(defn instructions [options-summary]
  (->> ["" "Instructions:"
        "Run the program with these commands: the project folder name (or path), followed by each pair of bank numbers to copy, ex. \"my_project --banks 1=2 2=4 3=7\""
        "If modifying a project like 'my_project', consider renaming it first to 'my_project_old' and then adding -t 'my_project' to make the new version have the current name"
        ""
        "Usage: java -jar ot-banks.jar -s <my-project-folder-name> [options]"
        ""
        "Inputs:" options-summary
        "Also see: https://github.com/tmprd/octatrack-banks#-usage"]
       (str/join \newline)))

(def cli-options
  [["-s" "--source <arg>" "Required: Source project folder path"
    :missing "-s is required. Please provide a project folder name, ex. -s my_project"]

   ["-t" "--target <arg>" "Optional: Target project folder path"
    :default-fn (fn [options] (str (:source options) "_new"))]

   ["-c" "--config <arg>" "Optional: Config file path"
    :parse-fn #(String. %)]

   ["-b" "--banks <args>" "Optional: Bank pairs"
    :parse-fn #(str/split (String. %) #" ")]

   ["-h" "--help"]])

(defn create-config [config-filename]
  (spit config-filename "1=\n2=\n3=\n4=\n5=\n6=\n7=\n8=\n9=\n10=\n11=\n12=\n13=\n14=\n15=\n16="))

(defn get-project-config [project-path]
  (let [default-config-path (str project-path ".config")]
    (log/debug (str "Default config path: " default-config-path))
    (when-not (.exists (io/file default-config-path))
      (create-config default-config-path)
      (throw (Exception. (str "Config file '" default-config-path "' didn't exist, so we created it for you. Please fill it out and retry."))))
    default-config-path))

(defn read-config [project-path provided-config-path]
  (let [config-path (or provided-config-path
                        (get-project-config project-path))]
    (log/printlog (str "Using config: " config-path))
    (str/split (slurp config-path) #"\n")))

(defn read-bank-commands [options]
  (if (:banks options)
    (do (log/printlog "Skipping config, using CLI args") (:banks options))
    (read-config (:source options) (:config options))))

(defn run-command [options]
  (let [bank-command-strs (read-bank-commands options)]
    (when (not bank-command-strs)
      (throw (Exception. "No bank assignments provided!")))
    (api/copy-project-command (:source options)
                              (:target options)
                              bank-command-strs)))
(defn run-console [args]
  (log/printlog "O C T A T R A C K")
  (let [{:keys [options arguments summary errors]} (parse-opts args cli-options)]
    (log/debug (str "CLI Args: " [arguments options]))
    (cond
      errors                    (throw (Exception. (str errors)))
      (:help options)           (println (instructions summary))
      :else                     (run-command options))))

(defn -main "CLI entry point here" [& args]
  (log/enable-log-file!)
  (log/debug args)
  (try
    (run-console args)
    (catch Exception e
      (log/debug e)
      (log/printlog (str "(!) Error: " (ex-message e)))
      (println (instructions (:summary (parse-opts nil cli-options)))))
    (finally
      (log/write-to-file!))))

;;; Uncomment to test in REPL, otherwise use `console_test`
;; (-main "-s" "projects/my_project_new" "-t" "my_new_project")

;;; This is needed for running this file directly, instead of through a "main" entry point specified in deps.edn
;;; However, it will cause the CLI to run twice if run via the entry point
;; (apply -main *command-line-args*)