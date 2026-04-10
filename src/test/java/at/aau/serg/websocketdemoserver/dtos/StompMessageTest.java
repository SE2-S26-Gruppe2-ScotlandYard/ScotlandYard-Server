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

    @Test
    void testEqualsSameObject() {
        StompMessage message = new StompMessage("player1", "Hello");
        assertThat(message.equals(message)).isTrue();
    }

    @Test
    void testEqualsDifferentClass() {
        StompMessage message = new StompMessage("player1", "Hello");
        assertThat(message.equals("string")).isFalse();
    }

    @Test
    void testEqualsDifferentFrom() {
        StompMessage msg1 = new StompMessage("player1", "Hello");
        StompMessage msg2 = new StompMessage("player2", "Hello");
        assertThat(msg1).isNotEqualTo(msg2);
    }

    @Test
    void testEqualsDifferentText() {
        StompMessage msg1 = new StompMessage("player1", "Hello");
        StompMessage msg2 = new StompMessage("player1", "World");
        assertThat(msg1).isNotEqualTo(msg2);
    }

    @Test
    void testEqualsWithNullFrom() {
        StompMessage msg1 = new StompMessage(null, "Hello");
        StompMessage msg2 = new StompMessage(null, "Hello");
        StompMessage msg3 = new StompMessage("player1", "Hello");

        assertThat(msg1).isEqualTo(msg2);
        assertThat(msg1).isNotEqualTo(msg3);
    }

    @Test
    void testEqualsWithNullText() {
        StompMessage msg1 = new StompMessage("player1", null);
        StompMessage msg2 = new StompMessage("player1", null);
        StompMessage msg3 = new StompMessage("player1", "Hello");

        assertThat(msg1).isEqualTo(msg2);
        assertThat(msg1).isNotEqualTo(msg3);
    }

    @Test
    void testHashCodeConsistency() {
        StompMessage msg1 = new StompMessage("player1", "Hello");
        StompMessage msg2 = new StompMessage("player1", "Hello");

        assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
        assertThat(msg1.hashCode()).isEqualTo(msg1.hashCode()); // consistent
    }

    @Test
    void testHashCodeWithNullFields() {
        StompMessage msg1 = new StompMessage(null, null);
        StompMessage msg2 = new StompMessage(null, null);

        assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
    }
}