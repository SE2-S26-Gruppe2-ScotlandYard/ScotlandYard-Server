package at.aau.serg.websocketdemoserver.dtos;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class StompMessageTest {

    @Test
    void testAllArgsConstructor() {
        StompMessage message = new StompMessage("client1", "Hello World");

        assertThat(message.getFrom()).isEqualTo("client1");
        assertThat(message.getText()).isEqualTo("Hello World");
    }

    @Test
    void testSettersAndGetters() {
        StompMessage message = new StompMessage("", "");

        message.setFrom("server");
        message.setText("Response message");

        assertThat(message.getFrom()).isEqualTo("server");
        assertThat(message.getText()).isEqualTo("Response message");
    }
}