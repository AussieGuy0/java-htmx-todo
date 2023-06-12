package dev.anthonybruno.htmx;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerTest {

  @Test
  void testHtml() {
    var todo = new Server.Todo("id", "content", false);
    var tag = Server.createTodoItem(todo, false);
    assertThat(tag.getTagName()).isEqualTo("li");
  }
}
