# Clj-Microblog

Clj-Microblog was an attempt at writing a microblogging site.

## Usage

- If you're using Windows, the text will display better with ClearType text off.

- I've been displaying Clj-Microblog with Google Chrome, on Ubuntu, with a resolution of 1680x1050. I don't know how the site will look on much other.

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

Clj-Microblog is licensed under the Eclipse Public License, the same as Clojure.
