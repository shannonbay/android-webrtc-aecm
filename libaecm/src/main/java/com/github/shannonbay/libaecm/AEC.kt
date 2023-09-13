package com.github.shannonbay.libaecm

import android.util.Log

class AEC {
    // /////////////////////////////////////////////////////////
    // PUBLIC NESTED CLASSES
    /**
     * For security reason, this class supports constant sampling frequency values in
     * [FS_8000Hz][SamplingFrequency.FS_8000Hz], [FS_16000Hz][SamplingFrequency.FS_16000Hz]
     */
    class SamplingFrequency private constructor(val fS: Int) {

        companion object {
            /**
             * This constant represents sampling frequency in 8000Hz
             */
            val FS_8000Hz = SamplingFrequency(
                8000
            )

            /**
             * This constant represents sampling frequency in 16000Hz
             */
            val FS_16000Hz = SamplingFrequency(
                16000
            )
        }
    }

    /**
     * For security reason, this class supports constant aggressiveness of the AECM instance in
     * [MILD][AggressiveMode.MILD], [MEDIUM][AggressiveMode.MEDIUM], [HIGH][AggressiveMode.HIGH],
     * [AGGRESSIVE][AggressiveMode.AGGRESSIVE], [MOST_AGGRESSIVE][AggressiveMode.MOST_AGGRESSIVE].
     */
    class AggressiveMode private constructor(val mode: Int) {

        companion object {
            /**
             * This constant represents the aggressiveness of the AECM instance in MILD_MODE
             */
            val MILD = AggressiveMode(0)

            /**
             * This constant represents the aggressiveness of the AECM instance in MEDIUM_MODE
             */
            val MEDIUM = AggressiveMode(1)

            /**
             * This constant represents the aggressiveness of the AECM instance in HIGH_MODE
             */
            val HIGH = AggressiveMode(2)

            /**
             * This constant represents the aggressiveness of the AECM instance in AGGRESSIVE_MODE
             */
            val AGGRESSIVE = AggressiveMode(3)

            /**
             * This constant represents the aggressiveness of the AECM instance in MOST_AGGRESSIVE_MODE
             */
            val MOST_AGGRESSIVE = AggressiveMode(4)
        }
    }

    // /////////////////////////////////////////////////////////
    // PRIVATE MEMBERS
    private var mAecmHandler: Long = -1 // the handler of AECM instance.
    private var mAecmConfig: AecmConfig? = null // the configurations of AECM instance.
    private var mSampFreq: SamplingFrequency? = null // sampling frequency of input speech data.
    private var mIsInit = false // whether the AECM instance is initialized or not.
    // /////////////////////////////////////////////////////////
    // CONSTRUCTOR
    /**
     * Generate a new AECM instance
     */
    constructor() {
        mAecmHandler = nativeCreateAecmInstance()
        mAecmConfig = AecmConfig()
        Log.d(TAG, "AECM instance successfully created")
    }
    /**
     * To generate a new AECM instance, whether you set the sampling frequency and aggresive mode or not are both ok.
     *
     * @param sampFreqOfData - sampling frequency of input audio data. if null, then [FS_16000Hz][SamplingFrequency.FS_16000Hz] is set.
     * @param aggressiveMode - aggressiveness mode of AECM instance, more higher the mode is, more aggressive the instance will be.
     * if null, then [AGGRESSIVE][AggressiveMode.AGGRESSIVE] is set.
     */
    /**
     * To generate a new AECM instance, whether you set the sampling frequency of each parameter or not are both ok.
     *
     * @param sampFreqOfData - sampling frequency of input audio data. if null, then [FS_16000Hz][SamplingFrequency.FS_16000Hz] is set.
     */
    @kotlin.jvm.JvmOverloads
    constructor(sampFreqOfData: SamplingFrequency?, aggressiveMode: AggressiveMode? = null) {
        // create new AECM instance but without initialize. Init things are in prepare() method instead.
        mAecmHandler = nativeCreateAecmInstance()
        setSampFreq(sampFreqOfData, false)
        mAecmConfig = AecmConfig()
        setAecmMode(aggressiveMode, false)
        Log.d(TAG, "AECM instance successfully created")
        prepare()
    }

    // /////////////////////////////////////////////////////////
    // PUBLIC METHODS
    fun setSampFreq(frequency: SamplingFrequency?) {
        setSampFreq(frequency, true)
    }

