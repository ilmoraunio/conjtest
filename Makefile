BUILD_FILES=target conjtest

pod-ilmoraunio-conjtest/pod-ilmoraunio-conjtest:
	@make -C pod-ilmoraunio-conjtest build-macos-aarch64

pod-ilmoraunio-conftest/pod-ilmoraunio-conftest:
	@make -C pod-ilmoraunio-conftest build

.PHONY: default
default: pod-ilmoraunio-conjtest/pod-ilmoraunio-conjtest pod-ilmoraunio-conftest/pod-ilmoraunio-conftest

build-macos-aarch64: pod-ilmoraunio-conjtest/pod-ilmoraunio-conjtest
	./scripts/build_macos.sh

.PHONY: clean
clean:
	rm -rf $(BUILD_FILES)
