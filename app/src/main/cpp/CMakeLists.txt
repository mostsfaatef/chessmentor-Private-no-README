# Phase 2 — Stockfish native build.
#
# Clone the official source first (do NOT vendor a binary):
#   git clone https://github.com/official-stockfish/Stockfish.git
#   cd Stockfish && git checkout <a tagged release, e.g. sf_17>
#
# Then point CMake at it, e.g. in local.properties or on the command line:
#   -DSTOCKFISH_DIR=/path/to/Stockfish
#
# If STOCKFISH_DIR is missing, this still builds a small stub library
# (STOCKFISH_AVAILABLE=0) so the app compiles and runs — the Kotlin
# side reports "Stockfish not built yet" instead of crashing.
# Once STOCKFISH_DIR is set, the real engine sources are compiled in.
cmake_minimum_required(VERSION 3.22.1)
project(stockfish)

add_library(stockfish SHARED stockfish_jni.cpp)

if(DEFINED STOCKFISH_DIR AND EXISTS "${STOCKFISH_DIR}/src")
    message(STATUS "Building Stockfish from ${STOCKFISH_DIR}")
    # All engine sources except the standalone main. File names vary
    # slightly by release; adjust this list for your tagged release.
    file(GLOB STOCKFISH_SOURCES "${STOCKFISH_DIR}/src/*.cpp")
    list(FILTER STOCKFISH_SOURCES EXCLUDE REGEX ".*main\\.cpp$")
    list(FILTER STOCKFISH_SOURCES EXCLUDE REGEX ".*benchmark\\.cpp$")
    target_sources(stockfish PRIVATE ${STOCKFISH_SOURCES})
    target_include_directories(stockfish PRIVATE "${STOCKFISH_DIR}/src")
    # Stockfish's UCI entry point differs by release (uci::loop / UCI::loop).
    # Wire it in stockfish_jni.cpp: see the HAVE_STOCKFISH block there.
    target_compile_definitions(stockfish PRIVATE STOCKFISH_AVAILABLE=1 NNUE_EMBEDDED)
else()
    message(WARNING "STOCKFISH_DIR not set or not found — building stub lib. "
        "See README §5. Clone https://github.com/official-stockfish/Stockfish.git "
        "and rebuild with -DSTOCKFISH_DIR=<path>.")
    target_compile_definitions(stockfish PRIVATE STOCKFISH_AVAILABLE=0)
endif()

find_library(log-lib log)
target_link_libraries(stockfish ${log-lib})
