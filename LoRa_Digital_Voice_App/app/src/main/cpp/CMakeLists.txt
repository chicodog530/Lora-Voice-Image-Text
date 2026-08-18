cmake_minimum_required(VERSION 3.22.1)

project("loravoice")

# Disable codec2 tests and executables
set(UNITTEST OFF CACHE BOOL "Disable unit tests" FORCE)
set(INSTALL_EXAMPLES OFF CACHE BOOL "Disable examples" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build static codec2" FORCE)

add_subdirectory(codec2)

add_library(
        loravoice
        SHARED
        codec2_jni.cpp)

target_include_directories(loravoice PRIVATE codec2/src)

target_link_libraries(
        loravoice
        codec2
        log)
