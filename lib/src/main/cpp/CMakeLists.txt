cmake_minimum_required(VERSION 3.6.0)

project("argon2id")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -Os")

add_library(argon2id SHARED
        argon2/blake2/blake2b.c
        argon2/argon2.c
        argon2/core.c
        argon2/encoding.c
        argon2/ref.c
        argon2/thread.c
        jni/Argon2Jni.cpp)

target_compile_options(argon2id PRIVATE)
target_include_directories(argon2id PUBLIC argon2)
target_link_libraries(argon2id)