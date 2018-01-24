(ns watch.core
  "Allos you to (mainly) reload your namespaces in an simple way or do other
  things when your files are modified. It is intended for clojurecript repl
  over nodejs or lumo.")

(def fs (js/require "fs"))
(def watched-files "Descriptors." (atom {}))
(def enough-millis 1000)
(def waiting-millis 700)

(defn now
  []
  (.getTime (js/Date.)))

(defn watch-entry
  [f watcher]
  {:file f
   :descriptor watcher
   :last-call (atom (now))})

(defn watched
  "Return the watched file names from the user hashmap."
  []
  (keys @watched-files))

(defn stop
  "Stop a watch using the watch descriptor or using
  a hashmap of descriptors (gotten from watch fn) and
  the file name to stop watching." 
  [f]
  (if-let [entry (get @watched-files f)]
    (do
      (.close (:descriptor entry))
      (swap! watched-files #(dissoc % f))
      f)))

(defn stop-all
  "Stop all watching from a hashmap of descriptors."
  []
  (doseq [w (watched)]
    (stop w)))

(defn watch
  "Observes a file  and executes a handler.
  Receives a hashmap to associate the watch descriptor using the
  filename as key.
  The handler function receives the ctx and the event descriptor.
  The handler are dispatched ONLY when the file is modified.
  You can only define a watch per file, as the watch descriptor is
  associated in the hm by key. If you need to associate many watches
  to a file, it is better to use hawk directly.

  Ej.
  (watch \"test.clj\" (fn wtestwt [ctx e] nil))
  "
  [f hn]
  (stop f)
  (let [fs (js/require "fs")
        watcher
        (.watch fs
                f
                #js {:encoding "buffer"}
                (fn wfa-handler
                  [ev fname]
                  (let [entry (get @watched-files f)
                        last-call-atom (:last-call entry)
                        last-call @last-call-atom
                        rnow (now)]
                    (if (>= (- rnow last-call) enough-millis)
                      (do
                        (reset! last-call-atom rnow)
                        (js/setTimeout
                          (fn []
                            (try
                              (hn ev fname)
                              (catch js/Error e
                                (println (.getMessage e) "\n"))))
                          waiting-millis)))))) ]
    (swap! watched-files #(assoc % f (watch-entry f watcher)))
    f))

(defmacro reload
  "Reload a namespace every time a file is modified. It can also dispatch
   multiple actions. The macro returns the watch descriptor alone.
   If the reloading fails the actions are not executed.
  
  Ej.
  (def b
     (reload (require '[clojure.string :as st])
             \"t.clj\" (println \"hola\") (println \"adios\")))"
  [rou file & actions]
  (let [rrou (seq (conj (vec rou) :reload))
        prrou (list 'println (list 'quote rou))]
    `(do
       ~rrou
       (watch ~file (fn [ctx# e#]
                      ~rrou
                      ~prrou
                      ~@actions)))))


