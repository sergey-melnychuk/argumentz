![](https://github.com/sergey-melnychuk/argumentz/workflows/Java%20CI/badge.svg)

# Argumentz

Command-line arguments parser in Java. Small, simple and flexible.

# Install

```
<dependency>
  <groupId>io.github.sergey-melnychuk</groupId>
  <artifactId>argumentz</artifactId>
  <version>0.3.9</version>
</dependency>
```

# Example

```java
import io.github.sergey_melnychuk.Argumentz;

public class Main { 
    public static void main(String args[]) {
        Argumentz arguments = Argumentz.builder()
                // String parameter with default value provided (thus optional).
                .withParam('u', "user", "username to connect to the server", () -> "guest")
                // Mapped parameter with default value (optional as well).
                .withParam('p', "port", "port for server to listen", Integer::parseInt, () -> 8080)
                // Mapped required parameter (no default value)
                .withParam('s', "seconds", "timeout in seconds", Integer::parseInt)
                // String parameter, required as well
                .withParam('h', "host", "host for client to connect to")
                // Boolean flag, when provided match will return true for "-v" and "--verbose"
                .withFlag('v', "verbose", "enable extra logging")
                // Error handler to address misconfiguration - it must terminate the program (exit or exception).
                // Argumentz won't allow proceeding with program execution after error has been detected.
                // Argumentz also won't allow blowing up the stack by recursive call to `a.match(...)`.
                .withErrorHandler((RuntimeException e, Argumentz a) -> {
                    // print error and usage, then exit
                    System.err.println(e.getMessage() + "\n");
                    a.printUsage(System.out);
                    System.exit(1);
                    // or just re-throw the exception
                    // throw e;
                })
                .build();
        
        Argumentz.Match match = arguments.match(args);
        
        String user = match.get("user");
        int port = match.getInt("port");
        String host = match.get("host");
        int seconds = match.getInt("seconds");
        boolean verbose = match.getFlag("verbose");
        
        System.out.println(String.format("user=%s\nport=%d\nhost=%s\nseconds=%d\nverbose=%b", 
            user, port, host, seconds, verbose));
    }
}
```

```shell script
$ java -cp <...> Main -u admin -p 9000 -s 60 -h localhost -v
user=admin
port=9000
host=localhost
seconds=60
verbose=true
```

```shell script
# Use default values for 'user' and 'port'
$ java -cp <...> Main -s 60 -h localhost -v
user=guest
port=8080
host=localhost
seconds=60
verbose=true
```

```shell script
# Missing required parameter - error handler is fired
$ java -cp <...> Main -u admin -p 9000 -s 60
Missing required parameter: "-h" / "--host"

Usage: java -cp <...> <MainClass> [ARGUMENTS]
     -u --user          username to connect to the server (default: guest)
     -p --port          port for server to listen (default: 8080)
     -s --seconds       timeout in seconds (required)
     -h --host          host for client to connect to (required)
     -v --verbose       enable extra logging
```

# Release 

```
mvn release:clean release:prepare -P release
mvn release:perform -P release
```
