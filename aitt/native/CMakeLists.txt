# AITT Native Library - CMake Configuration
#
# Build Instructions:
#   cmake -B build -G "Visual Studio 17 2022" -A x64
#   cmake --build build --config Release
#
# Or with Ninja:
#   cmake -B build -G Ninja -DCMAKE_BUILD_TYPE=Release
#   cmake --build build

cmake_minimum_required(VERSION 3.16)
project(aitt_native VERSION 1.0.0 LANGUAGES C)

# ============================================================================
# OPTIONS
# ============================================================================

option(AITT_BUILD_SHARED "Build shared library (.dll)" ON)
option(AITT_BUILD_TESTS "Build test executables" OFF)

# ============================================================================
# FIND JAVA AND JNI
# ============================================================================

# Find Java
find_package(Java REQUIRED)
find_package(JNI REQUIRED)

if(NOT JNI_FOUND)
    message(FATAL_ERROR "JNI not found. Set JAVA_HOME environment variable.")
endif()

message(STATUS "JNI Include: ${JNI_INCLUDE_DIRS}")
message(STATUS "Java Home: $ENV{JAVA_HOME}")

# ============================================================================
# COMPILER SETTINGS
# ============================================================================

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

if(MSVC)
    add_compile_options(/W4 /WX- /utf-8)
    add_compile_definitions(_CRT_SECURE_NO_WARNINGS)
else()
    add_compile_options(-Wall -Wextra -Wpedantic)
endif()

# ============================================================================
# SOURCE FILES
# ============================================================================

set(NATIVE_SOURCES
    src/aitt_native.c
    src/jni_bindings.c
    src/chipset_s32k148.c
    src/chipset_mpc5777m.c
)

set(NATIVE_HEADERS
    include/aitt_native.h
)

# ============================================================================
# BUILD LIBRARY
# ============================================================================

if(AITT_BUILD_SHARED)
    add_library(aitt_native SHARED ${NATIVE_SOURCES} ${NATIVE_HEADERS})
else()
    add_library(aitt_native STATIC ${NATIVE_SOURCES} ${NATIVE_HEADERS})
endif()

target_include_directories(aitt_native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${JNI_INCLUDE_DIRS}
)

# Windows-specific libraries
if(WIN32)
    target_link_libraries(aitt_native PRIVATE
        setupapi
    )
endif()

# Export symbols on Windows
if(WIN32 AND AITT_BUILD_SHARED)
    target_compile_definitions(aitt_native PRIVATE AITT_EXPORTS)
endif()

# ============================================================================
# OUTPUT SETTINGS
# ============================================================================

set_target_properties(aitt_native PROPERTIES
    OUTPUT_NAME "aitt_native"
    PREFIX ""
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
    ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
)

# For multi-config generators (Visual Studio)
foreach(CONFIG_TYPE ${CMAKE_CONFIGURATION_TYPES})
    string(TOUPPER ${CONFIG_TYPE} CONFIG_TYPE_UPPER)
    set_target_properties(aitt_native PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY_${CONFIG_TYPE_UPPER} "${CMAKE_BINARY_DIR}/bin"
        LIBRARY_OUTPUT_DIRECTORY_${CONFIG_TYPE_UPPER} "${CMAKE_BINARY_DIR}/lib"
        ARCHIVE_OUTPUT_DIRECTORY_${CONFIG_TYPE_UPPER} "${CMAKE_BINARY_DIR}/lib"
    )
endforeach()

# ============================================================================
# INSTALL
# ============================================================================

install(TARGETS aitt_native
    RUNTIME DESTINATION bin
    LIBRARY DESTINATION lib
    ARCHIVE DESTINATION lib
)

install(FILES ${NATIVE_HEADERS}
    DESTINATION include
)

# ============================================================================
# TEST EXECUTABLE (optional)
# ============================================================================

if(AITT_BUILD_TESTS)
    add_executable(aitt_native_test test/test_main.c)
    target_include_directories(aitt_native_test PRIVATE include)
    target_link_libraries(aitt_native_test PRIVATE aitt_native)
endif()

# ============================================================================
# POST-BUILD: Copy DLL to Java resources
# ============================================================================

set(JAVA_NATIVE_RESOURCES "${CMAKE_SOURCE_DIR}/../src/main/resources/native/windows-x64")

add_custom_command(TARGET aitt_native POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E make_directory "${JAVA_NATIVE_RESOURCES}"
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_FILE:aitt_native>
        "${JAVA_NATIVE_RESOURCES}/"
    COMMENT "Copying aitt_native.dll to Java resources"
)

message(STATUS "Native library will be copied to: ${JAVA_NATIVE_RESOURCES}")
