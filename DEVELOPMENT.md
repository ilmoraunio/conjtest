# Development

## Build

### macOS (AArch64)

```
make build-macos-aarch64
```

## Run

### Conjtest (library)

```
clj -A:test
```

### Conjtest (cli/babashka)

```
CONJTEST_DEV=1 rlwrap bb
```

## Test

Run `./scripts/kaocha && ./scripts/test`
