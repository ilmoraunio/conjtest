# conjtest

## Introduction

Run Policy-as-Code using Clojure!

Conjtest is a command-line utility heavily inspired by and partially based on
[conftest](https://www.conftest.dev/). It allows you to write policies against
structured configuration data using a robust and practical language. You can,
for example, write policies against your EDN files, Kubernetes configurations,
Terraform code, or against other common configuration formats.

## Project status

[![Slack](https://img.shields.io/badge/slack-conjtest-orange.svg?logo=slack)](https://clojurians.slack.com/app_redirect?channel=conjtest)

Project is **active** and in
[alpha](https://kotlinlang.org/docs/components-stability.html#stability-levels-explained).

Check [CHANGELOG.md](CHANGELOG.md) for any breaking changes.

## Goals

- Write and evaluate policies using [Clojure](https://clojure.org/),
  [babashka](https://github.com/babashka/babashka), and
  [SCI](https://github.com/babashka/sci).
- Provide self-contained binary for ease of use.
- Support many common configuration file formats. Supported file formats:
  - CUE
  - Dockerfile
  - Dotenv
  - EDN
  - HCL1
  - HCL2
  - HOCON
  - Ignore
  - INI
  - JSON
  - Jsonnet
  - Properties
  - Spdx
  - TOML
  - VCL
  - XML
  - YAML
- Support customizability via
  [pod-ilmoraunio-conjtest](https://github.com/ilmoraunio/pod-ilmoraunio-conjtest)
  and separate conjtest (pure Clojure) library.
- Linux and macOS

## Quickstart

Download the binary from the latest
[release](https://github.com/ilmoraunio/conjtest/releases).

How to get started:

```
cat <<EOF > my-ingress.yaml
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  annotations:
    nginx.ingress.kubernetes.io/cors-allow-origin: '*'
  name: service-a
  namespace: foobar
EOF

cat <<EOF > my-rules.clj
(ns my-rules)

(defn deny-*-cors
  [input]
  (when (= "*" (get-in input
                       [:metadata
                        :annotations
                        :nginx.ingress.kubernetes.io/cors-allow-origin]))
    "CORS is too permissive"))
EOF
```

```
conjtest test my-ingress.yaml -p my-rules.clj
```

Output:

```
$ conjtest test my-ingress.yaml -p my-rules.clj
FAIL - my-ingress.yaml - deny-*-cors - CORS is too permissive

1 tests, 0 passed, 0 warnings, 1 failures
```

## Documentation

- [User Guide](https://conjtest.github.io)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [DEVELOPMENT.md](DEVELOPMENT.md)
- [CHANGELOG.md](CHANGELOG.md)
