#include <jni.h>
#include <codec2.h>
#include <vector>
#include <cstdlib>

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_loradigitalvoice_Codec2Wrapper_createCodec(JNIEnv *env, jobject /* this */, jint mode) {
    // Mode: CODEC2_MODE_2400 is typically 1, CODEC2_MODE_1300 is 2, etc.
    // For now, hardcode to CODEC2_MODE_2400 which is 1.
    struct CODEC2 *c2 = codec2_create(CODEC2_MODE_2400);
    return reinterpret_cast<jlong>(c2);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_loradigitalvoice_Codec2Wrapper_destroyCodec(JNIEnv *env, jobject /* this */, jlong codec_ptr) {
    struct CODEC2 *c2 = reinterpret_cast<struct CODEC2 *>(codec_ptr);
    if (c2 != nullptr) {
        codec2_destroy(c2);
    }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_loradigitalvoice_Codec2Wrapper_encode(JNIEnv *env, jobject /* this */, jlong codec_ptr, jshortArray pcm_data) {
    struct CODEC2 *c2 = reinterpret_cast<struct CODEC2 *>(codec_ptr);
    if (c2 == nullptr) return nullptr;

    int samples_per_frame = codec2_samples_per_frame(c2);
    int bytes_per_frame = codec2_bytes_per_frame(c2);

    jsize pcm_len = env->GetArrayLength(pcm_data);
    jshort *pcm_buf = env->GetShortArrayElements(pcm_data, nullptr);

    int num_frames = pcm_len / samples_per_frame;
    std::vector<unsigned char> encoded_data(num_frames * bytes_per_frame);

    for (int i = 0; i < num_frames; i++) {
        codec2_encode(c2, encoded_data.data() + (i * bytes_per_frame), pcm_buf + (i * samples_per_frame));
    }

    env->ReleaseShortArrayElements(pcm_data, pcm_buf, 0);

    jbyteArray result = env->NewByteArray(encoded_data.size());
    env->SetByteArrayRegion(result, 0, encoded_data.size(), reinterpret_cast<const jbyte*>(encoded_data.data()));
    return result;
}

extern "C" JNIEXPORT jshortArray JNICALL
Java_com_example_loradigitalvoice_Codec2Wrapper_decode(JNIEnv *env, jobject /* this */, jlong codec_ptr, jbyteArray encoded_data) {
    struct CODEC2 *c2 = reinterpret_cast<struct CODEC2 *>(codec_ptr);
    if (c2 == nullptr) return nullptr;

    int samples_per_frame = codec2_samples_per_frame(c2);
    int bytes_per_frame = codec2_bytes_per_frame(c2);

    jsize encoded_len = env->GetArrayLength(encoded_data);
    jbyte *encoded_buf = env->GetByteArrayElements(encoded_data, nullptr);

    int num_frames = encoded_len / bytes_per_frame;
    std::vector<short> decoded_data(num_frames * samples_per_frame);

    for (int i = 0; i < num_frames; i++) {
        codec2_decode(c2, decoded_data.data() + (i * samples_per_frame), reinterpret_cast<unsigned char*>(encoded_buf) + (i * bytes_per_frame));
    }

    env->ReleaseByteArrayElements(encoded_data, encoded_buf, 0);

    jshortArray result = env->NewShortArray(decoded_data.size());
    env->SetShortArrayRegion(result, 0, decoded_data.size(), decoded_data.data());
    return result;
}
