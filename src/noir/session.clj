(ns noir.session
  "Stateful session handling functions. Uses a memory-store by default, but can use a custom store
by supplying a :session-store option to server/start."
  (:refer-clojure :exclude [get remove])
  (:use ring.middleware.session
        ring.middleware.session.memory)
  (:require [noir.options :as options]))

(declare *noir-session*)
(defonce mem (atom {}))

(defn put!
  "Associates the key with the given value in the session"
  [k v]
  (swap! *noir-session* assoc k v))

(defn get
  "Get the key's value from the session, returns nil if it doesn't exist."
  ([k] (get k nil))
  ([k default]
    (clojure.core/get @*noir-session* k default)))

(defn clear!
  "Remove all data from the session and start over cleanly."
  []
  (reset! *noir-session* {}))

(defn remove!
  "Remove a key from the session"
  [k]
  (swap! *noir-session* dissoc k))

(defn flash-put!
  "Store a value with a lifetime of one retrieval (on the first flash-get,
it is removed). This is often used for passing small messages to pages
after a redirect. A category can be supplied in association with the message,
otherwise 'message' is the default category."
  ([v] (flash-put! v "message")
  ([v c] (put! :_flash {:message v :category c}))

(defn flash-get
  "Retrieve the flash stored value. This will remove the flash from the
session."
  []
  (let [flash ((get :_flash) :message)]
    (remove! :_flash)
    flash))

(defn flash-get-full
  "Retrieve the stored flash value and the associated category. This will
remove the flash from the session."
  []
  (let [flash (get :_flash)]
    (remove! :_flash)
    flash))

(defn noir-session [handler]
  (fn [request]
    (binding [*noir-session* (atom (:session request))]
      (when-let [resp (handler request)]
        (assoc resp :session @*noir-session*)))))

(defn wrap-noir-session [handler]
  (-> handler
    (noir-session)
    (wrap-session {:store (options/get :session-store (memory-store mem))})))