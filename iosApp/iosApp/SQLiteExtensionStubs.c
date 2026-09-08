#include <sqlite3.h>
#include <stddef.h>

// Apple's system libsqlite3 omits extension-loading support on some platforms (notably macOS,
// deliberately, for security), yet SQLDelight's native-driver still references these symbols —
// Kotlin/Native's cinterop generates a wrapper for every function declared in sqlite3.h,
// regardless of whether anything actually calls it, and this app never loads SQLite extensions.
// Without a definition somewhere, that missing symbol fails at link time (or, if the linker is
// told to allow it, crashes dyld the moment the app runs and Kotlin's wrapper resolves it).
//
// These are weak, so on a platform where the real system symbol does exist, it's used instead;
// this fallback only takes over where the real one is missing.
__attribute__((weak))
int sqlite3_enable_load_extension(sqlite3 *db, int onoff) {
    return SQLITE_OK;
}

__attribute__((weak))
int sqlite3_load_extension(sqlite3 *db, const char *zFile, const char *zProc, char **pzErrMsg) {
    if (pzErrMsg != NULL) {
        *pzErrMsg = NULL;
    }
    return SQLITE_ERROR;
}
