# conjtest

![Introducing Conjtest](https://ilmo.me/img/conjtest-hello-world.gif)

## Introduction

Run Policy-as-Code using Clojure!

Conjtest is a command-line utility heavily inspired by and partially based on
[Conftest](https://www.conftest.dev/). It allows you to write policies against
structured configuration data using a robust and practical language. You can,
for example, write policies against your EDN files, Kubernetes configurations,
Terraform code, or against other common configuration formats.

Conjtest enables you to:

- Define & run policies against your infrastructure as part of your CI pipeline
  or Git Hooks using a standardized tool.
- Catch problems or security issues before they become incidents & enforce
  compliance.
- Use Clojure to define your infrastructure policies, which may either support
  a full-stack Clojure approach or enable gradual introduction of Clojure to a
  codebase.
- Provide Go/Conftest parsers for compatibility.

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
  and separate [conjtest-clj](https://github.com/ilmoraunio/conjtest-clj) (pure
  Clojure) library.
- Linux and macOS

## Installation

You can install `conjtest` either using an installer script or by pulling the
binary from [GitHub releases](https://github.com/ilmoraunio/conjtest/releases).

### Installer script

Download & run the installer script using `bash`.

```bash
bash < <(curl -s https://raw.githubusercontent.com/ilmoraunio/conjtest/master/install)
```

By default the script will install the binary to `/usr/local/bin` (you may need
to use `sudo`).

You can install the binary to another location using `--install-dir`.

```bash
curl -sO https://raw.githubusercontent.com/ilmoraunio/conjtest/master/install
chmod u+x install
./install --install-dir .
```

To install a specific version, you may provide `--version`.

```bash
./install --version 0.1.0
```

The full list of versions can be found from
[here](https://github.com/ilmoraunio/conjtest/tags).

### GitHub releases

Download the binaries for the MacOS (arm64) and Linux (amd64) platforms from
the repository's [latest
release](https://github.com/ilmoraunio/conjtest/releases) and install the
binary to `usr/local/bin`.

MacOS (arm64):

```bash
curl -sL https://github.com/ilmoraunio/conjtest/releases/download/v0.1.0/conjtest-0.1.0-macos-arm64.zip -o conjtest.zip
unzip conjtest.zip conjtest
sudo mv conjtest /usr/local/bin
```

Linux (amd64):

```
curl -sL https://github.com/ilmoraunio/conjtest/releases/download/v0.1.0/conjtest-0.1.0-linux-x86_64.tar.gz -o conjtest.tar.gz
tar -xvzf conjtest.tar.gz conjtest
sudo mv conjtest /usr/local/bin
```

Linux (arm64):

```
curl -sL https://github.com/ilmoraunio/conjtest/releases/download/v0.1.0/conjtest-0.1.0-linux-arm64.tar.gz -o conjtest.tar.gz
tar -xvzf conjtest.tar.gz conjtest
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

cat <<EOF > policy.clj
(ns policy)

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
conjtest test my-ingress.yaml -p policy.clj
```

Output:

```
$ conjtest test my-ingress.yaml -p policy.clj
FAIL - my-ingress.yaml - deny-*-cors - CORS is too permissive

1 tests, 0 passed, 0 warnings, 1 failures
```

## Documentation

- [User Guide](https://user-guide.conjtest.org)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [DEVELOPMENT.md](DEVELOPMENT.md)
- [CHANGELOG.md](CHANGELOG.md)
