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

## Installation

Download the binaries for the MacOS (arm64) and Linux (amd64) platforms from
the repository's [latest
release](https://github.com/ilmoraunio/conjtest/releases) and install the
binary to `usr/local/bin`.

MacOS (arm64):

```bash
curl -sLO https://github.com/ilmoraunio/conjtest/releases/download/v0.0.1/conjtest-0.0.1-macos-arm64.zip -o conjtest.zip
unzip conjtest.zip conjtest
sudo mv conjtest /usr/local/bin
```

Linux (amd64):

```
curl -sLO https://github.com/ilmoraunio/conjtest/releases/download/v0.0.1/conjtest-0.0.1-linux-x86_64.tar.gz -o conjtest.tar.gz
tar -xvzf conjtest.zip conjtest
sudo mv conjtest /usr/local/bin
```

## Quickstart

```clojure
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
