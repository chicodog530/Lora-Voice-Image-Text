package com.example.loradigitalvoice

class Codec2Wrapper {

    private var codecPtr: Long = 0

    init {
        System.loadLibrary("loravoice")
    }

    fun initCodec(mode: Int = 1) {
        if (codecPtr == 0L) {
            codecPtr = createCodec(mode)
        }
    }

    fun encode(pcmData: ShortArray): ByteArray? {
        if (codecPtr == 0L) return null
        return encode(codecPtr, pcmData)
    }

    fun decode(encodedData: ByteArray): ShortArray? {
        if (codecPtr == 0L) return null
        return decode(codecPtr, encodedData)
    }

    fun release() {
        if (codecPtr != 0L) {
            destroyCodec(codecPtr)
            codecPtr = 0
        }
    }

    private external fun createCodec(mode: Int): Long
    private external fun destroyCodec(codecPtr: Long)
    private external fun encode(codecPtr: Long, pcmData: ShortArray): ByteArray
    private external fun decode(codecPtr: Long, encodedData: ByteArray): ShortArray
}
