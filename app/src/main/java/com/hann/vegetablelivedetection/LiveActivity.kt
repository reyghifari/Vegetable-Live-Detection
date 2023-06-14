package com.hann.vegetablelivedetection

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.Toast
import com.hann.vegetablelivedetection.DetailResultActivity.Companion.EXTRA_RESULT
import com.hann.vegetablelivedetection.databinding.ActivityLiveBinding
import com.hann.vegetablelivedetection.ml.ModelFinalBangkit
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class LiveActivity : AppCompatActivity() {

    lateinit var labels:List<String>
    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)
    val paint = Paint()
    lateinit var imageProcessor: ImageProcessor
    lateinit var bitmap: Bitmap
    lateinit var imageView: ImageView
    lateinit var cameraDevice: CameraDevice
    lateinit var handler: Handler
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var model: ModelFinalBangkit
    private var resultDetect : String = ""
    private lateinit var binding: ActivityLiveBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startDetection()
        binding.resultDetail.setOnClickListener {
            if (resultDetect.isNotBlank()){
                val intent = Intent(this, DetailResultActivity::class.java)
                intent.putExtra(EXTRA_RESULT, resultDetect)
                startActivity(intent)
            }else{
                Toast.makeText(this, "Arahkan object yang ingin dicheck", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startDetection() {
        labels = FileUtil.loadLabels(this, "labellive.txt")
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(320, 320, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = ModelFinalBangkit.newInstance(this)
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        imageView = findViewById(R.id.imageView)

        textureView = findViewById(R.id.textureView)
        textureView.surfaceTextureListener = object:TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                open_camera()
            }
            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                bitmap = textureView.bitmap!!
                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)

                val outputs = model.process(image)

                val locations = outputs.locationAsTensorBuffer.floatArray;
                val category = outputs.categoryAsTensorBuffer.floatArray;
                val score = outputs.scoreAsTensorBuffer.floatArray;

                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutable)


                val h = mutable.height
                val w = mutable.width
                paint.textSize = h/25f
                paint.strokeWidth = h/85f
                var x = 0
                score.forEachIndexed { index, fl ->
                    x = index
                    x *= 4
                    if(fl > 0.5){
                        paint.setColor(colors.get(index))
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(
                            RectF(locations.get(x+1)*w, locations.get(x)*h,
                                locations.get(x+3)*w, locations.get(x+2)*h), paint)
                        paint.style = Paint.Style.FILL
                        canvas.drawText(labels[category[index].toInt()] +" "+fl.toString(), locations.get(x+1)*w, locations.get(x)*h, paint)
                        resultDetect = labels[category[index].toInt()]
                    }
                }

                imageView.setImageBitmap(mutable)
            }
        }
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }


    @SuppressLint("MissingPermission")
    fun open_camera(){
        cameraManager.openCamera(cameraManager.cameraIdList[0], object:CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0

                var surfaceTexture = textureView.surfaceTexture
                var surface = Surface(surfaceTexture)

                var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        p0.setRepeatingRequest(captureRequest.build(), null, null)
                    }
                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                    }
                }, handler)
            }

            override fun onDisconnected(p0: CameraDevice) {

            }

            override fun onError(p0: CameraDevice, p1: Int) {

            }
        }, handler)
    }
}