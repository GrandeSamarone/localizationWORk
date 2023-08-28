package com.example.localization.bubbleWork

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat.startActivity
import com.example.localization.MainActivity
import com.example.localization.R

class BubbleWork {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingBubble: View
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0.toFloat()
    private var initialTouchY: Float = 0.toFloat()
    private lateinit var params : WindowManager.LayoutParams
    private lateinit var inflater:LayoutInflater
    private lateinit var context: Context



    fun Start(appcontext: Context) {
         context = appcontext
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        loadFloatingBubble(inflater)
    }

    private fun openApp(event: MotionEvent): Boolean {
        val diffPosicaoX = (event.rawX - initialTouchX).toInt()
        val diffPosicaoY = (event.rawY - initialTouchY).toInt()

        val singleClick: Boolean = diffPosicaoX < 5 && diffPosicaoY < 5

        if (singleClick) {
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
        return true
    }

    private fun storeTouchs(event: MotionEvent): Boolean {
        initialX = params.x
        initialY = params.y
        initialTouchX = (event.rawX)
        initialTouchY = (event.rawY)
        return true
    }


    private fun loadFloatingBubble(inflater: LayoutInflater){
        floatingBubble = inflater.inflate(R.layout.bubble_widget_layout, null)

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O) {
            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }else{
            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }
        //Specify the view position
        params.gravity = Gravity.NO_GRAVITY
        params.x = 0
        params.y = 50

        //Add the view to the window
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingBubble, params)

//        val closeButtonCollapsed = floatingBubble.close_btn
//        closeButtonCollapsed.setOnClickListener {
//            stopSelf()
//        }

        val bubbleView: View = floatingBubble.findViewById(R.id.bolha_layout)
        bubbleView.setOnTouchListener { view, event ->
            view.performClick()
            when (event.action) {
                MotionEvent.ACTION_DOWN ->  storeTouchs(event)

                MotionEvent.ACTION_MOVE ->  moveBubble(event)

                MotionEvent.ACTION_UP   ->  openApp(event)

                else ->  false
            }
        }
    }



}