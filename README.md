![](https://github.com/sergey-melnychuk/argumentz/workflows/Java%20CI/badge.svg)

# Argumentz

Command-line arguments parser in Java. Small, simple and flexible.

# Example

```java
import io.github.sergey_melnychuk.Argumentz;

public class Main { 
    public static void main(String args[]) {
        Argumentz arguments = Argumentz.builder()
                // String parameter with default value provided (thus optional).
                .withParam('u', "user", "username to connect to the server", "guest")
                // Mapped parameter with default value (optional as well).
                .withParam('p', "port", "port for server to listen", Integer::parseInt, () -> 8080)
                // Mapped required parameter (no default value)
                .withParam('s', "seconds", "timeout in seconds", Integer::parseInt)
                // String parameter, required as well
                .withParam('h', "host", "host for client to connect to")
                // Boolean flag, when provided match will return true for "-v" and "--verbose"
                .withFlag('v', "verbose", "enable extra logging")
                .build();
        
        arguments.printUsage(System.out);

        Argumentz.Match match = arguments.match(args);
        
        String user = match.get("user");
        int port = match.getInt("port");
        String host = match.get("host");
        int seconds = match.getInt("seconds");
        boolean verbose = match.getFlag("verbose");
        
        System.out.println(String.format("\nuser=%s\nport=%d\nhost=%s\nseconds=%d\nverbose=%b", 
            user, port, host, seconds, verbose));
    }
}
```

```shell script
$ java -cp <...> Main -u admin -p 9000 -s 60 -h localhost -v
Usage: java -cp <...> <MainClass> [ARGUMENTS]
     -u --user          username to connect to the server (default: guest)
     -p --port          port for server to listen (default: 8080)
     -s --seconds       timeout in seconds (required)
     -h --host          host for client to connect to (required)
     -v --verbose       enable extra logging

user=admin
port=9000
host=localhost
seconds=60
verbose=true
```

```shell script
# Use default values for 'user' and 'port'
$ java -cp <...> Main -s 60 -h localhost -v
<usage>

user=guest
port=8080
host=localhost
seconds=60
verbose=true
```

```shell script
# Missing required parameter - deal with regular exception as you wish
$ java -cp <...> Main -u admin -p 9000 -s 60
<usage>
Exception in thread "main" java.lang.IllegalArgumentException: Missing required parameter: "-h" / "--host"
	at io.github.sergey_melnychuk.Argumentz.match(Argumentz.java:73)
	at Main.main(Main.java:15)
```
