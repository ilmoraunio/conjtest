BUILD_FILES=target cljconf

pod-conftest-clj/pod-conftest-clj:
	@make -C pod-conftest-clj build-macos-aarch64

.PHONY: default
default: pod-conftest-clj/pod-conftest-clj

build-macos-aarch64: pod-conftest-clj/pod-conftest-clj
	./scripts/build_macos.sh

.PHONY: clean
clean:
	rm -rf $(BUILD_FILES)
