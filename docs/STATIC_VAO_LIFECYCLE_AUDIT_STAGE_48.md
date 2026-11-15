# Static VAO Lifecycle Audit — Stage 48

## Audited resource

File: `src/main/java/me/cortex/voxy/client/core/gl/GlVertexArray.java`

The class still declares:

```java
public static final int STATIC_VAO = glGenVertexArrays();
```

This allocates a GL object during class initialization, before the backend runtime necessarily owns a current context. The handle is not associated with a context generation and has no matching release/recreation owner.

## Why it was not mechanically replaced in this stage

`STATIC_VAO` is a public static integer API. Replacing it with a lazy accessor changes the call-site contract; keeping the integer field preserves the eager allocation bug. A safe migration therefore requires identifying and updating every consumer, then choosing an owner whose lifetime is tied to the actual Minecraft render context.

The branch-scoped source review did not establish a complete, reliable list of all consumers. GitHub's available code-search endpoint only searches the repository default branch, not this working branch, so its empty result is not evidence that the field has no consumers. No speculative API-breaking edit was made.

## Required follow-up

1. Obtain a branch-complete search of every Java source for `STATIC_VAO` and `GlVertexArray.STATIC_VAO`.
2. Inspect each caller's thread, initialization order, bind/unbind expectations, and shutdown owner.
3. Replace the integer field with a context-owned VAO provider or migrate each caller to its own `GlVertexArray` instance.
4. Ensure shutdown/context-loss behavior and avoid deleting a handle through a replacement context.

## Stage outcome

This is an audit/decision stage, not a completed static-VAO migration. The eager static allocation remains outstanding and must not be treated as fixed.

No Gradle build, automated tests, Minecraft launch, or Android device test was run.