    /**
     * set the sampling rate of speech data.
     *
     * @param frequency - sampling frequency of speech data, if null then [FS_16000Hz][SamplingFrequency.FS_16000Hz] is set.
     * @param prepare - is flag that indicate will or will not prepare AECM instance after sampling frequency was set.
     */
    private fun setSampFreq(frequency: SamplingFrequency?, prepare: Boolean) {
        mSampFreq = if (frequency == null) {
            Log.d(
                TAG,
                "setSampFreq() frequency == null, SamplingFrequency.FS_16000Hz will be used instead"
            )
            SamplingFrequency.FS_16000Hz
        } else frequency
        if (prepare) prepare()
    }

    /**
     * set the far-end signal of AECM instance.
     *
     * @param farendFrame
     * @param frameLength
     * @return the [AEC] object itself or null if farendBuffer() is called on an unprepared AECM instance
     * or you pass an invalid parameter.
     */
    fun farendBuffer(farendFrame: ShortArray, frameLength: Int): AEC? {
        // check if AECM instance is not initialized.
        if (!mIsInit) {
            Log.d(
                TAG,
                "farendBuffer() is called on an unprepared AECM instance or you pass an invalid parameter"
            )
            return null
        }
        if (nativeBufferFarend(mAecmHandler, farendFrame, frameLength) == -1) {
            Log.d(TAG, "farendBuffer() failed due to invalid arguments")
            return null
        }
        return this
    }

    /**
     * core process of AECM instance, must called on a prepared AECM instance. we only support 80 or 160 sample blocks
     * of data.
     *
     * @param nearendNoisy
     * - In buffer containing one frame of reference nearend+echo signal. If noise reduction is active,
     * provide the noisy signal here.
     * @param nearendClean
     * - In buffer containing one frame of nearend+echo signal. If noise reduction is active, provide the
     * clean signal here. Otherwise pass a NULL pointer
     * or just call [.echoCancellation].
     * @param numOfSamples
     * - Number of samples in nearend buffer. Must be <= Short.MAX_VALUE and >= Short.MIN_VALUE
     * @param delay
     * - Delay estimate for sound card and system buffers <br></br>
     * delay = (t_render - t_analyze) + (t_process - t_capture)<br></br>
     * where<br></br>
     * - t_analyze is the time a frame is passed to farendBuffer() and t_render is the time the first sample
     * of the same frame is rendered by the audio hardware.<br></br>
     * - t_capture is the time the first sample of a frame is captured by the audio hardware and t_process is
     * the time the same frame is passed to echoCancellation(). Must be <= Short.MAX_VALUE and >= Short.MIN_VALUE
     *
     * @return one processed frame without echo or null if echoCancellation() is called on an unprepared AECM instance
     * or you pass an invalid parameter.
     */
    fun echoCancellation(
        nearendNoisy: ShortArray,
        nearendClean: ShortArray?,
        numOfSamples: Short,
        delay: Short
    ): ShortArray? {
        // check if AECM instance is not initialized.
        var numOfSamples = numOfSamples
        var delay = delay
        if (!mIsInit) {
            Log.d(
                TAG,
                "echoCancellation() is called on an unprepared AECM instance or you pass an invalid parameter"
            )
            return null
        }
        if (numOfSamples > Short.MAX_VALUE) {
            Log.d(
                TAG,
                "echoCancellation() numOfSamples > Short.MAX_VALUE, Short.MAX_VALUE will be used instead"
            )
            numOfSamples = Short.MAX_VALUE
        } else if (numOfSamples < Short.MIN_VALUE) {
            Log.d(
                TAG,
                "echoCancellation() numOfSamples < Short.MIN_VALUE, Short.MIN_VALUE will be used instead"
            )
            numOfSamples = Short.MIN_VALUE
        }
        if (delay > Short.MAX_VALUE) {
            Log.d(
                TAG,
                "echoCancellation() delay > Short.MAX_VALUE, Short.MAX_VALUE will be used instead"
            )
            delay = Short.MAX_VALUE
        } else if (delay < Short.MIN_VALUE) {
            Log.d(
                TAG,
                "echoCancellation() delay < Short.MIN_VALUE, Short.MIN_VALUE will be used instead"
            )
            delay = Short.MIN_VALUE
        }
        return nativeAecmProcess(
            mAecmHandler,
            nearendNoisy,
            nearendClean,
            numOfSamples.toShort(),
            delay.toShort()
        )
    }

