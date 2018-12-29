package pl.adriandefus.webrtctest

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import org.webrtc.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WEB_RTC"
    }

    private var isCapturing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initWebRTC()
    }

    private fun initWebRTC() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions()
        )

        val peerConnectionFactory = PeerConnectionFactory.builder()
            .createPeerConnectionFactory()

        val options = PeerConnectionFactory.Options()

        val videoCapturer = createVideoCapturer()
        val mediaConstraints = MediaConstraints()


        //video source
        val videoSource = peerConnectionFactory.createVideoSource(true)
        val localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource)

        //audio source
        val audioSource = peerConnectionFactory.createAudioSource(mediaConstraints)
        val localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource)

        //init capturing

        videoCapturer?.let { capturer ->
            surfaceView.setMirror(true)

            val rootEglBase = EglBase.create()

            surfaceView.init(rootEglBase.eglBaseContext, null)
            capturer.initialize(
                SurfaceTextureHelper.create("foo", rootEglBase.eglBaseContext),
                this,
                capturerObserver
            )

            toggleCaptureBtn.setOnClickListener {
                if (isCapturing) {
                    isCapturing = false
                    capturer.stopCapture()
                    toggleCaptureBtn.text = "start capturing"
                } else {
                    isCapturing = true
                    capturer.startCapture(1000, 1000, 60)
                    toggleCaptureBtn.text = "stop capturing"
                }
            }
        }

    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera1Enumerator()

        return createCameraCapturer(enumerator)
    }

    private fun createCameraCapturer(enumerator: Camera1Enumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames

        deviceNames.forEach {
            if (enumerator.isFrontFacing(it)) {
                val capturer = enumerator.createCapturer(it, null)

                capturer?.let {
                    return capturer
                }
            }
        }

        deviceNames.forEach {
            if (!enumerator.isFrontFacing(it)) {
                val capturer = enumerator.createCapturer(it, null)

                capturer?.let {
                    return capturer
                }
            }
        }

        return null
    }

    private val capturerObserver = object : CapturerObserver {
        override fun onCapturerStopped() {
            surfaceView.clearImage()
            Log.d(TAG, "onCapturerStopped")
        }

        override fun onCapturerStarted(p0: Boolean) {
            Log.d(TAG, "onCapturerStarted")
        }

        override fun onFrameCaptured(p0: VideoFrame?) {
            p0?.let {
                surfaceView.onFrame(it)
                Log.d(TAG, "onFrameCaptured: (width: ${it.buffer.width}, height-${it.buffer.height})")
            }
        }
    }
}
