package dev.anthonybruno.htmx;

import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import j2html.attributes.Attr;
import j2html.tags.DomContent;
import j2html.tags.specialized.HtmlTag;
import j2html.tags.specialized.LiTag;
import j2html.tags.specialized.UlTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
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
      var content = createPage("My Todos",
        div(
          h1("My Todos"),
          createTodoList(idToTodo.values(), null)
        )
      );
      var rendered = "<!DOCTYPE html>\n" + content.render();
      ctx.header("Cache-Control", "no-store");
      ctx.html(rendered);
    });

    javalin.addHandler(HandlerType.GET, "/other-page", ctx -> {
      var content = createPage("Other Page",
        div(
          h1("Other Page"),
          div("Look how seamless that loaded!")
        )
      );
      var rendered = "<!DOCTYPE html>\n" + content.render();
      ctx.header("Cache-Control", "no-store");
      ctx.html(rendered);
    });

    // Create new todo item.
    javalin.post("/api/todos", ctx -> {
      var newContent = ctx.formParam("content");
      if (newContent == null || newContent.isBlank()) {
        ctx.html(createTodoList(idToTodo.values(), "Must be non-empty").render());
        return;
      }
      if (newContent.length() > 30) {
        ctx.html(createTodoList(idToTodo.values(), "Exceeds limit of 30 characters").renderFormatted());
        return;
      }
      var newTodo = new Todo(UUID.randomUUID().toString(), newContent, false);
      idToTodo.put(newTodo.id, newTodo);
      ctx.html(createTodoList(idToTodo.values(), null).renderFormatted());
    });

    javalin.post("/api/todos/validate", ctx -> {
      var newContent = ctx.formParam("content");
      if (newContent.length() > 30) {
        ctx.html(span("Exceeds limit of 30 characters!").render());
        return;
      }
      ctx.html(span("").render());
    });

    // Update the content of a todo item.
    javalin.post("/api/todos/{id}", ctx -> {
      var id = ctx.pathParam("id");
      var newContent = ctx.formParam("value");
      if (newContent == null || newContent.isBlank()) {
        ctx.status(204);
        return;
      }

      var updatedTodo = idToTodo.computeIfPresent(id, (_id, oldTodo) -> new Todo(id, newContent, oldTodo.completed));

      ctx.html(createTodoItem(updatedTodo, false).renderFormatted());
    });

    // Toggle the completed status of a specified todo item.
    javalin.post("/api/todos/{id}/toggle", ctx -> {
      var id = ctx.pathParam("id");

      var updatedTodo = idToTodo.computeIfPresent(id, (_id, oldTodo) -> new Todo(id, oldTodo.content, !oldTodo.completed));

      ctx.html(createTodoItem(updatedTodo, false).renderFormatted());
    });

    // Returns an 'edit' view of a todo item.
    javalin.post("/api/todos/{id}/edit", ctx -> {
      var id = ctx.pathParam("id");

      var todo = idToTodo.get(id);

      ctx.html(createTodoItem(todo, true).renderFormatted());
    });

    // Basic logging of requests/responses.
    javalin.after((ctx) -> {
      log.info("{} {} {}", ctx.req().getMethod(), ctx.path(), ctx.status());
    });
  }

  private HtmlTag createPage(String title, DomContent domContent) {
    return html(
      head(
        script().withSrc("/webjars/htmx.org/1.9.2/dist/htmx.min.js"),
        link().withRel("stylesheet").withHref("/style/pico.min.css"),
        link().withRel("stylesheet").withHref("/style/styles.css"),
        title(title)
      ),
      body(
        div(
          nav(
            ul(
              li(strong("Todo App"))
            ),
            ul(
              li(a("Todos").withHref("/")),
              li(a("Other Page").withHref("/other-page"))
            )
          ),
          domContent
        ).withClass("container")
      ).attr("hx-boost", true)
    ).attr("data-theme", "light");
  }

  private UlTag createTodoList(Collection<Todo> todos, @Nullable String errorMessage) {
    return ul()
      .withId("todo-list")
      .with(
        todos.stream()
          .map(todo -> createTodoItem(todo, false))
      )
      .with(
        li(
          createAddTodoForm(errorMessage)
        )
          .withStyle("list-style-type: none")
      );
  }

  private DomContent createAddTodoForm(@Nullable String errorMessage) {
    var errorMessageText = errorMessage != null ? errorMessage : "";
    return div(
      form(
        aClass("add-todo-form"),
        input(aClass("add-todo-form__text-input"))
          .isRequired()
          .withType("text")
          .withName("content")
          .attr("hx-target", "#error-message")
          .attr("hx-swap", "innerHTML")
          .attr("hx-post", "/api/todos/validate")
          .attr("hx-trigger", "keyup change delay:200ms")
        ,
        input(aClass("add-todo-form__button"))
          .withValue("Add")
          .withType("submit")
      )
        .attr("hx-swap", "outerHTML")
        .attr("hx-target", "#todo-list")
        .attr("hx-post", "/api/todos"),
      div(id("error-message"), errorMessageText)
    );

  }

  public static LiTag createTodoItem(Todo todo, boolean editing) {
    var text = div(todo.content)
      .attr("hx-post", "/api/todos/" + todo.id + "/edit")
      .withStyle("flex-grow: 1; cursor: text;")
      .withCondStyle(todo.completed, "text-decoration: line-through; flex-grow: 1; cursor: text;");
    var editInput = input()
      .withValue(todo.content)
      .withName("value")
      .withType("text")
      .withStyle("flex-grow: 2;")
      .isRequired()
      .isAutofocus()
      .attr("hx-trigger", "blur, change")
      .attr("hx-post", "/api/todos/" + todo.id);
    var completeCheckbox = (todo.completed ?
      input()
        .isChecked() :
      input())
      .withStyle("flex-basis: 0; min-width: 20px")
      .withType("checkbox")
      .withCondDisabled(editing)
      .attr("hx-post", "/api/todos/" + todo.id + "/toggle");
    return li(
      completeCheckbox,
      editing ? editInput : text
    )
      .attr("hx-target", "this")
      .attr("hx-swap", "outerHTML")
      .withStyle("display: flex; align-items: center;");
  }

  private static Attr.ShortForm aClass(String className) {
    return Attr.shortFormFromAttrsString("." + className);
  }

  private static Attr.ShortForm id(String id) {
    return Attr.shortFormFromAttrsString("#" + id);
  }

  public void start() {
    javalin.start(9001);
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
