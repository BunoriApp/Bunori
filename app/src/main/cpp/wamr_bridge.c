#include <jni.h>
#include <wasm_export.h>
#include <android/log.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include <stdbool.h>

#define LOG_TAG "BunoriWamr"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static JavaVM *g_jvm = NULL;
static jclass g_http_bridge_class = NULL;
static jmethodID g_http_execute_method = NULL;

static jclass g_string_class = NULL;
static jmethodID g_string_constructor = NULL;
static jstring g_utf8_encoding = NULL;

static uint64_t host_time_ms(wasm_exec_env_t exec_env) {
    struct timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts);
    return (uint64_t)ts.tv_sec * 1000 + (ts.tv_nsec / 1000000);
}

static void host_log(wasm_exec_env_t exec_env, int32_t level, int32_t msg_ptr, int32_t msg_len) {
    wasm_module_inst_t inst = wasm_runtime_get_module_inst(exec_env);
    if (!inst) return;
    char *msg = (char *)wasm_runtime_addr_app_to_native(inst, msg_ptr);
    if (msg && msg_len > 0) {
        __android_log_print(ANDROID_LOG_INFO, "BunoriExtensions", "%.*s", msg_len, msg);
    }
}

static uint64_t host_http(wasm_exec_env_t exec_env, int32_t req_ptr, int32_t req_len) {
    wasm_module_inst_t inst = wasm_runtime_get_module_inst(exec_env);
    if (!inst) return 0;
    char *req_json = (char *)wasm_runtime_addr_app_to_native(inst, req_ptr);
    if (!req_json || req_len <= 0) return 0;

    JNIEnv *env;
    (*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL);

    char *req_buf = (char *)malloc(req_len + 1);
    if (!req_buf) return 0;
    memcpy(req_buf, req_json, req_len);
    req_buf[req_len] = '\0';

    jstring j_req = (*env)->NewStringUTF(env, req_buf);
    free(req_buf);

    jbyteArray j_res = (jbyteArray)(*env)->CallStaticObjectMethod(env, g_http_bridge_class, g_http_execute_method, j_req);
    (*env)->DeleteLocalRef(env, j_req);

    if (!j_res) return 0;

    jsize res_len = (*env)->GetArrayLength(env, j_res);

    wasm_function_inst_t alloc_func = wasm_runtime_lookup_function(inst, "alloc");
    uint32_t wasm_dest_ptr = 0;
    if (alloc_func) {
        uint32_t argv[1] = {(uint32_t)res_len + 1};
        if (wasm_runtime_call_wasm(exec_env, alloc_func, 1, argv)) {
            wasm_dest_ptr = argv[0];
        } else {
            LOGI("host_http: 'alloc' call failed: %s", wasm_runtime_get_exception(inst));
        }
    }
    if (!wasm_dest_ptr) {
        void *native_addr = NULL;
        wasm_dest_ptr = (uint32_t)wasm_runtime_module_malloc(inst, res_len + 1, &native_addr);
    }

    if (!wasm_dest_ptr) {
        LOGI("host_http: failed to allocate wasm memory for response (%d bytes)", res_len);
        (*env)->DeleteLocalRef(env, j_res);
        return 0;
    }

    char *dest = (char *)wasm_runtime_addr_app_to_native(inst, wasm_dest_ptr);
    if (!dest) {
        LOGI("host_http: failed to resolve native address for wasm_dest_ptr %u", wasm_dest_ptr);
        (*env)->DeleteLocalRef(env, j_res);
        return 0;
    }

    (*env)->GetByteArrayRegion(env, j_res, 0, res_len, (jbyte *)dest);
    dest[res_len] = '\0';
    (*env)->DeleteLocalRef(env, j_res);

    return ((uint64_t)wasm_dest_ptr) | (((uint64_t)res_len) << 32);
}

static NativeSymbol g_native_symbols[] = {
    {"host_time_ms", (void *)host_time_ms, "()I", NULL},
    {"host_log", (void *)host_log, "(iii)", NULL},
    {"host_http", (void *)host_http, "(ii)I", NULL}
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_jvm = vm;
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;

    RuntimeInitArgs init_args;
    memset(&init_args, 0, sizeof(RuntimeInitArgs));
    init_args.mem_alloc_type = Alloc_With_Allocator;
    init_args.mem_alloc_option.allocator.malloc_func = malloc;
    init_args.mem_alloc_option.allocator.realloc_func = realloc;
    init_args.mem_alloc_option.allocator.free_func = free;

    if (!wasm_runtime_full_init(&init_args)) return JNI_ERR;

    wasm_runtime_register_natives("bunori", g_native_symbols, sizeof(g_native_symbols) / sizeof(NativeSymbol));

    jclass clz = (*env)->FindClass(env, "com/halovoid/bunori/wasm/WamrHttpBridge");
    g_http_bridge_class = (jclass)(*env)->NewGlobalRef(env, clz);
    g_http_execute_method = (*env)->GetStaticMethodID(env, g_http_bridge_class, "execute", "(Ljava/lang/String;)[B");

    jclass str_clz = (*env)->FindClass(env, "java/lang/String");
    g_string_class = (jclass)(*env)->NewGlobalRef(env, str_clz);
    g_string_constructor = (*env)->GetMethodID(env, g_string_class, "<init>", "([BLjava/lang/String;)V");
    g_utf8_encoding = (jstring)(*env)->NewGlobalRef(env, (*env)->NewStringUTF(env, "UTF-8"));

    return JNI_VERSION_1_6;
}

