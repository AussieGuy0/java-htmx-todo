package dev.anthonybruno.htmx;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.HandlerType;
import j2html.tags.specialized.LiTag;
import j2html.tags.specialized.UlTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static j2html.TagCreator.*;

public class Server implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(Server.class);

  /**
   * Represents a single todo item.
   */
  public record Todo(String id, String content, boolean completed) {
  }

  private final Javalin javalin;


  public Server() {
    // In-memory 'todo' store.
    var exampleTodo = new Todo(UUID.randomUUID().toString(), "Buy milk", false);
    var idToTodo = new LinkedHashMap<String, Todo>();
    idToTodo.put(exampleTodo.id, exampleTodo);

    // Set up Javalin server.
    javalin = Javalin.create(config -> {
      config.staticFiles.enableWebjars();
      config.staticFiles.add("public");
    });

    javalin.addHandler(HandlerType.GET, "/", ctx -> {
      var content = html(
        head(
          script().withSrc("/webjars/htmx.org/1.9.2/dist/htmx.min.js"),
          link().withRel("stylesheet").withHref("/style/pico.min.css")
        ),
        body(
           div(
            h1("Todo App"),
            createTodoList(idToTodo, Optional.empty())
          ).withClass("container")
        )
      );
      var rendered = "<!DOCTYPE html>\n" + content.render();
      ctx.header("Cache-Control", "no-store");
      ctx.html(rendered);
    });

    // Create new todo item.
    javalin.addHandler(HandlerType.POST, "/todos", ctx -> {
      var newContent = ctx.formParam("content");
      if (newContent == null || newContent.isBlank()) {
        ctx.html(createTodoList(idToTodo, Optional.of("Must be non-empty")).render());
        return;
      }
      var newTodo = new Todo(UUID.randomUUID().toString(), newContent, false);
      idToTodo.put(newTodo.id, newTodo);
    });

    // Update the content of a todo item.
    javalin.addHandler(HandlerType.POST, "/todos/{id}", ctx -> {
      var id = ctx.pathParam("id");
      var newContent = ctx.formParam("value");

      var updatedTodo = idToTodo.computeIfPresent(id, (_id, oldTodo) -> new Todo(id, newContent, oldTodo.completed));

      ctx.html(createTodoItem(updatedTodo, false).render());
    });

    // Toggle the completed status of a specified todo item.
    javalin.addHandler(HandlerType.POST, "/todos/{id}/toggle", ctx -> {
      var id = ctx.pathParam("id");

      var updatedTodo = idToTodo.computeIfPresent(id, (_id, oldTodo) -> new Todo(id, oldTodo.content, !oldTodo.completed));

      ctx.html(createTodoItem(updatedTodo, false).render());
    });

    // Returns an 'edit' view of a todo item.
    javalin.addHandler(HandlerType.POST, "/todos/{id}/edit", ctx -> {
      var id = ctx.pathParam("id");

      var todo = idToTodo.get(id);

      ctx.html(createTodoItem(todo, true).render());
    });

    // Basic logging of requests/responses.
    javalin.after((ctx) -> {
      log.info("{} {} {}", ctx.req().getMethod(), ctx.path(), ctx.status());
    });
  }

  private UlTag createTodoList(Map<String, Todo> idToTodo, Optional<String> errorMessage) {
    return ul()
      .withId("todo-list")
      .with(
        idToTodo.values().stream()
          .map(todo -> createTodoItem(todo, false))
      )
      .with(
        li(
          form(
            input()
//              .isRequired()
              .withType("text")
              .withName("content"),
            input()
              .withValue("Add")
              .withType("submit"),
              iff(errorMessage, (msg) -> div(msg))
          )
            .withStyle("display: flex;")
            .attr("hx-swap", "outerHTML")
            .attr("hx-target", "#todo-list")
            .attr("hx-post", "/todos")
        )
       .withStyle("list-style-type: none")
      );
  }

  public static LiTag createTodoItem(Todo todo, boolean editing) {
    var text = div(todo.content)
      .attr("hx-post", "/todos/" + todo.id + "/edit")
      .withStyle("flex-grow: 1; cursor: text;")
      .withCondStyle(todo.completed, "text-decoration: line-through; flex-grow: 1; cursor: text;");
    var editInput = input()
      .withValue(todo.content)
      .withName("value")
      .withType("text")
      .withStyle("flex-grow: 2;")
      .isAutofocus()
      .attr("hx-post", "/todos/" + todo.id)
      .attr("hx-target", "#todo-" + todo.id);
    var completeCheckbox = (todo.completed ?
      input()
       .isChecked() :
      input())
      .withStyle("flex-basis: 0; min-width: 20px")
      .withType("checkbox")
      .withCondDisabled(editing)
      .attr("hx-post", "/todos/" + todo.id + "/toggle");
    return li(
      completeCheckbox,
      editing ? editInput : text
    )
      .attr("hx-target", "#todo-" + todo.id)
      .attr("hx-swap", "outerHTML")
      .withId("todo-" + todo.id)
      .withStyle("display: flex; align-items: center;");
  }

  public void start() {
    javalin.start();
  }

  @Override
  public void close() throws Exception {
    javalin.close();
  }

  // Run the thing!
  public static void main(String[] args) {
    var server = new Server();
    server.start();
  }
}
