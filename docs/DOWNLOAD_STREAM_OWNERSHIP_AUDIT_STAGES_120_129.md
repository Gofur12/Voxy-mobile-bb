# Download-stream ownership audit — combined stages 120–129

This single draft batch groups ten focused ownership checks into one reviewable change set.

1. Raw download allocation registration rolls back the arena slot if fragment-record creation or list insertion fails.
2. Raw download shutdown retires allocations still held by the unsubmitted fragment list after successful backend finish.
3. CPU readback queue insertion frees native staging memory if queue insertion fails.
4. Persistent download allocation registration rolls back a newly allocated arena segment if tracking insertion fails.
5. Commit callback transfer rolls back any partially appended callback records if transfer fails.
6. Download frame snapshots are created before the fence; source lists remain authoritative until enqueue succeeds.
7. Download frame enqueue deletes the newly created fence if queue insertion fails.
8. Download close records successful backend finish independently from flush/callback failures.
9. Download close frees frame fences and the mapped buffer only when GPU completion is established.
10. If completion cannot be established, shutdown drops Java-side frame references without issuing GL deletion against potentially in-flight resources.

The batch preserves callback exception aggregation and the existing render-context ownership model. It does not claim that the GPU backend is thread-safe.

Static validation: ownership rollback, source-list retention, and quiescence-gated GL cleanup are checked in the generated source. Not performed: Android device execution, GL-driver validation, Gradle build, or runtime shader/render testing.
