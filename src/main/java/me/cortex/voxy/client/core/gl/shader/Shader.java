package me.cortex.voxy.client.core.gl.shader;

import me.cortex.voxy.client.core.gl.Capabilities;
import me.cortex.voxy.client.core.gl.backend.GlBackend;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;
import me.cortex.voxy.client.core.gl.GlDebug;
import me.cortex.voxy.common.Logger;
import me.cortex.voxy.common.util.ThreadUtils;
import me.cortex.voxy.common.util.TrackedObject;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class Shader extends TrackedObject {
    private final int id;
    private final long contextGeneration;
    Shader(int program) {
        id = program;
        this.contextGeneration = GlBackendRuntime.getContextGeneration();
    }

    public int id() {
        return this.id;
    }

    protected final void assertContextCurrent() {
        this.assertNotFreed();
        GlBackendRuntime.requireBackend(this.contextGeneration, "Shader");
    }

    public void bind() {
        this.assertContextCurrent();
        GlBackendRuntime.requireBackend(this.contextGeneration, "Shader").useProgram(this.id);
    }

    public void free() {
        super.free0();
        if (GlBackendRuntime.isContextGenerationCurrent(this.contextGeneration)) {
            GlBackendRuntime.requireBackend(this.contextGeneration, "Shader").deleteProgram(this.id);
        }
    }


    public Shader name(String name) {
        this.assertContextCurrent();
        return GlDebug.name(name, this);
    }


    public static Builder<Shader> make(IShaderProcessor... processor) {
        return makeInternal((a,b)->new Shader(b), processor);
    }

    public static Builder<AutoBindingShader> makeAuto(IShaderProcessor... processor) {
        return makeInternal(AutoBindingShader::new, processor);
    }



    static <T extends Shader> Builder<T> makeInternal(Builder.IShaderObjectConstructor<T> constructor, IShaderProcessor[] processors) {
        List<IShaderProcessor> aa = new ArrayList<>(List.of(processors));
        Collections.reverse(aa);
        IShaderProcessor applicator = (type,source)->source;
        for (IShaderProcessor processor : processors) {
            IShaderProcessor finalApplicator = applicator;
            applicator = (type, source) -> finalApplicator.process(type, processor.process(type, source));
        }
        return new Builder<>(constructor, applicator);
    }

    public static class Builder <T extends Shader> {
        protected interface IShaderObjectConstructor <J extends Shader> {
            J make(Builder<J> builder, int program);
        }
        final Map<String, String> defines = new HashMap<>();
        final Map<String, String> replacements = new LinkedHashMap<>();
        private final Map<ShaderType, String> sources = new HashMap<>();
        private final IShaderProcessor processor;
        private final IShaderObjectConstructor<T> constructor;
        private Builder(IShaderObjectConstructor<T> constructor, IShaderProcessor processor) {
            this.constructor = constructor;
            this.processor = processor;
        }

        public Builder<T> clone() {
            var clone = new Builder<>(this.constructor, this.processor);
            clone.defines.putAll(this.defines);
            clone.sources.putAll(this.sources);
            clone.replacements.putAll(this.replacements);
            return clone;
        }

        public Builder<T> define(String name) {
            this.defines.put(name, "");
            return this;
        }

        //Useful for inline setting (such as debug)
        public Builder<T> defineIf(String name, boolean condition) {
            if (condition) {
                this.defines.put(name, "");
            }
            return this;
        }

        public Builder<T> defineIf(String name, boolean condition, int value) {
            if (condition) {
                this.defines.put(name, Integer.toString(value));
            }
            return this;
        }

        public Builder<T> define(String name, int value) {
            this.defines.put(name, Integer.toString(value));
            return this;
        }

        public Builder<T> define(String name, float value) {
            this.defines.put(name, Float.toString(value)+"f");
            return this;
        }

        public Builder<T> define(String name, String value) {
            this.defines.put(name, value);
            return this;
        }

        public Builder<T> replace(String value, String replacement) {
            this.replacements.put(value, replacement);
            return this;
        }

        public Builder<T> add(ShaderType type, String id) {
            this.addSource(type, ShaderLoader.parse(id));
            return this;
        }

        public Builder<T> addSource(ShaderType type, String source) {
            this.sources.put(type, this.processor.process(type, source));
            return this;
        }

        public Builder<T> apply(Consumer<Builder<T>> applyer) {
            applyer.accept(this);
            return this;
        }


        private int compileToProgram() {
            GlBackend backend = GlBackendRuntime.requireBackend();
            int program = backend.createProgram();
            List<Integer> shaders = new ArrayList<>(this.sources.size());
            List<Integer> attached = new ArrayList<>(this.sources.size());
            try {
                String defs = this.defines.entrySet().stream()
                        .map(a -> "#define " + a.getKey() + " " + a.getValue() + "\n")
                        .collect(Collectors.joining());
                for (var entry : this.sources.entrySet()) {
                    String src = entry.getValue();
                    validateShaderVersion(backend.capabilities(), entry.getKey(), src);
                    int firstLineEnd = src.indexOf('\n');
                    if (firstLineEnd < 0) throw new IllegalArgumentException("Shader source must start with a version line");
                    src = src.substring(0, firstLineEnd + 1) + defs + src.substring(firstLineEnd + 1);
                    for (var replacement : this.replacements.entrySet()) {
                        src = src.replace(replacement.getKey(), replacement.getValue());
                    }
                    shaders.add(createShader(backend, entry.getKey(), src));
                }
                for (int shader : shaders) {
                    backend.attachShader(program, shader);
                    attached.add(shader);
                }
                backend.linkProgram(program);
                printProgramLinkLog(backend, program);
                if (!backend.isProgramLinked(program)) {
                    throw new RuntimeException("Shader program linking failed, see log for details");
                }
                for (int shader : attached) backend.detachShader(program, shader);
                attached.clear();
                for (int shader : shaders) backend.deleteShader(shader);
                shaders.clear();
                return program;
            } catch (RuntimeException | Error failure) {
                for (int shader : attached) {
                    try { backend.detachShader(program, shader); } catch (RuntimeException ignored) {}
                }
                for (int shader : shaders) {
                    try { backend.deleteShader(shader); } catch (RuntimeException ignored) {}
                }
                try { backend.deleteProgram(program); } catch (RuntimeException ignored) {}
                throw failure;
            }
        }

        public T compile() {
            this.defineIf("IS_INTEL", Capabilities.INSTANCE.isIntel);
            this.defineIf("IS_WINDOWS", ThreadUtils.isWindows);
            return this.constructor.make(this, this.compileToProgram());
        }

        private static void validateShaderVersion(GlCapabilities capabilities, ShaderType type, String source) {
            int lineEnd = source.indexOf('\n');
            String versionLine = lineEnd < 0 ? source : source.substring(0, lineEnd);
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("#version\\s+(\\d+)(?:\\s+(core|compatibility|es))?")
                    .matcher(versionLine.trim());
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid or missing GLSL version directive for " + type
                        + ": " + versionLine);
            }

            int requestedVersion = Integer.parseInt(matcher.group(1));
            int requestedMajor = requestedVersion / 100;
            int requestedMinor = (requestedVersion % 100) / 10;
            String profile = matcher.group(2);
            boolean requestsEs = "es".equals(profile);

            if (capabilities.embeddedProfile != requestsEs) {
                throw new IllegalStateException("Shader profile mismatch for " + type + ": source requests "
                        + (requestsEs ? "GLSL ES " : "desktop GLSL ") + requestedVersion
                        + " but backend '" + GlBackendRuntime.requireBackend().name() + "' exposes "
                        + (capabilities.embeddedProfile ? "an embedded" : "a desktop") + " GL profile");
            }
            if (requestedMajor > capabilities.majorVersion
                    || (requestedMajor == capabilities.majorVersion && requestedMinor > capabilities.minorVersion)) {
                throw new IllegalStateException("Shader " + type + " requires GLSL " + requestedVersion
                        + " but backend '" + GlBackendRuntime.requireBackend().name() + "' exposes GL "
                        + capabilities.majorVersion + "." + capabilities.minorVersion);
            }
        }

        private static void printProgramLinkLog(GlBackend backend, int program) {
            String log = backend.getProgramInfoLog(program);
            if (!log.isEmpty()) Logger.error(log);
        }

        private static int createShader(GlBackend backend, ShaderType type, String src) {
            int shader = backend.createShader(type.gl);
            try {
                backend.setShaderSource(shader, src);
                backend.compileShader(shader);
                String log = backend.getShaderInfoLog(shader);
                if (!log.isEmpty()) Logger.warn(log);
                if (!backend.isShaderCompiled(shader)) {
                    try {
                        Files.writeString(Path.of("SHADER_DUMP.txt"), src);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    throw new RuntimeException("Shader compilation failed of type " + type.name()
                            + ", see log for details, dumped shader");
                }
                return shader;
            } catch (RuntimeException | Error failure) {
                try { backend.deleteShader(shader); } catch (RuntimeException ignored) {}
                throw failure;
            }
        }

    }
}
