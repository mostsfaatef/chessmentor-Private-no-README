// Phase 2 — tiny JNI surface for Stockfish.
//
// JNI functions (must match StockfishEngine.kt exactly):
//   nativeStart() / nativeStop()
//   nativeSendCommand(String) / nativeReadLine(): String?
//   nativeIsAvailable(): Boolean
//
// Design: one worker thread owns the engine. Kotlin sends UCI lines
// into a command queue; the worker feeds them to the engine and
// pushes reply lines into a response queue that nativeReadLine()
// drains. All UCI parsing lives in Kotlin.
//
// STOCKFISH_AVAILABLE is set by CMakeLists.txt:
//  - 0 (default): no engine sources were found. The worker thread
//    runs but produces no replies, so Kotlin times out and reports
//    "Stockfish not built yet". The app still builds and runs.
//  - 1: engine sources were compiled in. Wire the release's UCI
//    entry point in the HAVE_STOCKFISH block below — the exact
//    symbol differs by Stockfish release (see README §5), so it is
//    a one-line, release-specific call, not a second engine.
//    Until that line is wired for your tagged release, the build
//    still succeeds and Kotlin reports the engine as unavailable.
#include <jni.h>

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <mutex>
#include <queue>
#include <string>
#include <thread>

#ifndef STOCKFISH_AVAILABLE
#define STOCKFISH_AVAILABLE 0
#endif

namespace {

std::mutex g_mutex;
std::condition_variable g_cmdCv;
std::condition_variable g_respCv;
std::queue<std::string> g_commands;
std::queue<std::string> g_responses;
std::thread g_worker;
std::atomic<bool> g_running{false};

void pushResponse(const std::string& line) {
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        g_responses.push(line);
    }
    g_respCv.notify_one();
}

void workerLoop() {
#if STOCKFISH_AVAILABLE
    // TODO (one line, release-specific): start the engine's UCI loop
    // on this thread, feeding it lines from g_commands and pushing
    // its replies with pushResponse(). Example shape for recent
    // releases — verify the symbol for your tagged release:
    //   Stockfish::UCI::loop(...);  // or uci::loop(argc, argv)
    // Until this is wired, fall through to the drain loop below so
    // the library still loads and Kotlin can report "not available".
    (void)0;
#endif
    // Drain loop: keeps the thread alive and handles "quit".
    // With no engine wired, commands are dropped and no replies
    // are produced — Kotlin's read timeout handles that gracefully.
    while (g_running.load()) {
        std::string cmd;
        {
            std::unique_lock<std::mutex> lock(g_mutex);
            g_cmdCv.wait(lock, [] { return !g_commands.empty() || !g_running.load(); });
            if (!g_running.load()) break;
            cmd = g_commands.front();
            g_commands.pop();
        }
        if (cmd == "quit") break;
        // Engine replies, when wired, arrive here via pushResponse().
    }
}

std::string toCppString(JNIEnv* env, jstring value) {
    if (value == nullptr) return "";
    const char* chars = env->GetStringUTFChars(value, nullptr);
    std::string out = chars != nullptr ? chars : "";
    if (chars != nullptr) env->ReleaseStringUTFChars(value, chars);
    return out;
}

}  // namespace

extern "C" {

JNIEXPORT void JNICALL
Java_com_chessmentor_chess_engine_StockfishEngine_nativeStart(JNIEnv* /*env*/, jobject /*thiz*/) {
    if (g_running.load()) return;
    g_running.store(true);
    g_worker = std::thread(workerLoop);
}

JNIEXPORT void JNICALL
Java_com_chessmentor_chess_engine_StockfishEngine_nativeStop(JNIEnv* /*env*/, jobject /*thiz*/) {
    if (!g_running.load()) return;
    g_running.store(false);
    g_cmdCv.notify_all();
    g_respCv.notify_all();
    if (g_worker.joinable()) g_worker.join();
}

JNIEXPORT void JNICALL
Java_com_chessmentor_chess_engine_StockfishEngine_nativeSendCommand(
    JNIEnv* env, jobject /*thiz*/, jstring command) {
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        g_commands.push(toCppString(env, command));
    }
    g_cmdCv.notify_one();
}

JNIEXPORT jstring JNICALL
Java_com_chessmentor_chess_engine_StockfishEngine_nativeReadLine(JNIEnv* env, jobject /*thiz*/) {
    std::string line;
    {
        std::unique_lock<std::mutex> lock(g_mutex);
        // Short poll: Kotlin loops with its own deadline, so never block here.
        if (!g_respCv.wait_for(lock, std::chrono::milliseconds(100),
                               [] { return !g_responses.empty(); })) {
            return nullptr;
        }
        line = g_responses.front();
        g_responses.pop();
    }
    return env->NewStringUTF(line.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_chessmentor_chess_engine_StockfishEngine_nativeIsAvailable(JNIEnv* /*env*/, jobject /*thiz*/) {
#if STOCKFISH_AVAILABLE
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

}  // extern "C"
