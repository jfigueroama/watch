# watch

Little library based on Hawk to reload namespaces when a file is modified.
It helps during development time. Also it includes other basic functions
to observe files and do things when they changes.

[![Clojars Project](https://img.shields.io/clojars/v/jfigueroama/watch.svg)](https://clojars.org/jfigueroama/watch)

## Usage

For now, please see the docs in the functions.

```clojure
(require '[watch.core :as watch])
; This will reload your some.core every time the source file changes and also will print Hello and Bye.
(reload (require '[some.core :as c]) "./src/some/core..clj" (println "Hello") (println "Bye"))
```

For more documentation please see the source code.

## License

Copyright © 2016 José Figueroa Martínez
All rights reserved.

BSD 2-Clause License
