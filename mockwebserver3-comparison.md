# mockwebserver3 Support

kiwi-test now includes support for OkHttp's updated mock web server library, `mockwebserver3`
(`com.squareup.okhttp3:mockwebserver3`), in a new package mirroring the existing `mockwebserver` support.

| Original package | New package |
|---|---|
| `org.kiwiproject.test.okhttp3.mockwebserver` | `org.kiwiproject.test.okhttp3.mockwebserver3` |
| `com.squareup.okhttp3:mockwebserver` | `com.squareup.okhttp3:mockwebserver3` |

The five classes are identical in purpose and structure. Most changes are purely mechanical
(package and import names), but `RecordedRequestAssertions` has several meaningful API differences
driven by changes in the mockwebserver3 library itself.

---

## Classes with only package/import changes

**`RecordedRequests`**, **`MockWebServers`**, and **`MockWebServerAssertions`** are functionally
identical to their originals. The only differences are the package declaration and the imports
for `mockwebserver3.MockWebServer` and `mockwebserver3.RecordedRequest`.

---

## MockWebServerExtension — one behavioral difference

`afterEach` changes from:

```java
// original — close() declares throws IOException
KiwiIO.closeQuietly(server);
```

to:

```java
// mockwebserver3 — close() is public void close() (no throws)
server.close();
```

`MockWebServer.close()` in mockwebserver3 does not declare `throws IOException`, so
`KiwiIO.closeQuietly` is no longer needed and `close()` can be called directly.

---

## RecordedRequestAssertions — API differences

These changes reflect renamed or reworked methods in the mockwebserver3 `RecordedRequest` API.

### `hasPath` → `hasTarget`

```java
// original
.hasPath("/users/42")
.hasPath("/users/{}", 42)

// mockwebserver3
.hasTarget("/users/42")
.hasTarget("/users/{}", 42)
```

`getPath()` was renamed to `getTarget()` in mockwebserver3. "Request target" is also the correct
term from [RFC 7230 §3.1.1](https://datatracker.ietf.org/doc/html/rfc7230#section-3.1.1),
covering both the path and any query string (e.g. `/users?active=true`).

### `hasRequestUrl` → `hasUrl`

```java
// original
.hasRequestUrl(uri)
.hasRequestUrl("http://localhost:8080/status")

// mockwebserver3
.hasUrl(uri)
.hasUrl("http://localhost:8080/status")
```

`getRequestUrl()` was renamed to `getUrl()` in mockwebserver3.

### `hasHeader` — internal change only

The method signature is unchanged, but the implementation switches from `getHeader(name)` to
`getHeaders().get(name)`, which is the mockwebserver3 API.

### Body methods — `Buffer` → `ByteString`

`getBody()` in the original returns an `okio.Buffer`; in mockwebserver3 it returns an
`okio.ByteString` (or `null` for requests with no body, such as GET). The public API is
unchanged, but the internals differ:

```java
// original
var actualBodyUtf8 = bodyBuffer.readUtf8();   // okio.Buffer

// mockwebserver3
var actualBodyUtf8 = body.utf8();              // okio.ByteString
```

`hasNoBody()` also changed. Both APIs have `getBodySize()` — the original's `hasBodySize()` already
uses it — but `hasNoBody()` in the original happened to reach through `getBody()` to call
`Buffer.size()`. In mockwebserver3 that pattern is unsafe because `getBody()` returns `null` for
bodyless requests, so `hasNoBody()` switches to `getBodySize()` directly:

```java
// original — getBodySize() exists, but hasNoBody() went through the Buffer
Assertions.assertThat(bodyBuffer.size()).isZero();

// mockwebserver3 — getBody() can be null, so use getBodySize() directly
Assertions.assertThat(recordedRequest.getBodySize()).isZero();
```

### TLS assertions — `hasTlsVersion` removed, `isTls` added

```java
// original — checks getTlsVersion()
.isNotTls()                          // asserts getTlsVersion() is null
.hasTlsVersion(TlsVersion.TLS_1_3)  // asserts specific version

// mockwebserver3 — checks getHandshake()
.isNotTls()   // asserts getHandshake() is null
.isTls()      // asserts getHandshake() is non-null
```

`getTlsVersion()` was removed in mockwebserver3. TLS detection now uses `getHandshake()`, which
returns an `okhttp3.Handshake` object (or `null` for non-TLS). Because the `Handshake` API
provides richer information than a bare `TlsVersion`, `hasTlsVersion(TlsVersion)` was not carried
forward; `isTls()` is provided as the positive counterpart to `isNotTls()`. Tests that need to
inspect the specific TLS version or cipher suite can use `hasRecordedRequest(Consumer<RecordedRequest>)`
or `hasBodySatisfying` to access the `Handshake` directly.

---

## Summary table

| Method / behaviour | `mockwebserver` | `mockwebserver3` |
|---|---|---|
| Path / target | `hasPath(String)` | `hasTarget(String)` |
| Path / target template | `hasPath(String, Object...)` | `hasTarget(String, Object...)` |
| Request URL | `hasRequestUrl(URI/String)` | `hasUrl(URI/String)` |
| Header lookup | `getHeader(name)` (internal) | `getHeaders().get(name)` (internal) |
| Body type | `okio.Buffer.readUtf8()` | `okio.ByteString.utf8()` |
| No-body check | `getBody().size()` (both have `getBodySize()`; see note) | `getBodySize()` directly — `getBody()` can be `null` |
| Not TLS | `isNotTls()` via `getTlsVersion()` | `isNotTls()` via `getHandshake()` |
| Is TLS | `hasTlsVersion(TlsVersion)` | `isTls()` via `getHandshake()` |
| Server close | `KiwiIO.closeQuietly(server)` | `server.close()` |
