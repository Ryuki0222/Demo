package com.example.ryukisakuma.demo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.AttributeSet
import android.util.Size
import android.view.Surface
import android.view.TextureView
import java.util.*

class FilterCameraView : TextureView {
    private val baseActivity : Activity
    private var ratioWidth = 0
    private var ratioHeight = 0
    private var captureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private var previewSize: Size = Size(1000, 1000)
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private var imageReader: ImageReader? = null
    private lateinit var previewRequest: CaptureRequest
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    constructor(context: Context, attrs: AttributeSet, defstyle: Int): super(context, attrs, defstyle) {
        baseActivity = context as Activity
    }
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs!!, 0) {

    }
    constructor(context: Context) : this(context, null){

    }

    fun setAspectRatio(width: Int, height: Int) {
        if(width < 0 || height < 0) {
            return
        }

        ratioWidth = width
        ratioHeight = height
        //サイズを更新してViewを指定する
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height)
        }
        else {
            if(width < height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth)
            }
            else {
                setMeasuredDimension(width, width * ratioWidth / ratioHeight)
            }
        }
        //カメラを起動させていく
        this.surfaceTextureListener = surfaceFiiterCameraViewListener
        startBackgroundThread()
    }

    val surfaceFiiterCameraViewListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2)
            openCamera()
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return false
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            this@FilterCameraView.cameraDevice = camera
            try {
                cameraCreatePreviewSession()
            }
            catch (e: CameraAccessException) {

            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            this@FilterCameraView.cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            onDisconnected(camera)
        }
    }

    fun cameraCreatePreviewSession() {
        try {
            val texture = this.surfaceTexture
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)
            val surface = Surface(texture)
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)
            cameraDevice!!.createCaptureSession(Arrays.asList(surface, imageReader?.surface),
                @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        captureSession = session
                        try {
                            previewRequestBuilder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            previewRequest = previewRequestBuilder.build()
                            val handler = Handler(backgroundThread!!.looper)
                            captureSession!!.setRepeatingRequest(previewRequest, null, handler)
                        }
                        catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {

                    }
                }, null)
        }
        catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun openCamera () {
        //get manager
        val manager = this.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = manager.cameraIdList[0]
            //get most graphics of camera
            val cameraCharacteristics = manager.getCameraCharacteristics(cameraId)
            val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            previewSize = streamConfigurationMap!!.getOutputSizes(SurfaceTexture::class.java)[0]

            val permission = ContextCompat.checkSelfPermission(this.context, Manifest.permission.CAMERA)

            if (permission != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission()
                return
            }
            manager.openCamera(cameraId, stateCallback, null)
        }
        catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun requestCameraPermission () {
        if(this.baseActivity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            AlertDialog.Builder(this.baseActivity.baseContext)
                .setMessage("Permissoin Here")
                .setPositiveButton(android.R.string.ok) {_, _ ->
                    this.baseActivity.requestPermissions(arrayOf(Manifest.permission.CAMERA),200)
                }
                .setNegativeButton(android.R.string.cancel) {_, _ ->
                    this.baseActivity.finish()
                }
                .create()
        }
        else {
            this.baseActivity.requestPermissions(arrayOf(Manifest.permission.CAMERA), 200)
        }
    }

    private fun startBackgroundThread () {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }


}