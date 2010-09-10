# Clj-Microblog

Clj-Microblog is an attempt at writing a microblogging site.

This is my first web project, and I've pretty much been designing it
as I write it, so the code isn't great. However, it does show a use
of some libraries which are lacking documentation, and could prove
useful for learning how to use those.

## todo

- Complete refactor.
- Congomongo in place of StupidDB.

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
