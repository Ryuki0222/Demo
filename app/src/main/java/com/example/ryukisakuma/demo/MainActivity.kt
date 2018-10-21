package com.example.ryukisakuma.demo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import jp.co.cyberagent.android.gpuimage.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(),ActivityCompat.OnRequestPermissionsResultCallback {

    private var captureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private var previewSize: Size = Size(1000, 1000)
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private var imageReader: ImageReader? = null
    private lateinit var previewRequest: CaptureRequest
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private lateinit var baseBitmap : Bitmap
    private  var filteredgpuImage : GPUImage? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Toast.makeText(this, android.R.string.ok.toString(), Toast.LENGTH_SHORT).show()

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        _textureView.surfaceTextureListener = surfaceTextureListener
        startBackgroundThread()

    }

    fun openCamera() {
        //マネージャーの取得
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            val cameraId = manager.cameraIdList[0]
            //最高解像度を取得
            val cameraCharacteristics = manager.getCameraCharacteristics(cameraId)
            val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            previewSize = streamConfigurationMap!!.getOutputSizes(SurfaceTexture::class.java)[0]

            val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

            if(permission != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission()
                return
            }
            manager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            this@MainActivity.cameraDevice = camera
            try {
                createCameraPreviewSession()
            }
            catch (e: CameraAccessException) {
                Log.e("erts", e.toString())
            }
        }

        override fun onError(camera: CameraDevice, error: Int) {
            onDisconnected(camera)
            finish()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            this@MainActivity.cameraDevice = null
        }
    }

    private fun createCameraPreviewSession () {
        try {
            val texture = _textureView.surfaceTexture
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
                            captureSession?.setRepeatingRequest(previewRequest, null, Handler(backgroundThread?.looper))
                        } catch (e: CameraAccessException) {
                            Log.e("erfs", e.toString())
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {

                    }
                }, null)
        }
        catch (e: CameraAccessException) {
            Log.e("erfs", e.toString())
        }
    }

    fun requestCameraPermission () {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            AlertDialog.Builder(baseContext)
                .setMessage("Permission Here")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    requestPermissions(arrayOf(Manifest.permission.CAMERA),
                        200)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    finish()
                }
                .create()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 200)
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            imageReader = ImageReader.newInstance(width, height,
                ImageFormat.JPEG, /*maxImages*/ 2)
            openCamera()

        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

        }
        //フレームごとにアップデートする
        //ここにフィルターを記述
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            baseBitmap = _textureView.bitmap
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return false
        }
    }

    fun _textureView_onClick(view : View) {

        val bitmap = baseBitmap

        val filter_blend = GPUImageNormalBlendFilter()
        val filter_gaussian = GPUImageGaussianBlurFilter()

       val b1 = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
       val canvas = Canvas(b1)
       val p = Paint()
       p.color = 0x30AFAFAF
       canvas.drawRect(0f, 0f, 1f, 1f, p)
       filter_blend.bitmap = b1
       val gpuImage = GPUImage(this)
       gpuImage.setImage(bitmap)
       gpuImage.setFilter(filter_blend)
       gpuImage.setImage(gpuImage.bitmapWithFilterApplied)
       gpuImage.setFilter(filter_gaussian)
       filter_gaussian.setBlurSize(50f)
       gpuImage.setImage(gpuImage.bitmapWithFilterApplied)
       filter_gaussian.setBlurSize(5f)
       gpuImage.setImage(gpuImage.bitmapWithFilterApplied)
       filter_gaussian.setBlurSize(1f)
       filteredgpuImage = gpuImage
        val fitered = gpuImage.bitmapWithFilterApplied //Bitmap


    }
}
