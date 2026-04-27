/**
 * JNI Bindings for AITT Native Library
 * 
 * Generated method signatures from: com.aerospace.aitt.native_.HardwareBridge
 * 
 * To regenerate JNI header:
 *   javac -h native/include src/main/java/com/aerospace/aitt/native_/HardwareBridge.java
 */

#include <jni.h>
#include "aitt_native.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// ============================================================================
// JNI HELPER MACROS
// ============================================================================

#define JNI_CLASS "com/aerospace/aitt/native_/HardwareBridge"

// ============================================================================
// JNI NATIVE METHODS
// ============================================================================

/*
 * Class:     com_aerospace_aitt_native__HardwareBridge
 * Method:    nativeInit
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_aerospace_aitt_native_1_HardwareBridge_nativeInit
  (JNIEnv *env, jobject obj)
{
    int result = aitt_init();
    return result == AITT_OK ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     com_aerospace_aitt_native__HardwareBridge
 * Method:    nativeShutdown
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_aerospace_aitt_native_1_HardwareBridge_nativeShutdown
  (JNIEnv *env, jobject obj)
{
    aitt_shutdown();
}

/*
 * Class:     com_aerospace_aitt_native__HardwareBridge
 * Method:    nativeDetectHardware
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_aerospace_aitt_native_1_HardwareBridge_nativeDetectHardware
  (JNIEnv *env, jobject obj)
{
    #define MAX_PROBES 32
    ProbeInfo probes[MAX_PROBES];
    
    int count = aitt_detect_hardware(probes, MAX_PROBES);
    if (count < 0) {
        return NULL;
    }
    
    // Create String array
    jclass stringClass = (*env)->FindClass(env, "java/lang/String");
    jobjectArray result = (*env)->NewObjectArray(env, count, stringClass, NULL);
    
    if (result == NULL) {
        return NULL;
    }
    
    // Format: "TYPE:ID:NAME:STATUS"
    for (int i = 0; i < count; i++) {
        char buffer[512];
        const char* typeStr = probes[i].type == PROBE_TYPE_USB ? "USB" :
                              probes[i].type == PROBE_TYPE_SERIAL ? "SERIAL" :
                              probes[i].type == PROBE_TYPE_JTAG ? "JTAG" :
                              probes[i].type == PROBE_TYPE_SWD ? "SWD" : "UNKNOWN";
        
        const char* statusStr = probes[i].status == PROBE_STATUS_AVAILABLE ? "AVAILABLE" :
                                probes[i].status == PROBE_STATUS_IN_USE ? "IN_USE" :
                                probes[i].status == PROBE_STATUS_ERROR ? "ERROR" : "UNKNOWN";
        
        snprintf(buffer, sizeof(buffer), "%s:%s:%s:%s",
                 typeStr, probes[i].id, probes[i].name, statusStr);
        
        jstring str = (*env)->NewStringUTF(env, buffer);
        (*env)->SetObjectArrayElement(env, result, i, str);
        (*env)->DeleteLocalRef(env, str);
    }
    
    return result;
}

/*
 * Class:     com_aerospace_aitt_native__HardwareBridge
 * Method:    nativeDetectPorts
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_aerospace_aitt_native_1_HardwareBridge_nativeDetectPorts
  (JNIEnv *env, jobject obj)
{
    #define MAX_PORTS 64
    #define PORT_BUF_SIZE 32
    
    char* ports[MAX_PORTS];
    for (int i = 0; i < MAX_PORTS; i++) {
        ports[i] = (char*)malloc(PORT_BUF_SIZE);
    }
    
    int count = aitt_detect_ports(ports, MAX_PORTS, PORT_BUF_SIZE);
    
    jclass stringClass = (*env)->FindClass(env, "java/lang/String");
    jobjectArray result = (*env)->NewObjectArray(env, count, stringClass, NULL);
    
    for (int i = 0; i < count; i++) {
        jstring str = (*env)->NewStringUTF(env, ports[i]);
        (*env)->SetObjectArrayElement(env, result, i, str);
        (*env)->DeleteLocalRef(env, str);
    }
    
    for (int i = 0; i < MAX_PORTS; i++) {
        free(ports[i]);
    }
    
    return result;
}

/*
 * Class:     com_aerospace_aitt_native__HardwareBridge
 * Method:    nativeConnectProbe
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_aerospace_aitt_native_1_HardwareBridge_nativeConnectProbe
  (JNIEnv *env, jobject obj, jstring probeId)
{
    if (probeId == NULL) {
        return AITT_ERROR_INVALID_PARAM;
    }
    
    const char* id = (*env)->GetStringUTFChars(env, probeId, NULL);
    if (id == NULL) {
        return AITT_ERROR_MEMORY;
    }
    
    int result = aitt_connect(id);
    
    (*env)->ReleaseStringUTFChars(env, probeId, id);
    return result;
}

/*
 * Class:     com_aerospace_aitt_native__HardwareBridge
 * Method:    nativeDisconnectProbe
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_aerospace_aitt_native_1_HardwareBridge_nativeDisconnectProbe
  (JNIEnv *env, jobject obj)
{
    return aitt_disconnect();
}

/*
 * Class:     com_aerospace_aitt_native__HardwareBridge
 * Method:    nativeSendCommand
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_aerospace_aitt_native_1_HardwareBridge_nativeSendCommand
  (JNIEnv *env, jobject obj, jstring command)
{
    if (command == NULL) {
        return (*env)->NewStringUTF(env, "ERROR:-3:Null command");
    }
    
    const char* cmd = (*env)->GetStringUTFChars(env, command, NULL);
    if (cmd == NULL) {
        return (*env)->NewStringUTF(env, "ERROR:-8:Memory error");
    }
    
    CommandResult result;
    int ret = aitt_send_command(cmd, &result, 5000);
    
    (*env)->ReleaseStringUTFChars(env, command, cmd);
    
    char response[4096];
    if (ret == AITT_OK) {
        snprintf(response, sizeof(response), "OK:%s", result.response);
    } else {
        snprintf(response, sizeof(response), "ERROR:%d:%s", 
                 result.errorCode, result.errorMessage[0] ? result.errorMessage : aitt_get_last_error());
    }
    
    return (*env)->NewStringUTF(env, response);
}

/*
 * Class:     com_aerospace_aitt_native__HardwareBridge
 * Method:    nativeSendCommandWithTimeout
 * Signature: (Ljava/lang/String;I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_aerospace_aitt_native_1_HardwareBridge_nativeSendCommandWithTimeout
  (JNIEnv *env, jobject obj, jstring command, jint timeoutMs)
{
    if (command == NULL) {
        return (*env)->NewStringUTF(env, "ERROR:-3:Null command");
    }
    
    const char* cmd = (*env)->GetStringUTFChars(env, command, NULL);
    if (cmd == NULL) {
        return (*env)->NewStringUTF(env, "ERROR:-8:Memory error");
    }
    
    CommandResult result;
    int ret = aitt_send_command(cmd, &result, timeoutMs);
    
    (*env)->ReleaseStringUTFChars(env, command, cmd);
    
    char response[4096];
    if (ret == AITT_OK) {
        snprintf(response, sizeof(response), "OK:%s", result.response);
    } else {
        snprintf(response, sizeof(response), "ERROR:%d:%s", 
                 result.errorCode, result.errorMessage[0] ? result.errorMessage : aitt_get_last_error());
    }
    
    return (*env)->NewStringUTF(env, response);
}

/*
 * Class:     com_aerospace_aitt_native__HardwareBridge
 * Method:    nativeReadMemory
 * Signature: (JI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_aerospace_aitt_native_1_HardwareBridge_nativeReadMemory
  (JNIEnv *env, jobject obj, jlong address, jint length)
{
    if (length <= 0 || length > 1024*1024) { // Max 1MB read
        return NULL;
    }
    
    uint8_t* buffer = (uint8_t*)malloc(length);
    if (buffer == NULL) {
        return NULL;
    }
    
    int bytesRead = aitt_read_memory((uint64_t)address, buffer, length);
    
    if (bytesRead < 0) {
        free(buffer);
        return NULL;
    }
    
    jbyteArray result = (*env)->NewByteArray(env, bytesRead);
    if (result != NULL) {
        (*env)->SetByteArrayRegion(env, result, 0, bytesRead, (jbyte*)buffer);
    }
    
    free(buffer);
    return result;
}

/*
 * Class:     com_aerospace_aitt_native__HardwareBridge
 * Method:    nativeWriteMemory
 * Signature: (J[B)I
 */
