I have an Android app that handles tides data with the following current architecture:
- Reads static data files directly when generating tide display output
- Re-reads the same static data files when generating next tide notifications
- This results in duplicate file I/O operations and tightly coupled data access

I'm considering refactoring to:
1. Load static data files once into an in-memory data structure at app startup
2. Generate tide display output from this cached data structure
3. Generate notifications from the same cached data structure

Please evaluate:
1. **Is this refactoring worthwhile?** Consider:
   - Code simplicity and maintainability
   - Performance implications (startup time vs runtime efficiency)
   - Memory usage trade-offs
   - Testability improvements
   - Error handling simplification

2. **If beneficial, design the optimal data structure** that should:
   - Efficiently support both tide display queries and notification scheduling
   - Follow immutable data principles where possible
   - Be memory-efficient for static data
   - Support fast lookups for tide predictions by date/time
   - Handle potential data updates gracefully

3. **Suggest the implementation approach**:
   - Where to initialise this data structure (Application class, Repository pattern, etc.)
   - How to handle loading failures
   - Whether to use dependency injection for accessing the data
   - Any Android-specific considerations (lifecycle, memory management)