JNIEXPORT jstring JNICALL
Java_com_halovoid_bunori_wasm_WamrBridge_nativeCallString(JNIEnv *env, jclass clz, jlong inst_ptr, jstring func_name, jstring arg_str, jint page) {
    wasm_module_inst_t inst = (wasm_module_inst_t)inst_ptr;
    if (!inst) {
        LOGI("nativeCallString: module instance is NULL");
        return NULL;
    }

    const char *func_cstr = (*env)->GetStringUTFChars(env, func_name, NULL);
    wasm_exec_env_t exec_env = wasm_runtime_get_exec_env_singleton(inst);
    if (!exec_env) {
        LOGI("nativeCallString: exec_env is NULL");
        (*env)->ReleaseStringUTFChars(env, func_name, func_cstr);
        return NULL;
    }

    uint32_t str_ptr = 0;
    uint32_t str_len = 0;
    bool str_allocated_via_wasm = false;
    wasm_function_inst_t dealloc_func = wasm_runtime_lookup_function(inst, "dealloc");

    if (arg_str != NULL) {
        const char *arg_cstr = (*env)->GetStringUTFChars(env, arg_str, NULL);
        str_len = strlen(arg_cstr);

        wasm_function_inst_t alloc_func = wasm_runtime_lookup_function(inst, "alloc");
        if (alloc_func) {
            uint32_t argv[1] = {str_len + 1};
            if (wasm_runtime_call_wasm(exec_env, alloc_func, 1, argv)) {
                str_ptr = argv[0];
                str_allocated_via_wasm = true;
            } else {
                LOGI("nativeCallString: 'alloc' call failed: %s", wasm_runtime_get_exception(inst));
            }
        }
        if (!str_ptr) {
            void *native_dest = NULL;
            str_ptr = (uint32_t)wasm_runtime_module_malloc(inst, str_len + 1, &native_dest);
            str_allocated_via_wasm = false;
        }

        if (!str_ptr) {
            LOGI("nativeCallString: failed to allocate memory for arg_str");
            (*env)->ReleaseStringUTFChars(env, arg_str, arg_cstr);
            (*env)->ReleaseStringUTFChars(env, func_name, func_cstr);
            return NULL;
        }

        char *native_dest = (char *)wasm_runtime_addr_app_to_native(inst, str_ptr);
        if (!native_dest) {
            LOGI("nativeCallString: failed to resolve native address for str_ptr %u", str_ptr);
            (*env)->ReleaseStringUTFChars(env, arg_str, arg_cstr);
            (*env)->ReleaseStringUTFChars(env, func_name, func_cstr);
            return NULL;
        }
        memcpy(native_dest, arg_cstr, str_len);
        native_dest[str_len] = '\0';
        (*env)->ReleaseStringUTFChars(env, arg_str, arg_cstr);
    }

    wasm_function_inst_t func = wasm_runtime_lookup_function(inst, func_cstr);
    if (!func) {
        LOGI("nativeCallString: function '%s' not found in module instance!", func_cstr);
        if (str_ptr != 0) {
            if (str_allocated_via_wasm && dealloc_func != NULL) {
                uint32_t dealloc_arg_argv[2] = {str_ptr, str_len + 1};
                wasm_runtime_call_wasm(exec_env, dealloc_func, 2, dealloc_arg_argv);
            } else {
                wasm_runtime_module_free(inst, str_ptr);
            }
        }
        (*env)->ReleaseStringUTFChars(env, func_name, func_cstr);
        return NULL;
    }

    uint64_t packed = 0;
    bool call_ok = false;
    if (page >= 0) {
        uint32_t call_argv[3] = {str_ptr, str_len, (uint32_t)page};
        call_ok = wasm_runtime_call_wasm(exec_env, func, 3, call_argv);
        packed = *(uint64_t *)call_argv;
    } else if (arg_str != NULL) {
        uint32_t call_argv[2] = {str_ptr, str_len};
        call_ok = wasm_runtime_call_wasm(exec_env, func, 2, call_argv);
        packed = *(uint64_t *)call_argv;
    } else {
        uint32_t call_argv[1] = {0};
        call_ok = wasm_runtime_call_wasm(exec_env, func, 0, call_argv);
        packed = *(uint64_t *)call_argv;
    }

    (*env)->ReleaseStringUTFChars(env, func_name, func_cstr);

    if (str_ptr != 0) {
        if (str_allocated_via_wasm && dealloc_func != NULL) {
            uint32_t dealloc_arg_argv[2] = {str_ptr, str_len + 1};
            wasm_runtime_call_wasm(exec_env, dealloc_func, 2, dealloc_arg_argv);
        } else {
            wasm_runtime_module_free(inst, str_ptr);
        }
    }

    if (!call_ok) {
        LOGI("nativeCallString: invocation of '%s' failed: %s", func_cstr, wasm_runtime_get_exception(inst));
        return NULL;
    }

    if (packed == 0) return NULL;

    uint32_t res_ptr = (uint32_t)(packed & 0xFFFFFFFF);
    uint32_t res_len = (uint32_t)(packed >> 32);

    char *res_chars = (char *)wasm_runtime_addr_app_to_native(inst, res_ptr);
    if (!res_chars || res_len == 0) return NULL;

    jbyteArray j_bytes = (*env)->NewByteArray(env, res_len);
    if (!j_bytes) {
        if (dealloc_func != NULL) {
            uint32_t dealloc_argv[2] = {res_ptr, res_len};
            wasm_runtime_call_wasm(exec_env, dealloc_func, 2, dealloc_argv);
        }
        return NULL;
    }
    (*env)->SetByteArrayRegion(env, j_bytes, 0, res_len, (jbyte *)res_chars);

    jstring result = (jstring)(*env)->NewObject(env, g_string_class, g_string_constructor, j_bytes, g_utf8_encoding);
    (*env)->DeleteLocalRef(env, j_bytes);

    if (dealloc_func != NULL) {
        uint32_t dealloc_argv[2] = {res_ptr, res_len};
        wasm_runtime_call_wasm(exec_env, dealloc_func, 2, dealloc_argv);
    }
    
    return result;
}