    /**
     * core process of AECM instance, must called on a prepared AECM instance. we only support 80 or 160 sample blocks
     * of data.
     *
     * @param nearendNoisy
     * - In buffer containing one frame of reference nearend+echo signal. If noise reduction is active,
     * provide the noisy signal here.
     * @param numOfSamples
     * - Number of samples in nearend buffer. Must be <= Short.MAX_VALUE and >= Short.MIN_VALUE
     * @param delay
     * - Delay estimate for sound card and system buffers <br></br>
     * delay = (t_render - t_analyze) + (t_process - t_capture)<br></br>
     * where<br></br>
     * - t_analyze is the time a frame is passed to farendBuffer() and t_render is the time the first sample
     * of the same frame is rendered by the audio hardware.<br></br>
     * - t_capture is the time the first sample of a frame is captured by the audio hardware and t_process is
     * the time the same frame is passed to echoCancellation(). Must be <= Short.MAX_VALUE and >= Short.MIN_VALUE
     *
     * @return one processed frame without echo or null if echoCancellation() is called on an unprepared AECM instance
     * or you pass an invalid parameter.
     */
    fun echoCancellation(nearendNoisy: ShortArray, numOfSamples: Short, delay: Short): ShortArray? {
        return echoCancellation(nearendNoisy, null, numOfSamples, delay)
    }

    fun setAecmMode(mode: AggressiveMode?): AEC {
        return setAecmMode(mode, true)
    }

    /**
     * Set the aggressiveness mode of AECM instance, more higher the mode is, more aggressive the instance will be.
     * @param prepare - is flag that indicate will or will not prepare AECM instance after aggressiveness mode was set.
     * @param mode
     * @return the [AEC] object itself or null if mode is null.
     */
    private fun setAecmMode(mode: AggressiveMode?, prepare: Boolean): AEC {
        // check the mode argument.
        var mode = mode
        if (mode == null) {
            Log.d(TAG, "setAecMode() mode == null, AggressiveMode.AGGRESSIVE will be used instead")
            mode = AggressiveMode.AGGRESSIVE
        }
        mAecmConfig!!.mAecmMode = mode.mode.toShort()
        return if (prepare) prepare() else this
    }

    /**
     * When finished the pre-works or any settings are changed, call this to make AECM instance prepared. Otherwise your
     * new settings will be ignored by the AECM instance.
     *
     * @return the [AEC] object itself.
     */
    fun prepare(): AEC {
        if (mIsInit) {
            close()
            mAecmHandler = nativeCreateAecmInstance()
        }
        mInitAecmInstance(mSampFreq!!.fS, mAecmConfig!!.mAecmMode)
        mIsInit = true

        // set AecConfig to native side.
        nativeSetConfig(mAecmHandler, mAecmConfig)
        Log.d(
            TAG,
            "AECM instance successfully prepared with sampling frequency: " + mSampFreq!!.fS + "hz " + "and aggressiveness mode: " + mAecmConfig!!.mAecmMode
        )
        return this
    }

    /**
     * Release the resources in AECM instance and the AECM instance is no longer available until next **prepare()**
     * is called.<br></br>
     * You should **always** call this **manually** when all things are done.
     */
    fun close() {
        if (mIsInit) {
            nativeFreeAecmInstance(mAecmHandler)
            mAecmHandler = -1
            mIsInit = false
        }
    }

