# clj-microblog

clj-microblog was an attempt at writing a microblogging site.

## Installation/Use

There are a few ways to start the server.

* Clone the repository.
* Change the credentials in [mail.conf](http://github.com/MayDaniel/clj-microblog/blob/master/mail.conf).
* Browse to the project's directory.
* `lein deps` or `cake deps`
* `(use 'run)` in a REPL or swank connection.
* Browse to [http://localhost:8080/](http://localhost:8080/)
* To stop and start the server, `(.stop server)` and `(.start server)`

## License

clj-microblog is licensed under the Eclipse Public License, the same as Clojure.
