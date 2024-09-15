pod-conftest-clj/pod-conftest-clj:
	@make -C pod-conftest-clj build-macos-aarch64

.PHONY: default
default: pod-conftest-clj/pod-conftest-clj
