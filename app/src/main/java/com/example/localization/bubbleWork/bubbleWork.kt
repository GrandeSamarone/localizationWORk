package com.example.localization.bubbleWork

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import com.example.localization.MainActivity
import com.example.localization.R
import java.util.Calendar


class BubbleWork{
    private lateinit var windowManager: WindowManager
    private  var floatingBubble: View? =null
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var height:Int=0
    private var width:Int=0
    private var initialTouchX: Float = 0.toFloat()
    private var initialTouchY: Float = 0.toFloat()
    private lateinit var params : WindowManager.LayoutParams
    private lateinit var imageparams  : WindowManager.LayoutParams
    private lateinit var inflater:LayoutInflater
    private lateinit var context: Context
    private lateinit var imageViewClose:ImageView
    private var maxClickDuration:Int = 300
    private var  startclickTime:Long = 0
    private var running:Boolean=false

    @Suppress("InflateParams")
    fun start(appcontext: Context, visibleBubble:Boolean,runBubble:Boolean) {

        running=runBubble
         context = appcontext
         inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

          if(floatingBubble==null){
              loadFloatingBubble()
          }else floatingBubble?.isVisible = visibleBubble


    }
    private fun upBubble(event: MotionEvent): Boolean {
        val diffPosicaoX = (event.rawX - initialTouchX).toInt()
        val diffPosicaoY = (event.rawY - initialTouchY).toInt()

        val clickDuration: Long = Calendar.getInstance().timeInMillis - startclickTime
        imageViewClose.visibility = View.INVISIBLE

        if (clickDuration<maxClickDuration) {
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        return true
    }
    private fun moveBubble(event: MotionEvent): Boolean {
        params.x = initialX + (event.rawX - initialTouchX).toInt()
        params.y = initialY + (event.rawY - initialTouchY).toInt()

        windowManager.updateViewLayout(floatingBubble, params)

        /// verifica se o work ainda esta funcionando para mostrar ou nao o icon bottom
        runIconClose()
        return true
    }

    private fun downBubble(event: MotionEvent): Boolean {

        startclickTime= Calendar.getInstance().timeInMillis
        initialX = params.x
        initialY = params.y
        initialTouchX = (event.rawX)
        initialTouchY = (event.rawY)
        return true
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun loadFloatingBubble(){

        floatingBubble = inflater.inflate(R.layout.bubble_widget_layout,null)

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O) {
            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

             imageparams = WindowManager.LayoutParams(
                140,
                140,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }
             ///icon
            params.gravity = Gravity.CENTER or Gravity.CENTER
            params.x = 0
            params.y = 100

             ///close
           imageparams.gravity = Gravity.BOTTOM or Gravity.CENTER
           imageparams.y = 100

        imageViewClose= ImageView(context)
        imageViewClose.setImageResource(R.drawable.cancel_white)
        imageViewClose.visibility = View.INVISIBLE


        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingBubble, params)
        windowManager.addView(imageViewClose, imageparams)

        //Tamanho da tela
        height=getDisplayMetrics(context).heightPixels
        width =getDisplayMetrics(context).widthPixels




        val btnBubble: Button? =floatingBubble?.findViewById(R.id.bubbleFloatingButton)



        btnBubble?.setOnClickListener {
            floatingBubble?.background?.setColorFilter(Color.parseColor("#4e1a1a"), PorterDuff.Mode.SRC_ATOP)
        }
        btnBubble?.setOnTouchListener { view, event ->
            view.performClick()
            when (event.action) {
                MotionEvent.ACTION_DOWN ->  downBubble(event)

                MotionEvent.ACTION_MOVE ->  moveBubble(event)

                MotionEvent.ACTION_UP   ->  upBubble(event)


                else ->  false
            }
        }
    }

    private fun getDisplayMetrics(context: Context): DisplayMetrics {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val defaultDisplayContext = context.createDisplayContext(defaultDisplay)
        return defaultDisplayContext.resources.displayMetrics
    }


private fun runIconClose(){
    if(!running){
        imageViewClose.visibility = View.VISIBLE
        if(params.y>(height*0.2)){
            imageViewClose.setImageResource(R.drawable.cancel_orange)
            if(params.y>(height*0.4) && floatingBubble?.isVisible == true){
                    windowManager.removeView(floatingBubble)
                    windowManager.removeView(imageViewClose)
            }

        }else{
            imageViewClose.setImageResource(R.drawable.cancel_white)
        }
    }
}

}