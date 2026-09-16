cmake_minimum_required(VERSION 3.22.1)
project(wamr_bridge LANGUAGES C CXX)

set(WAMR_BUILD_PLATFORM "android")

if (ANDROID_ABI STREQUAL "arm64-v8a")
    set(WAMR_BUILD_TARGET "AARCH64")
elseif (ANDROID_ABI STREQUAL "armeabi-v7a")
    set(WAMR_BUILD_TARGET "ARM")
elseif (ANDROID_ABI STREQUAL "x86_64")
    set(WAMR_BUILD_TARGET "X86_64")
endif()

set(WAMR_BUILD_AOT 1)
set(WAMR_BUILD_INTERP 1)
set(WAMR_BUILD_FAST_INTERP 1)
set(WAMR_BUILD_LIBC_BUILTIN 1)
set(WAMR_BUILD_LIBC_WASI 1)
set(WAMR_BUILD_REF_TYPES 1)
set(WAMR_DISABLE_HW_BOUND_CHECK 1)
set(WAMR_DISABLE_STACK_HW_BOUND_CHECK 1)

set(WAMR_ROOT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/wamr)
include(${WAMR_ROOT_DIR}/build-scripts/runtime_lib.cmake)

add_library(wamr SHARED
    ${WAMR_RUNTIME_LIB_SOURCE}
    wamr_bridge.c
)

target_link_libraries(wamr
    android
    log
    m
    dl
)

target_include_directories(wamr PRIVATE
    ${WAMR_ROOT_DIR}/core/iwasm/include
    ${WAMR_ROOT_DIR}/core/shared/platform/android
)
