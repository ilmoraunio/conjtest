BUILD_FILES=target cljconf

pod-ilmoraunio-conftest-clj/pod-ilmoraunio-conftest-clj:
	@make -C pod-ilmoraunio-conftest-clj build-macos-aarch64

pod-ilmoraunio-conftest/pod-ilmoraunio-conftest:
	@make -C pod-ilmoraunio-conftest build

.PHONY: default
default: pod-ilmoraunio-conftest-clj/pod-ilmoraunio-conftest-clj pod-ilmoraunio-conftest/pod-ilmoraunio-conftest

build-macos-aarch64: pod-ilmoraunio-conftest-clj/pod-ilmoraunio-conftest-clj
	./scripts/build_macos.sh

.PHONY: clean
clean:
	rm -rf $(BUILD_FILES)
