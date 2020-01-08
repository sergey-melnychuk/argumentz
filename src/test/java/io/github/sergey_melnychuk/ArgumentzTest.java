package io.github.sergey_melnychuk;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ArgumentzTest {

    Argumentz arguments = Argumentz.builder()
            .withParam('u', "user", "username to connect to the server", () -> "guest")
            .withParam('p', "port", "port for server to listen", Integer::parseInt, () -> 8080)
            .withParam('s', "seconds", "timeout in seconds", Integer::parseInt)
            .withParam('h', "host", "host for client to connect to")
            .withFlag('v', "verbose", "enable extra logging")
            .build();

    @Test
    void testFullArguments() {
        String[] args = {"-u", "admin", "-p", "9000", "-s", "3600", "-h", "localhost", "-v"};
        Argumentz.Match match = arguments.match(args);

        assertThat(match.get("user")).isEqualTo("admin");
        assertThat(match.get("host")).isEqualTo("localhost");
        assertThat(match.getInt("port")).isEqualTo(9000);
        assertThat(match.getInt("seconds")).isEqualTo(3600);
        assertThat(match.getFlag("verbose")).isTrue();
    }

    @Test
    void testInvalidIntegerDefaultArgument() {
        String[] args = {"-u", "admin", "-p", "PORT_NUMBER", "-s", "3600", "-h", "localhost", "-v"};

        assertThatThrownBy(() -> arguments.match(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to resolve parameter: \"-p\" / \"--port\": For input string: \"PORT_NUMBER\"");
    }

    @Test
    void testInvalidIntegerRequiredArgument() {
        String[] args = {"-u", "admin", "-p", "9000", "-s", "SECONDS", "-h", "localhost", "-v"};

        assertThatThrownBy(() -> arguments.match(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to resolve parameter: \"-s\" / \"--seconds\": For input string: \"SECONDS\"");
    }

    @Test
    void testDefaultValue() {
        String[] args = {"-p", "9000", "-s", "3600", "-h", "localhost", "-v"};
        Argumentz.Match match = arguments.match(args);

        assertThat(match.get("user")).isEqualTo("guest");
        assertThat(match.get("host")).isEqualTo("localhost");
        assertThat(match.getInt("port")).isEqualTo(9000);
        assertThat(match.getInt("seconds")).isEqualTo(3600);
        assertThat(match.getFlag("verbose")).isTrue();
    }

    @Test
    void testDefaultMappedValue() {
        String[] args = {"-u", "admin", "-s", "3600", "-h", "localhost", "-v"};
        Argumentz.Match match = arguments.match(args);

        assertThat(match.get("user")).isEqualTo("admin");
        assertThat(match.get("host")).isEqualTo("localhost");
        assertThat(match.getInt("port")).isEqualTo(8080);
        assertThat(match.getInt("seconds")).isEqualTo(3600);
        assertThat(match.getFlag("verbose")).isTrue();
    }

    @Test
    void testFlagDisabled() {
        String[] args = {"-u", "admin", "-p", "9000", "-s", "3600", "-h", "localhost"};
        Argumentz.Match match = arguments.match(args);

        assertThat(match.get("user")).isEqualTo("admin");
        assertThat(match.get("host")).isEqualTo("localhost");
        assertThat(match.getInt("port")).isEqualTo(9000);
        assertThat(match.getInt("seconds")).isEqualTo(3600);
        assertThat(match.getFlag("verbose")).isFalse();
    }

    @Test
    void testMissingRequiredArgument() {
        String[] args = {"-s", "3600"};
        assertThatThrownBy(() -> arguments.match(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing required parameter: \"-h\" / \"--host\"");
    }

    @Test
    void testUsageMessage() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        arguments.printUsage(ps);

        assertThat(baos.toString()).isEqualTo("Usage: java -cp <...> <MainClass> [ARGUMENTS]\n" +
                "     -u --user          username to connect to the server (default: guest)\n" +
                "     -p --port          port for server to listen (default: 8080)\n" +
                "     -s --seconds       timeout in seconds (required)\n" +
                "     -h --host          host for client to connect to (required)\n" +
                "     -v --verbose       enable extra logging\n");
    }

    @Test
    void testAllContents() {
        String[] args = {"-u", "admin", "-p", "9000", "-s", "3600", "-h", "localhost", "-v"};
        Argumentz.Match match = arguments.match(args);

        assertThat(match.all()).containsOnly(
                entry("-u", "admin"), entry("--user", "admin"),
                entry("-p", 9000), entry("--port", 9000),
                entry("-s", 3600), entry("--seconds", 3600),
                entry("-h", "localhost"), entry("--host", "localhost"),
                entry("-v", true), entry("--verbose", true));
    }
}