typedef struct {
    wasm_module_t module;
    uint8_t *file_buf;
} BunoriModuleWrapper;

JNIEXPORT jlong JNICALL
Java_com_halovoid_bunori_wasm_WamrBridge_nativeLoad(JNIEnv *env, jclass clz, jbyteArray bytes) {
    if (!bytes) return 0;
    jsize size = (*env)->GetArrayLength(env, bytes);
    if (size <= 0) return 0;

    jbyte *buf = (*env)->GetByteArrayElements(env, bytes, NULL);
    if (!buf) return 0;

    BunoriModuleWrapper *wrapper = (BunoriModuleWrapper *)malloc(sizeof(BunoriModuleWrapper));
    if (!wrapper) {
        (*env)->ReleaseByteArrayElements(env, bytes, buf, JNI_ABORT);
        return 0;
    }

    wrapper->file_buf = (uint8_t *)malloc(size);
    if (!wrapper->file_buf) {
        free(wrapper);
        (*env)->ReleaseByteArrayElements(env, bytes, buf, JNI_ABORT);
        return 0;
    }

    memcpy(wrapper->file_buf, buf, size);
    (*env)->ReleaseByteArrayElements(env, bytes, buf, JNI_ABORT);

    char error_buf[128] = {0};
    wrapper->module = wasm_runtime_load(wrapper->file_buf, size, error_buf, sizeof(error_buf));
    if (!wrapper->module) {
        LOGI("Failed to load module : %s", error_buf);
        free(wrapper->file_buf);
        free(wrapper);
        return 0;
    }
    
    return (jlong)wrapper;
}

JNIEXPORT jlong JNICALL
Java_com_halovoid_bunori_wasm_WamrBridge_nativeInstantiate(JNIEnv *env, jclass clz, jlong module_ptr) {
    if (!module_ptr) return 0;
    BunoriModuleWrapper *wrapper = (BunoriModuleWrapper *)module_ptr;
    if (!wrapper->module) return 0;

    char error_buf[128] = {0};
    wasm_module_inst_t inst = wasm_runtime_instantiate(wrapper->module, 128 * 1024, 512 * 1024, error_buf, sizeof(error_buf));
    if (!inst) {
        LOGI("Failed to instantiate module : %s", error_buf);
        return 0;
    }
    LOGI("Instantiated module. Functions found: alloc=%p, dealloc=%p, search=%p, get_listings=%p",
         wasm_runtime_lookup_function(inst, "alloc"),
         wasm_runtime_lookup_function(inst, "dealloc"),
         wasm_runtime_lookup_function(inst, "search"),
         wasm_runtime_lookup_function(inst, "get_listings"));
    return (jlong)inst;
}

JNIEXPORT void JNICALL
Java_com_halovoid_bunori_wasm_WamrBridge_nativeDestroy(JNIEnv *env, jclass clz, jlong inst_ptr, jlong module_ptr) {
    if (inst_ptr) {
        wasm_runtime_deinstantiate((wasm_module_inst_t)inst_ptr);
    }
    if (module_ptr) {
        BunoriModuleWrapper *wrapper = (BunoriModuleWrapper *)module_ptr;
        if (wrapper->module) {
            wasm_runtime_unload(wrapper->module);
            wrapper->module = NULL;
        }
        if (wrapper->file_buf) {
            free(wrapper->file_buf);
            wrapper->file_buf = NULL;
        }
        free(wrapper);
    }
}