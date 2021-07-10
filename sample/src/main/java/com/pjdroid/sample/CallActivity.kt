package com.pjdroid.sample

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.pjsip.pjsua2.*

internal class VideoPreviewHandler : SurfaceHolder.Callback {
    var videoPreviewActive = false
    fun updateVideoPreview(holder: SurfaceHolder) {
        if (MainActivity.currentCall != null && MainActivity.currentCall?.vidWin != null && MainActivity.currentCall?.vidPrev != null) {
            if (videoPreviewActive) {
                val vidWH = VideoWindowHandle()
                vidWH.handle.setWindow(holder.surface)
                val vidPrevParam = VideoPreviewOpParam()
                vidPrevParam.window = vidWH
                try {
                    MainActivity.currentCall?.vidPrev?.start(vidPrevParam)
                } catch (e: Exception) {
                    println(e)
                }
            } else {
                try {
                    MainActivity.currentCall?.vidPrev?.stop()
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        updateVideoPreview(holder)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        try {
            MainActivity.currentCall?.vidPrev?.stop()
        } catch (e: Exception) {
            println(e)
        }
    }
}

class CallActivity : AppCompatActivity(), Handler.Callback, SurfaceHolder.Callback {
    private val handler = Handler(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        val surfaceInVideo = findViewById<View>(R.id.surfaceIncomingVideo) as SurfaceView
        val surfacePreview = findViewById<View>(R.id.surfacePreviewCapture) as SurfaceView
        val buttonShowPreview = findViewById<View>(R.id.buttonShowPreview) as Button
        if (MainActivity.currentCall == null ||
            MainActivity.currentCall?.vidWin == null
        ) {
            surfaceInVideo.visibility = View.GONE
            buttonShowPreview.visibility = View.GONE
        }
        setupVideoPreview(surfacePreview, buttonShowPreview)
        surfaceInVideo.holder.addCallback(this)
        surfacePreview.holder.addCallback(previewHandler)
        handler_ = handler
        MainActivity.currentCall?.let {
            try {
                lastCallInfo = it.info
                updateCallState(lastCallInfo)
            } catch (e: Exception) {
                println(e)
            }
        }?:apply {
            updateCallState(lastCallInfo)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val display: Display
        val orient: Int
        val wm: WindowManager = this.getSystemService(WINDOW_SERVICE) as WindowManager
        display = wm.defaultDisplay
        val rotation: Int = display.rotation
        println("Device orientation changed: $rotation")
        orient = when (rotation) {
            Surface.ROTATION_0 -> pjmedia_orient.PJMEDIA_ORIENT_ROTATE_270DEG
            Surface.ROTATION_90 -> pjmedia_orient.PJMEDIA_ORIENT_NATURAL
            Surface.ROTATION_180 -> pjmedia_orient.PJMEDIA_ORIENT_ROTATE_90DEG
            Surface.ROTATION_270 -> pjmedia_orient.PJMEDIA_ORIENT_ROTATE_180DEG
            else -> pjmedia_orient.PJMEDIA_ORIENT_UNKNOWN
        }
        MainActivity.account?.let { acc ->
            try {
                val cfg = acc.cfg
                val cap_dev = cfg.videoConfig.defaultCaptureDevice
                MyApp.ep?.vidDevManager()?.setCaptureOrient(
                    cap_dev, orient,
                    true
                )
            } catch (e: Exception) {
                println(e)
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        handler_ = null
    }

    private fun updateVideoWindow(show: Boolean) {
        if (MainActivity.currentCall != null && MainActivity.currentCall?.vidWin != null && MainActivity.currentCall?.vidPrev != null) {
            val surfaceInVideo = findViewById<View>(R.id.surfaceIncomingVideo) as SurfaceView
            val vidWH = VideoWindowHandle()
            if (show) {
                vidWH.handle.setWindow(
                    surfaceInVideo.holder.surface
                )
            } else {
                vidWH.handle.setWindow(null)
            }
            try {
                MainActivity.currentCall?.vidWin?.setWindow(vidWH)
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        updateVideoWindow(true)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        updateVideoWindow(false)
    }

    fun acceptCall(view: View) {
        val prm = CallOpParam()
        prm.statusCode = pjsip_status_code.PJSIP_SC_OK
        try {
            MainActivity.currentCall?.answer(prm)
        } catch (e: Exception) {
            println(e)
        }
        view.visibility = View.GONE
    }

    fun hangupCall(view: View?) {
        handler_ = null
        finish()
        if (MainActivity.currentCall != null) {
            val prm = CallOpParam()
            prm.statusCode = pjsip_status_code.PJSIP_SC_DECLINE
            try {
                MainActivity.currentCall?.hangup(prm)
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    fun setupVideoPreview(
        surfacePreview: SurfaceView,
        buttonShowPreview: Button
    ) {
        surfacePreview.visibility =
            if (previewHandler.videoPreviewActive) View.VISIBLE else View.GONE
        buttonShowPreview.text =
            if (previewHandler.videoPreviewActive) getString(R.string.hide_preview) else getString(
                R.string.show_preview
            )
    }

    fun showPreview(view: View?) {
        val surfacePreview = findViewById<View>(R.id.surfacePreviewCapture) as SurfaceView
        val buttonShowPreview = findViewById<View>(R.id.buttonShowPreview) as Button
        previewHandler.videoPreviewActive = !previewHandler.videoPreviewActive
        setupVideoPreview(surfacePreview, buttonShowPreview)
        previewHandler.updateVideoPreview(surfacePreview.holder)
    }

    private fun setupVideoSurface() {
        val surfaceInVideo = findViewById<View>(R.id.surfaceIncomingVideo) as SurfaceView
        val surfacePreview = findViewById<View>(R.id.surfacePreviewCapture) as SurfaceView
        val buttonShowPreview = findViewById<View>(R.id.buttonShowPreview) as Button
        surfaceInVideo.visibility = View.VISIBLE
        buttonShowPreview.visibility = View.VISIBLE
        surfacePreview.visibility = View.GONE
    }

    override fun handleMessage(m: Message): Boolean {
        if (m.what == MainActivity.MSG_TYPE.CALL_STATE) {
            lastCallInfo = m.obj as CallInfo
            updateCallState(lastCallInfo)
        } else if (m.what == MainActivity.MSG_TYPE.CALL_MEDIA_STATE) {
            if (MainActivity.currentCall?.vidWin != null) {
                /* Set capture orientation according to current
                 * device orientation.
                 */
                onConfigurationChanged(resources.configuration)
                /* If there's incoming video, display it. */setupVideoSurface()
            }
        } else {

            /* Message not handled */
            return false
        }
        return true
    }

    private fun updateCallState(ci: CallInfo?) {
        val tvPeer = findViewById<View>(R.id.textViewPeer) as TextView
        val tvState = findViewById<View>(R.id.textViewCallState) as TextView
        val buttonHangup = findViewById<View>(R.id.buttonHangup) as Button
        val buttonAccept = findViewById<View>(R.id.buttonAccept) as Button
        var call_state: String? = ""
        if (ci == null) {
            buttonAccept.visibility = View.GONE
            buttonHangup.text = "OK"
            tvState.text = "Call disconnected"
            return
        }
        if (ci.role == pjsip_role_e.PJSIP_ROLE_UAC) {
            buttonAccept.visibility = View.GONE
        }
        if (ci.state <
            pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED
        ) {
            if (ci.role == pjsip_role_e.PJSIP_ROLE_UAS) {
                call_state = "Incoming call.."
                /* Default button texts are already 'Accept' & 'Reject' */
            } else {
                buttonHangup.text = "Cancel"
                call_state = ci.stateText
            }
        } else if (ci.state >=
            pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED
        ) {
            buttonAccept.visibility = View.GONE
            call_state = ci.stateText
            if (ci.state == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) {
                buttonHangup.text = "Hangup"
            } else if (ci.state ==
                pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED
            ) {
                buttonHangup.text = "OK"
                call_state = "Call disconnected: " + ci.lastReason
            }
        }
        tvPeer.text = ci.remoteUri
        tvState.text = call_state
    }

    companion object {
        @JvmField
        var handler_: Handler? = null
        private val previewHandler = VideoPreviewHandler()
        private var lastCallInfo: CallInfo? = null
    }
}