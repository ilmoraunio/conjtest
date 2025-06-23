# Changelog

This project uses [Break Versioning][breakver]. The version numbers follow a
`<major>.<minor>.<patch>` scheme with the following intent:

| Bump    | Intent                                                     |
| ------- | ---------------------------------------------------------- |
| `major` | Major breaking changes -- check the changelog for details. |
| `minor` | Minor breaking changes -- check the changelog for details. |
| `patch` | No breaking changes, ever!!                                |

`-SNAPSHOT` versions are preview versions for upcoming releases.

[breakver]: https://github.com/ptaoussanis/encore/blob/master/BREAK-VERSIONING.md

## 0.3.0

- Support declarative policies via malli schemas [#10](https://github.com/ilmoraunio/conjtest/pull/10)
- upgrade babashka to 1.12.203 + minor sci fix [2beb82a](https://github.com/ilmoraunio/conjtest/commit/2beb82a0f1170d70431f2f03883ad6799f8cff71)
- Unknown option prints out exception & crashes [#3](https://github.com/ilmoraunio/conjtest/issues/3)

### Highlights

It's now possible to define **declarative** policies using
[malli](https://github.com/metosin/malli) schemas. Malli schemas are evaluated
using
[`malli.core/validate`](https://github.com/metosin/malli?tab=readme-ov-file#validation),
after which they are processed for any errors using
[`malli.core/explain`](https://github.com/metosin/malli?tab=readme-ov-file#error-messages)
and
[`malli.error/humanize`](https://github.com/metosin/malli?tab=readme-ov-file#humanized-error-messages).

Example:

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

(def allow-non-*-cors
  [:map
   [:metadata
    [:map
     [:annotations
      [:map
       [:nginx.ingress.kubernetes.io/cors-allow-origin [:not= {:error/message "CORS is too permissive"} "*"]]]]]]])
EOF
```

```
conjtest init
conjtest test my-ingress.yaml -p policy.clj
```

Output:

```
$ conjtest test my-ingress.yaml -p policy.clj
FAIL - my-ingress.yaml - allow-non-*-cors - {:metadata {:annotations {:nginx.ingress.kubernetes.io/cors-allow-origin ["CORS is too permissive"]}}}

1 tests, 0 passed, 0 warnings, 1 failures
```

## 0.2.0

- Add default `--config` value [#8](https://github.com/ilmoraunio/conjtest/pull/8)
- Add `conjtest init` command [#9](https://github.com/ilmoraunio/conjtest/pull/9)

### Highlights

This version offers a way to handle all parse outputs as fully keywordized by
default using `conjtest init` (which will initialize a new config file
`conjtest.edn` to current directory) & default `--config` value `conjtest.edn`
(which allows conjtest to implicitly transform keys as keywords).

```
$ conjtest init # creates conjtest.edn with default config, eg. `{:keywordize? true}`
Creating conjtest.edn...
Done!
$ conjtest parse examples/hcl2/terraform.tf # using keyworded keys implicitly
{:resource
 {:aws_db_security_group {:my-group [{}]},
  :aws_s3_bucket
  {:valid
   [{:acl "private",
     :bucket "validBucket",
     :tags {:environment "prod", :owner "devops"}}]},
  :aws_security_group_rule
  {:my-rule [{:cidr_blocks ["0.0.0.0/0"], :type "ingress"}]},
  :azurerm_managed_disk
  {:source [{:encryption_settings [{:enabled false}]}]},
  :aws_alb_listener
  {:my-alb-listener [{:port "80", :protocol "HTTP"}]}}}
$ conjtest test examples/hcl2/terraform.tf -p examples/hcl2/policy.clj # using keyworded keys implicitly
FAIL - examples/hcl2/terraform.tf - deny-fully-open-ingress - ASG rule ':my-rule' defines a fully open ingress
FAIL - examples/hcl2/terraform.tf - deny-http - ALB listener ':my-alb-listener' is using HTTP rather than HTTPS
FAIL - examples/hcl2/terraform.tf - deny-missing-tags - AWS resource: :aws_alb_listener named ':my-alb-listener' is missing required tags: #{:environment :owner}
FAIL - examples/hcl2/terraform.tf - deny-missing-tags - AWS resource: :aws_db_security_group named ':my-group' is missing required tags: #{:environment :owner}
FAIL - examples/hcl2/terraform.tf - deny-missing-tags - AWS resource: :aws_security_group_rule named ':my-rule' is missing required tags: #{:environment :owner}
FAIL - examples/hcl2/terraform.tf - deny-unencrypted-azure-disk - Azure disk ':source' is not encrypted

4 tests, 0 passed, 0 warnings, 4 failures
```

## 0.1.0

- Support keyworded keys via `keywordize?` [#4](https://github.com/ilmoraunio/conjtest/pull/4)
  - Enables keyworded keys for parsed data results. This allows for more
    idiomatic access to Clojure data structures inside policies.

```clojure
$ cat conjtest.edn
{:keywordize? true}

$ conjtest parse examples/hcl2/terraform.tf --config conjtest.edn
{:resource
 {:aws_alb_listener
  {:my-alb-listener [{:port "80", :protocol "HTTP"}]},
  :aws_db_security_group {:my-group [{}]},
  :aws_s3_bucket
  {:valid
   [{:acl "private",
     :bucket "validBucket",
     :tags {:environment "prod", :owner "devops"}}]},
  :aws_security_group_rule
  {:my-rule [{:cidr_blocks ["0.0.0.0/0"], :type "ingress"}]},
  :azurerm_managed_disk
  {:source [{:encryption_settings [{:enabled false}]}]}}}

$ conjtest test examples/hcl2/terraform.tf -p examples/hcl2/policy.clj --config conjtest.edn
FAIL - examples/hcl2/terraform.tf - deny-fully-open-ingress - ASG rule ':my-rule' defines a fully open ingress
FAIL - examples/hcl2/terraform.tf - deny-http - ALB listener ':my-alb-listener' is using HTTP rather than HTTPS
FAIL - examples/hcl2/terraform.tf - deny-missing-tags - AWS resource: :aws_alb_listener named ':my-alb-listener' is missing required tags: #{:environment :owner}
FAIL - examples/hcl2/terraform.tf - deny-missing-tags - AWS resource: :aws_db_security_group named ':my-group' is missing required tags: #{:environment :owner}
FAIL - examples/hcl2/terraform.tf - deny-missing-tags - AWS resource: :aws_security_group_rule named ':my-rule' is missing required tags: #{:environment :owner}
FAIL - examples/hcl2/terraform.tf - deny-unencrypted-azure-disk - Azure disk ':source' is not encrypted

4 tests, 0 passed, 0 warnings, 4 failures

# See policy definitions at: https://github.com/ilmoraunio/conjtest/blob/main/examples/hcl2/policy.clj
```

- BREAKING: Upgrade ilmoraunio/conftest to 0.1.0 [#5](https://github.com/ilmoraunio/conjtest/pull/5)
  - The output of HCL2 changes slightly. See [open-policy-agent/conftest#1074](https://github.com/open-policy-agent/conftest/pull/1074) and [open-policy-agent/conftest#1006](https://github.com/open-policy-agent/conftest/issues/1006) for more info.
- Bump babashka dependency to 1.12.200 [d55dbc7](https://github.com/ilmoraunio/conjtest/commit/d55dbc7d60dadc3a2cf1a5d8c58ae649eea58c24)
- ci: bump DeLaGuardo/setup-clojure to 13.2 [0ae518a](https://github.com/ilmoraunio/conjtest/commit/0ae518a7948a0aca76b83a88157f2754aa34cf8c)

## 0.0.2

- Add Linux arm64 binary [#2](https://github.com/ilmoraunio/conjtest/pull/2) [eaef9f9](https://github.com/ilmoraunio/conjtest/commit/eaef9f98888b179a16ec40dc3ac76ff55ea335ae)
- Add install script [7c901cd](https://github.com/ilmoraunio/conjtest/commit/7c901cd89f58acc265dc1b55c3667e77efe83cdc) [0e191b1](https://github.com/ilmoraunio/conjtest/commit/0e191b1eafa6800de5c4874786df169bdc0fe06e) [d0393a7](https://github.com/ilmoraunio/conjtest/commit/d0393a741e4ec24a4e9d4dd4f4960f787ec8503e) [eaef9f9](https://github.com/ilmoraunio/conjtest/commit/eaef9f98888b179a16ec40dc3ac76ff55ea335ae)
- Add `conjtest version` [aacdc3e](https://github.com/ilmoraunio/conjtest/commit/aacdc3e45d2cd54a658156accb885ca0ebcd90c1) [d55fd80](https://github.com/ilmoraunio/conjtest/commit/d55fd80a7fba0cda11a72c666b1bc8a235dfa28d)

---

### Highlights

#### Linux arm64 build

This release adds binary for the Linux arm64 architecture. To install by hand:

```bash
curl -sL https://github.com/ilmoraunio/conjtest/releases/download/v0.0.2/conjtest-0.0.2-linux-arm64.tar.gz -o conjtest.tar.gz
tar -xvzf conjtest.tar.gz conjtest
sudo mv conjtest /usr/local/bin
```

#### New installer script

New installer script makes it easier to install `conjtest`:

```
curl -sO https://raw.githubusercontent.com/ilmoraunio/conjtest/master/install
chmod u+x install
./install --install-dir .
```

Currently supported OSes and architectures are:
- Linux arm64/aarch64 & amd64
- MacOS arm64/aarch64

#### Version information available

You can now check which version of conjtest you are running via `conjtest
version`.

## 0.0.1

This is the first release! 🎉
