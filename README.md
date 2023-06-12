# HTMX Java example

## WTF is this?

This is a groundbreaking piece of technology.
This application combines blockchain, AI and secrets stolen from Area 51 to bring you wealth beyond
your wildest dreams.

## Really?

Nah, it's just a todo list app.

![A beautiful TODO app](/screenshot.png)

## Great, we need more of them.

I agree! This one has a bit of a unique stack, it uses:

- Java (Yes, people still use this!)
- [HTMX](https://htmx.org/): Frontend """framework"""
- [picocss](https://picocss.com/): Minimal CSS library (the first result for "minimal css")
- [Javalin](https://javalin.io/): Backend web server.
- [j2html](https://j2html.com/): Html builder library.

Which allows the whole thing to be written a [single Java file](https://github.com/AussieGuy0/java-htmx-todo/blob/e8c174b8bfd3173e2b392601b460dc75411db7c5/src/main/java/dev/anthonybruno/htmx/Server.java).
There is not a single line of Javascript in this whole project!^

^ Yes, technically HTMX does bring in Javascript but ya know what I'm saying.

## Wow...there is an awful lot of inline styling in this project.

This is just an example application, not a fully fledged deploy-on-prod thing!

If you were doing it properly, maybe you would write it in css files, import tailwind or something.

## Alright alright, how do I run this?

Load it in your favourite Java IDE (like IntelliJ or....uh...) and run the main method in `Server`.
Access it via `localhost:8080` in a web browser.

In the root directory, we can also run it by:

1. `mvn package`
2. `java -jar target/htmx-1.0-SNAPSHOT.jar`