    // ////////////////////////////////////////////////////////
    // PROTECTED METHODS
    @kotlin.Throws(Throwable::class)
    protected fun finalize() {
        if (mIsInit) {
            close()
        }
    }
    // ////////////////////////////////////////////////////////
    // PRIVATE METHODS
    /**
     * initialize the AECM instance
     *
     * @param sampFreq
     * @param mAecmMode
     */
    private fun mInitAecmInstance(sampFreq: Int, mAecmMode: Short) {
        if (!mIsInit) {
            nativeInitializeAecmInstance(mAecmHandler, sampFreq)

            // initialize configurations of AECM instance.
            mAecmConfig = AecmConfig()
            mAecmConfig!!.mAecmMode = mAecmMode

            // set default configuration of AECM instance
            nativeSetConfig(mAecmHandler, mAecmConfig)
            mIsInit = true
        }
    }
    // ////////////////////////////////////////////////////////
    // PRIVATE NESTED CLASSES
    /**
     * Acoustic Echo Cancellation for Mobile Configuration class, holds the config Info. of AECM instance.<br></br>
     * [NOTE] **DO NOT** modify the name of members, or you must change the native code to match your modifying.
     * Otherwise the native code could not find pre-binding members name.<br></br>
     *
     */
    @Suppress("unused")
    inner class AecmConfig {
        var mAecmMode =
            AggressiveMode.AGGRESSIVE.mode.toShort() // default AggressiveMode.AGGRESSIVE
        private val mCngMode = AECM_ENABLE // AECM_UNABLE, AECM_ENABLE (default)
    }

        private val TAG = "AECM_LOG"

        init {
            try {
                System.loadLibrary("AEC")
            } catch (e: Exception) {
                Log.d(
                    TAG,
                    "Can't load AECM library: $e"
                )
            }
        }
        // /////////////////////////////////////////////////////////
        // PUBLIC CONSTANTS
        /**
         * constant unable mode for Aecm configuration settings.
         */
        val AECM_UNABLE: Short = 0

        /**
         * constant enable mode for Aecm configuration settings.
         */
        val AECM_ENABLE: Short = 1
        // ///////////////////////////////////////////
        // PRIVATE NATIVE INTERFACES
        /**
         * Allocates the memory needed by the AECM. The memory needs to be initialized separately using the
         * nativeInitializeAecmInstance() method.
         *
         * @return -1: error<br></br>
         * other values: created AECM instance handler.
         */
        private external fun nativeCreateAecmInstance(): Long

        /**
         * Release the memory allocated by nativeCreateAecmInstance().
         *
         * @param aecmHandler
         * - handler of the AECM instance created by nativeCreateAecmInstance()
         * @return 0: OK<br></br>
         * -1: error
         */
        private external fun nativeFreeAecmInstance(aecmHandler: Long): Int

        /**
         * Initializes an AECM instance.
         *
         * @param aecmHandler
         * - Handler of AECM instance
         * @param samplingFrequency
         * - Sampling frequency of data
         * @return: 0: OK<br></br>
         * -1: error
         */
        private external fun nativeInitializeAecmInstance(
            aecmHandler: Long,
            samplingFrequency: Int
        ): Int

        /**
         * Inserts an 80 or 160 sample block of data into the farend buffer.
         *
         * @param aecmHandler
         * - Handler to the AECM instance
         * @param farend
         * - In buffer containing one frame of farend signal for L band
         * @param nrOfSamples
         * - Number of samples in farend buffer
         * @return: 0: OK<br></br>
         * -1: error
         */
        private external fun nativeBufferFarend(
            aecmHandler: Long,
            farend: ShortArray,
            nrOfSamples: Int
        ): Int

        /**
         * Runs the AECM on an 80 or 160 sample blocks of data.
         *
         * @param aecmHandler
         * - Handler to the AECM handler
         * @param nearendNoisy
         * - In buffer containing one frame of reference nearend+echo signal. If noise reduction is active,
         * provide the noisy signal here.
         * @param nearendClean
         * - In buffer containing one frame of nearend+echo signal. If noise reduction is active, provide the
         * clean signal here.Otherwise pass a NULL pointer.
         * @param nrOfSamples
         * - Number of samples in nearend buffer
         * @param msInSndCardBuf
         * - Delay estimate for sound card and system buffers <br></br>
         * @return short array: OK: processed shorts<br></br>
         * null: error
         */
        private external fun nativeAecmProcess(
            aecmHandler: Long,
            nearendNoisy: ShortArray,
            nearendClean: ShortArray?,
            nrOfSamples: Short,
            msInSndCardBuf: Short
        ): ShortArray?

        /**
         * Enables the user to set certain parameters on-the-fly.
         *
         * @param aecmHandler
         * - Handler to the AECM instance
         * @param aecmConfig
         * - the new configuration of AECM instance to set.
         *
         * @return 0: OK<br></br>
         * -1: error
         */
        private external fun nativeSetConfig(aecmHandler: Long, aecmConfig: AecmConfig?): Int
}
