//
// Created by User on 11.06.2019.
//

#ifndef AECM_AEC_H
#define AECM_AEC_H

#include <jni.h>
    JNIEXPORT jlong Java_com_github_shannonbay_libaecm_AEC_nativeCreateAecmInstance(JNIEnv *env,
                                                                                    jobject thiz);
    JNIEXPORT jint Java_com_github_shannonbay_libaecm_AEC_nativeFreeAecmInstance(JNIEnv *env,
                                                                                 jobject thiz, jlong aecmHandler);
    JNIEXPORT jint Java_com_github_shannonbay_libaecm_AEC_nativeInitializeAecmInstance(JNIEnv *env,
                                                                                       jobject thiz, jlong aecmHandler, jint sampFreq);
    JNIEXPORT jint Java_com_github_shannonbay_libaecm_AEC_nativeBufferFarend(JNIEnv *env,
                                                                             jobject thiz, jlong aecmHandler, jshortArray farend, jint nrOfSamples);
    JNIEXPORT jshortArray Java_com_github_shannonbay_libaecm_AEC_nativeAecmProcess(JNIEnv *env,
                                                                                   jobject thiz, jlong aecmHandler,
                                                                                   jshortArray nearendNoisy,
                                                                                   jshortArray nearendClean, jshort nrOfSamples, jshort msInSndCardBuf);
    JNIEXPORT jint Java_com_github_shannonbay_libaecm_AEC_nativeSetConfig(JNIEnv *env, jobject thiz, jlong aecmHandler, jobject aecmConfig);
#endif //AECM_AEC_H