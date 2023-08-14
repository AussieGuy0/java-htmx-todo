package dev.anthonybruno.htmx;

import io.javalin.Javalin;
import j2html.tags.specialized.H2Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static j2html.TagCreator.*;

public class Server {

  private static final Logger log = LoggerFactory.getLogger(Server.class);

  // Create a H2 tag with an id.
  private static H2Tag createCounterElement(int count) {
    return h2("count: " + count)
        .withId("counter");
  }

  public static void main(String[] args) {
    var javalin = Javalin.create(config -> {
          config.staticFiles.enableWebjars();
        }
    );

    var counter = new AtomicInteger();
    javalin.get("/", ctx -> {
      var content = html(
          head(
              script().withSrc("/webjars/htmx.org/1.9.2/dist/htmx.min.js")
          ), (
              body(
                  h1("Hello world"),
                  createCounterElement(counter.get()),
                  button("Increment")
                      .attr("hx-post", "/increment")
                      .attr("hx-swap", "outerHTML")
                      .attr("hx-target", "#counter")
              )
          )
      );
      var rendered = "<!DOCTYPE html>\n" + content.renderFormatted();
      ctx.html(rendered);
    });

    javalin.post("/increment", ctx -> {
      var newCounter = createCounterElement(counter.incrementAndGet());
      ctx.html(newCounter.render());
    });

    javalin.after((ctx) -> {
      log.info(ctx.result());
    });

    javalin.start();

  }
}
