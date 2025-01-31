BUILD_FILES=target cljconf

pod-ilmoraunio-cljconf/pod-ilmoraunio-cljconf:
	@make -C pod-ilmoraunio-cljconf build-macos-aarch64

pod-ilmoraunio-conftest/pod-ilmoraunio-conftest:
	@make -C pod-ilmoraunio-conftest build

.PHONY: default
default: pod-ilmoraunio-cljconf/pod-ilmoraunio-cljconf pod-ilmoraunio-conftest/pod-ilmoraunio-conftest

build-macos-aarch64: pod-ilmoraunio-cljconf/pod-ilmoraunio-cljconf
	./scripts/build_macos.sh

.PHONY: clean
clean:
	rm -rf $(BUILD_FILES)
