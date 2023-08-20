package dev.anthonybruno.htmx;

import io.javalin.Javalin;

import java.util.concurrent.atomic.AtomicInteger;

public class CounterServer {

  /**
   * Represents a single todo item.
   */
  public record Todo(String id, String content, boolean completed) {
  }

  private final Javalin javalin;


  public CounterServer() {
    var counter = new AtomicInteger();
    // Set up Javalin server.
    javalin = Javalin.create(config -> {
      config.staticFiles.enableWebjars();
      config.staticFiles.add("public");
    });

    javalin.get("/", ctx -> {
      //language=HTML
      var html = """
        <!DOCTYPE html>
        <html data-theme="light" lang="en">
          <head>
             <script src="/webjars/htmx.org/1.9.2/dist/htmx.min.js"></script>
             <link rel="stylesheet" href="/style/pico.min.css">
             <link rel="stylesheet" href="/style/styles.css">
             <title>Counter</title>
          </head>
          <body class='container'>
            <h1>Counter</h1>
            %s
            <button
              hx-post="/increment"
              hx-swap="outerHTML"
              hx-target="#counter"
            >Increment</button>
          </body>
        </html>
        """.formatted(createCounterComponent(counter.get()));
      ctx.header("Cache-Control", "no-store");

      ctx.html(html);
    });

    javalin.post("/increment", ctx -> {
      var newCount = counter.incrementAndGet();
      ctx.html(createCounterComponent(newCount));
    });
  }

  public String createCounterComponent(int count) {
    //language=HTML
    return """
    <h2 id="counter" style='text-align: center'>
      Count: %s
    </h2>
    """.formatted(count);
  }

  public void start() {
    javalin.start(9022);
  }

  public static void main(String[] args) {
    var server = new CounterServer();
    server.start();
  }
}
