package io.github.sergey_melnychuk;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ArgumentzTest {

    private static Argumentz makeArgumentz() {
        return Argumentz.builder()
                .withParam('u', "user", "username to connect to the server", () -> "guest")
                .withParam('p', "port", "port for server to listen", Integer::parseInt, () -> 8080)
                .withParam('s', "seconds", "timeout in seconds", Integer::parseInt)
                .withParam('h', "host", "host for client to connect to")
                .withFlag('v', "verbose", "enable extra logging")
                .build();
    }

    @Test
    void testFullArguments() {
        String[] args = {"-u", "admin", "-p", "9000", "-s", "3600", "-h", "localhost", "-v"};
        Argumentz arguments = makeArgumentz();
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

        Argumentz arguments = makeArgumentz();
        assertThatThrownBy(() -> arguments.match(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to resolve parameter: \"-p\" / \"--port\": For input string: \"PORT_NUMBER\"");
    }

    @Test
    void testInvalidIntegerRequiredArgument() {
        String[] args = {"-u", "admin", "-p", "9000", "-s", "SECONDS", "-h", "localhost", "-v"};

        Argumentz arguments = makeArgumentz();
        assertThatThrownBy(() -> arguments.match(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to resolve parameter: \"-s\" / \"--seconds\": For input string: \"SECONDS\"");
    }

    @Test
    void testDefaultValue() {
        String[] args = {"-p", "9000", "-s", "3600", "-h", "localhost", "-v"};
        Argumentz arguments = makeArgumentz();
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
        Argumentz arguments = makeArgumentz();
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
        Argumentz arguments = makeArgumentz();
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
        Argumentz arguments = makeArgumentz();
        assertThatThrownBy(() -> arguments.match(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing required parameter: \"-h\" / \"--host\"");
    }

    @Test
    void testUsageMessage() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Argumentz arguments = makeArgumentz();
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
        Argumentz arguments = makeArgumentz();
        Argumentz.Match match = arguments.match(args);

        assertThat(match.all()).containsOnly(
                entry("-u", "admin"), entry("--user", "admin"),
                entry("-p", 9000), entry("--port", 9000),
                entry("-s", 3600), entry("--seconds", 3600),
                entry("-h", "localhost"), entry("--host", "localhost"),
                entry("-v", true), entry("--verbose", true));
    }

    // Example of a part of a complex argument Param
    private static class Range {
        private final long lo;
        private final long hi;

        private Range(long lo, long hi) {
            this.lo = lo;
            this.hi = hi;
        }

        public static Range of(long lo, long hi) {
            return new Range(lo, hi);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Range range = (Range) o;
            return lo == range.lo && hi == range.hi;
        }

        @Override
        public int hashCode() {
            return Objects.hash(lo, hi);
        }

        @Override
        public String toString() {
            return "Range{lo=" + lo + ", hi=" + hi + '}';
        }

        static Range parse(String s) {
            String[] chunks = s.split("/");
            return new Range(Long.parseLong(chunks[0]), Long.parseLong(chunks[1]));
        }
    }

    // Example of a complex argument
    private static class Param {
        private final Map<String, List<Range>> map;

        public Param(Map<String, List<Range>> map) {
            this.map = map;
        }

        static Param parse(String str) {
            Map<String, List<Range>> map = new HashMap<>();
            for (String kv : str.split(";")) {
                String[] entries = kv.split("=");
                String name = entries[0];
                List<Range> ranges = new ArrayList<>();
                for (String r : entries[1].split(",")) {
                    Range range = Range.parse(r);
                    ranges.add(range);
                }
                map.put(name, ranges);
            }
            return new Param(map);
        }

        static Param empty() {
            return new Param(new HashMap<>());
        }

        @Override
        public String toString() {
            return "Param{map=" + map + '}';
        }
    }


    @Test
    void testGenericParameterGetter() {
        Argumentz args = Argumentz.builder()
                .withParam('p', "param", "Some tricky parameter", Param::parse, Param::empty)
                .build();

        Argumentz.Match match = args.match(new String[]{"--param", "abc=123/456,789/012;def=777/333,999/222"});

        Param param = match.getAs(Param.class, "param");

        assertThat(param.map).containsOnly(
                entry("abc", Arrays.asList(Range.of(123L, 456L), Range.of(789L, 12L))),
                entry("def", Arrays.asList(Range.of(777L, 333L), Range.of(999L, 222L))));
    }

    @Test
    void testGenericParameterGetterFails() {
        Argumentz args = Argumentz.builder()
                .withParam('p', "param", "Some tricky parameter", Param::parse, Param::empty)
                .build();

        Argumentz.Match match = args.match(new String[]{"--param", "abc=123/456,789/012;def=777/333,999/222"});

        assertThatThrownBy(() -> match.getAs(Integer.class, "param"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to cast value 'Param{map={abc=[Range{lo=123, hi=456}, Range{lo=789, hi=12}], " +
                        "def=[Range{lo=777, hi=333}, Range{lo=999, hi=222}]}}' to class 'Integer'.");
    }

    @Test
    void testNonTerminalErrorHandlerWithFailingGenericParameterGetter() {
        Argumentz args = Argumentz.builder()
                .withParam('p', "param", "Some tricky parameter", Param::parse, Param::empty)
                .withErrorHandler((e, a) -> { /* empty */ })
                .build();

        Argumentz.Match match = args.match(new String[]{"--param", "abc=123/456,789/012;def=777/333,999/222"});

        assertThatThrownBy(() -> match.getAs(Integer.class, "param"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Error handler did not terminate execution flow of `getAs`.");
    }

    static class Ref<T> {
        public T value;
    }

    @Test
    void testCustomErrorHandlerForMissingStringArgument() {
        Ref<String> message = new Ref<>();

        Argumentz args = Argumentz.builder()
                .withParam('s', "str", "string arg")
                .withErrorHandler((e, a) -> {
                    message.value = e.getMessage();
                    throw new RuntimeException("terminate-the-app");
                })
                .build();

        assertThatThrownBy(() -> args.match(new String[]{"--name", "value"}))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("terminate-the-app");

        assertThat(message.value).isEqualTo("Missing required parameter: \"-s\" / \"--str\"");
    }

    @Test
    void testNonTerminalErrorHandlerForMissingStringArgument() {
        Argumentz args = Argumentz.builder()
                .withParam('s', "str", "string arg")
                .withErrorHandler((e, a) -> { /* empty */ })
                .build();

        assertThatThrownBy(() -> args.match(new String[]{"--name", "value"}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Error handler did not terminate execution flow of `match`.");
    }

    @Test
    void testCustomErrorHandlerForMissingMappedArgument() {
        Ref<String> message = new Ref<>();

        Argumentz args = Argumentz.builder()
                .withParam('i', "int", "integer arg", Integer::parseInt)
                .withErrorHandler((e, a) -> {
                    message.value = e.getMessage();
                    throw new RuntimeException("terminate-the-app");
                })
                .build();

        assertThatThrownBy(() -> args.match(new String[]{"--name", "value"}))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("terminate-the-app");

        assertThat(message.value).isEqualTo("Missing required parameter: \"-i\" / \"--int\"");
    }

    @Test
    void testNonTerminalErrorHandlerForMissingMappedArgument() {
        Argumentz args = Argumentz.builder()
                .withParam('i', "int", "integer arg", Integer::parseInt)
                .withErrorHandler((e, a) -> { /* empty */ })
                .build();

        assertThatThrownBy(() -> args.match(new String[]{"--name", "value"}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Error handler did not terminate execution flow of `match`.");
    }

    @Test
    void testCustomErrorHandlerForInvalidMappedArgument() {
        Ref<String> message = new Ref<>();

        Argumentz args = Argumentz.builder()
                .withParam('i', "int", "integer arg", Integer::parseInt)
                .withErrorHandler((e, a) -> {
                    message.value = e.getMessage();
                    throw new RuntimeException("terminate-the-app");
                })
                .build();

        assertThatThrownBy(() -> args.match(new String[]{"--int", "value"}))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("terminate-the-app");

        assertThat(message.value)
                .isEqualTo("Failed to resolve parameter: \"-i\" / \"--int\": For input string: \"value\"");
    }

    @Test
    void testNonTerminalErrorHandlerForInvalidMappedArgument() {
        Argumentz args = Argumentz.builder()
                .withParam('i', "int", "integer arg", Integer::parseInt)
                .withErrorHandler((e, a) -> { /* empty */ })
                .build();

        assertThatThrownBy(() -> args.match(new String[]{"--int", "value"}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Error handler did not terminate execution flow of `match`.");
    }

    @Test
    void testInfiniteRecursiveMatchIsDetected() {
        final String[] args = new String[]{"--message", "hello"};

        Argumentz argumentz = Argumentz.builder()
                .withParam('m', "match", "some description")
                .withErrorHandler((e, a) -> {
                    a.match(args);
                })
                .build();

        assertThatThrownBy(() -> argumentz.match(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Infinite recursive call to Argumentz.match detected.");
    }
}
