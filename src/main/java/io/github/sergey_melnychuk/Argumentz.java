package io.github.sergey_melnychuk;

import java.io.PrintStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Argumentz {
    private static final Function<String, String> IDENTITY = x -> x;

    public interface Builder {
        <T> Builder withParam(char chr, String name, String desc, Function<String, T> mapper, Supplier<T> defaultValue);
        <T> Builder withParam(char chr, String name, String desc, Function<String, T> mapper);
        Builder withParam(char chr, String name, String desc, Supplier<String> defaultValue);
        Builder withParam(char chr, String name, String desc);
        Builder withFlag(char chr, String name, String desc);
        Builder withErrorHandler(BiConsumer<RuntimeException, Argumentz> errorHandler);
        Argumentz build();
    }

    public interface Match {
        Map<String, Object> all();
        String get(String name);
        Integer getInt(String name);
        boolean getFlag(String name);
        <T> T getAs(Class<T> clazz, String name);
    }

    private final Map<String, String> names;
    private final Map<String, Function<String, ?>> mappers;
    private final Set<String> flags;
    private final String usage;
    private final BiConsumer<RuntimeException, Argumentz> errorHandler;

    public Argumentz(Map<String, String> names,
                  Map<String, Function<String, ?>> mappers,
                  Set<String> flags,
                  String usage,
                  BiConsumer<RuntimeException, Argumentz> errorHandler) {
        this.names = names;
        this.mappers = mappers;
        this.flags = flags;
        this.usage = usage;
        this.errorHandler = errorHandler;
    }

    public Match match(String[] args) {
        long depth = Arrays.stream(Thread.currentThread().getStackTrace())
                .map(StackTraceElement::toString)
                .filter(s -> s.contains("io.github.sergey_melnychuk.Argumentz.match"))
                .count();

        if (depth > 1) {
            throw new IllegalStateException("Infinite recursive call to Argumentz.match detected.");
        }

        final Map<String, Object> values = new HashMap<>();
        final Set<String> enabled = new HashSet<>();

        for (int i = 0; i < args.length; i++) {
            String name = args[i];
            if (flags.contains(name)) {
                enabled.add(name);
                enabled.add(names.get(name));
                continue;
            }

            if (mappers.containsKey(name) && i < args.length - 1) {
                i ++;
                String input = args[i];
                try {
                    Object value = mappers.get(name).apply(input);
                    values.put(name, value);
                    values.put(names.get(name), value);
                } catch (IllegalArgumentException e) {
                    errorHandler.accept(e, Argumentz.this);
                    throw new IllegalStateException("Error handler did not terminate execution flow of `match`.");
                }
            }
        }

        for (Map.Entry<String, Function<String, ?>> mapper : mappers.entrySet()) {
            if (!values.containsKey(mapper.getKey())) {
                try {
                    Object value = mapper.getValue().apply(null);
                    if (value != null) {
                        values.put(mapper.getKey(), value);
                    } else {
                        String chr = mapper.getKey();
                        String str = names.get(mapper.getKey());
                        if (chr.length() > str.length()) {
                            String tmp = str;
                            str = chr;
                            chr = tmp;
                        }
                        String message = "Missing required parameter: \"" + chr + "\" / \"" + str + "\"";
                        errorHandler.accept(new IllegalArgumentException(message), Argumentz.this);
                        throw new IllegalStateException("Error handler did not terminate execution flow of `match`.");
                    }
                } catch (IllegalArgumentException e) {
                    errorHandler.accept(e, Argumentz.this);
                    throw new IllegalStateException("Error handler did not terminate execution flow of `match`.");
                }
            }
        }

        return new Match() {
            private final Map<String, Object> all = new HashMap<>(values);
            {
                for (String flag : flags) {
                    all.put(flag, true);
                }
            }
            @Override
            public Map<String, Object> all() {
                return new HashMap<>(all);
            }

            @Override
            public String get(String name) {
                return getAs(String.class, name);
            }

            @Override
            public Integer getInt(String name) {
                return getAs(Integer.class, name);
            }

            @Override
            public boolean getFlag(String name) {
                return enabled.contains(prefixed(name));
            }

            @Override
            public <T> T getAs(Class<T> clazz, String name) {
                Object value = values.get(prefixed(name));
                try {
                    return clazz.cast(value);
                } catch (ClassCastException e) {
                    String message = "Failed to cast value '" + value + "' to class '" + clazz.getSimpleName() + "'.";
                    errorHandler.accept(new IllegalArgumentException(message, e), Argumentz.this);
                    throw new IllegalStateException("Error handler did not terminate execution flow of `getAs`.");
                }
            }
        };
    }

    public static Builder builder() {
        return new Builder() {
            private Set<String> flags = new HashSet<>();
            private Map<String, Function<String, ?>> getters = new HashMap<>();
            private Map<String, String> names = new HashMap<>();
            private StringBuilder sb = new StringBuilder();
            private BiConsumer<RuntimeException, Argumentz> errorHandler = (e, a) -> { throw e; };

            private void bindNames(char chr, String name) {
                names.put(prefixed(chr), prefixed(name));
                names.put(prefixed(name), prefixed(chr));
            }

            private void saveParamUsage(char chr, String name, String desc, Function<String, ?> getter) {
                sb.append(String.format("%7.14s %-15.30s %-15.60s", prefixed(chr), prefixed(name), desc));
                try {
                    Object val = getter.apply(null);
                    if (val != null) {
                        sb.append(" (default: ");
                        sb.append(val);
                        sb.append(")");
                    } else {
                        sb.append(" (required)");
                    }
                } catch (Exception e) {
                    sb.append(" (required)");
                }
                sb.append("\n");
            }

            private void saveFlagUsage(char chr, String name, String desc) {
                sb.append(String.format("%7.14s %-15.30s %-15.50s", prefixed(chr), prefixed(name), desc));
            }

            private void bindGetter(char chr, String name, String desc, Function<String, ?> getter) {
                saveParamUsage(chr, name, desc, getter);
                bindNames(chr, name);
                getters.put(prefixed(chr), getter);
                getters.put(prefixed(name), getter);
            }

            private void bindFlag(char chr, String name, String desc) {
                saveFlagUsage(chr, name, desc);
                bindNames(chr, name);
                flags.add(prefixed(chr));
                flags.add(prefixed(name));
            }

            private <T> T applyOrThrow(String value, char chr, String name, Function<String, T> mapper) {
                if (value == null) {
                    String message = "Missing required parameter: \"" + prefixed(chr) + "\" / \"" + prefixed(name) + "\"";
                    throw new IllegalArgumentException(message);
                }
                try {
                    return mapper.apply(value);
                } catch (IllegalArgumentException e) {
                    String message = "Failed to resolve parameter: \"" +
                            prefixed(chr) + "\" / \"" + prefixed(name) + "\": " + e.getMessage();
                    throw new IllegalArgumentException(message, e);
                }
            }

            @Override
            public <T> Builder withParam(char chr, String name, String desc, Function<String, T> mapper, Supplier<T> defaultValue) {
                bindGetter(chr, name, desc,
                        value -> Optional.ofNullable(value)
                            .map(v -> applyOrThrow(v, chr, name, mapper))
                            .orElseGet(defaultValue));
                return this;
            }

            @Override
            public <T> Builder withParam(char chr, String name, String desc, Function<String, T> mapper) {
                bindGetter(chr, name, desc, value -> applyOrThrow(value, chr, name, mapper));
                return this;
            }

            @Override
            public Builder withParam(char chr, String name, String desc, Supplier<String> defaultValue) {
                bindGetter(chr, name, desc, value -> Optional.ofNullable(value).orElseGet(defaultValue));
                return this;
            }

            @Override
            public Builder withParam(char chr, String name, String desc) {
                bindGetter(chr, name, desc, IDENTITY);
                return this;
            }

            @Override
            public Builder withFlag(char chr, String name, String desc) {
                bindFlag(chr, name, desc);
                return this;
            }

            @Override
            public Builder withErrorHandler(BiConsumer<RuntimeException, Argumentz> errorHandler) {
                this.errorHandler = errorHandler;
                return this;
            }

            @Override
            public Argumentz build() {
                return new Argumentz(names, getters, flags, sb.toString(), errorHandler);
            }
        };
    }

    public void printUsage(PrintStream ps) {
        ps.println("Usage: java -cp <...> <MainClass> [ARGUMENTS]\n" + usage);
    }

    private static String prefixed(char chr) {
        return "-" + chr;
    }

    private static String prefixed(String name) {
        return "--" + name;
    }
}
