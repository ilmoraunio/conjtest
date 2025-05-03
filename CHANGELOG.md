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

## 0.0.3-SNAPSHOT

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