JNIEXPORT jint JNICALL Java_com_aerospace_aitt_native_1_HardwareBridge_nativeWriteMemory
  (JNIEnv *env, jobject obj, jlong address, jbyteArray data)
{
    if (data == NULL) {
        return AITT_ERROR_INVALID_PARAM;
    }
    
    jsize length = (*env)->GetArrayLength(env, data);
    jbyte* bytes = (*env)->GetByteArrayElements(env, data, NULL);
    
    if (bytes == NULL) {
        return AITT_ERROR_MEMORY;
    }
    
    int result = aitt_write_memory((uint64_t)address, (uint8_t*)bytes, length);
    
    (*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
    
    return result;
}

/*
 * Class:     com_aerospace_aitt_native__HardwareBridge
 * Method:    nativeGetLastError
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_aerospace_aitt_native_1_HardwareBridge_nativeGetLastError
  (JNIEnv *env, jobject obj)
{
    return (*env)->NewStringUTF(env, aitt_get_last_error());
}

/*
 * Class:     com_aerospace_aitt_native__HardwareBridge
 * Method:    nativeGetVersion
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_aerospace_aitt_native_1_HardwareBridge_nativeGetVersion
  (JNIEnv *env, jobject obj)
{
    return (*env)->NewStringUTF(env, aitt_get_version());
}

/*
 * Class:     com_aerospace_aitt_native__HardwareBridge
 * Method:    nativeConfigureChipset
 * Signature: (Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_aerospace_aitt_native_1_HardwareBridge_nativeConfigureChipset
  (JNIEnv *env, jobject obj, jstring chipsetId, jstring configJson)
{
    if (chipsetId == NULL || configJson == NULL) {
        return AITT_ERROR_INVALID_PARAM;
    }
    
    const char* id = (*env)->GetStringUTFChars(env, chipsetId, NULL);
    const char* json = (*env)->GetStringUTFChars(env, configJson, NULL);
    
    if (id == NULL || json == NULL) {
        if (id) (*env)->ReleaseStringUTFChars(env, chipsetId, id);
        if (json) (*env)->ReleaseStringUTFChars(env, configJson, json);
        return AITT_ERROR_MEMORY;
    }
    
    // Parse JSON configuration (simplified)
    ChipsetConfig config = {0};
    strncpy(config.chipsetId, id, sizeof(config.chipsetId) - 1);
    
    // TODO: Full JSON parsing with a library like cJSON
    // For now, just copy the chipset ID
    
    int result = aitt_configure_chipset(&config);
    
    (*env)->ReleaseStringUTFChars(env, chipsetId, id);
    (*env)->ReleaseStringUTFChars(env, configJson, json);
    
    return result;
}
