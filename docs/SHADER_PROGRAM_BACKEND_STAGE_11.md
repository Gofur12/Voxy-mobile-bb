# Stage 11 — Shader and program backend

## Scope

This stage moves the shader-object and program-object lifecycle used by
`core/gl/shader/Shader` behind the backend contract.

Implemented operations:

- Create a shader from its GL shader-type enum.
- Supply source, compile, query compile status, and retrieve the shader log.
- Delete shader objects.
- Create programs, attach/detach shaders, link, query link status, retrieve
  the program log, bind a program, and delete programs.

The desktop adapter implements these operations with the LWJGL OpenGL 2.0
shader/program API. The existing `Shader.Builder` continues to preprocess
sources and inject defines/replacements, but no longer makes direct LWJGL
calls for compilation or linking.

## Resource handling

The builder now deletes compiled shader objects after a successful link.
On compile/link failure it attempts to detach attached shaders and delete
temporary shader/program objects before propagating the error. Shader source
dumps on compilation failure are retained.

## Explicitly not solved

- `ShaderLoader` still emits `#version 460 core`.
- Existing shader sources still use desktop GLSL features, including compute,
  SSBOs, image operations, subgroup operations, and mesh/task stages.
- `ShaderType` still includes NV mesh/task stage enums. A GLES backend must
  reject unsupported stages based on capabilities rather than pretending
  they are portable.
- `AutoBindingShader` still binds UBO/SSBO buffers, textures, and samplers
  directly through LWJGL. Those resource-binding operations are not part of
  this stage.
- Uniform lookup/setters and program resource introspection have not been
  migrated.
- No Android/GLES backend is implemented by this stage.

## Validation status

Source changes were committed to the feature branch. No Gradle compilation,
automated test, desktop runtime test, or Android/Minecraft runtime test was
run. This stage is an API migration, not a claim of Android shader
compatibility.
