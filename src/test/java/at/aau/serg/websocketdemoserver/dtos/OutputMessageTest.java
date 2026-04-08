package at.aau.serg.websocketdemoserver.dtos;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OutputMessageTest {

    @Test
    void testConstructor() {
        OutputMessage message = new OutputMessage("client1", "Hello World", "12:00:00");

        assertThat(message.getFrom()).isEqualTo("client1");
        assertThat(message.getText()).isEqualTo("Hello World");
        assertThat(message.getTime()).isEqualTo("12:00:00");
    }

    @Test
    void testGetters() {
        OutputMessage message = new OutputMessage("server", "Response", "13:30:45");

        // test getters (no setters)
        assertThat(message.getFrom()).isEqualTo("server");
        assertThat(message.getText()).isEqualTo("Response");
        assertThat(message.getTime()).isEqualTo("13:30:45");
    }
}