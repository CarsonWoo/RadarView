package dev.carson.radarview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.graphics.contains
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toPoint
import androidx.core.graphics.toRectF
import androidx.core.graphics.withScale
import java.lang.Math.pow
import kotlin.math.*
import kotlin.random.Random

class RadarView : View {

    enum class TYPE { RANDOM, FIRST, SECOND, THIRD, FOURTH }

    private val mPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    private val mSweepPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    private val mOutlinePaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    private val mTargetPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    private val mDefaultSize = 120// px

    // limit the size of bitmap
    private var mBitmapMaxSize = 0F

    private var mBitmapWHRatio = 0F

    private val mScaleFactor = 30F

    private var mStartAngle = 0F
    private val mSweepAngle = -60F

    private var mScanColors: IntArray? = null

    private val mDefaultScanColors = intArrayOf(Color.parseColor("#0F7F7F7F"),
        Color.parseColor("#7F7F7F7F"),
        Color.parseColor("#857F7F7F"))

    private val mDefaultBackgroundColor = Color.WHITE

    private var mBackgroundColor: Int = mDefaultBackgroundColor

    private var mBorderColor: Int = Color.BLACK

    private var mBorderWidth = 0F

    private var mTargetColor: Int = Color.RED

    private var mTargetRadius = 10F

    private lateinit var mOutlineRect: Rect

    private val mAnimator = ValueAnimator.ofFloat(0F, 360F)

    private var mTargetList: ArrayList<PointF>? = null

    private var mIsAnimating = false

    private var mNeedBitmap = false

    private lateinit var mBitmap: Bitmap

    constructor(context: Context): this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)

    init {
        mPaint.color = Color.GRAY
        mPaint.strokeWidth = 10F
        mPaint.style = Paint.Style.FILL_AND_STROKE
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.strokeCap = Paint.Cap.ROUND

        mSweepPaint.style = Paint.Style.FILL

        mOutlinePaint.style = Paint.Style.FILL_AND_STROKE
        mOutlinePaint.color = mBackgroundColor

        mTargetPaint.style = Paint.Style.FILL
        mTargetPaint.color = mTargetColor
        mTargetPaint.strokeWidth = 10F
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val vWidth = measureDimension(widthMeasureSpec)
        val vHeight = measureDimension(heightMeasureSpec)
        val size = min(vWidth, vHeight)

        setShader(size)

        setMeasuredDimension(size, size)

        setParamUpdate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // 设置默认居中
        var l = left
        var r = right
        var t = top
        var b = bottom
        when {
            width > height -> {
                // 宽度比高度大 那么要设置默认居中就得把left往右移 right往左移
                l = (width - measuredWidth) / 2
                r = width - l
                layout(l, t, r, b)
            }
            height > width -> {
                // 高度比宽度大 那么要设置默认居中就得把top往下移 bottom往上移
                t = (height - measuredHeight) / 2
                b = height - t
                layout(l, t, r, b)
            }
            else -> super.onLayout(changed, left, top, right, bottom)
        }
    }

    private fun setShader(size: Int) {
        val shader = SweepGradient(size.toFloat() / 2, size.toFloat() / 2,
            mScanColors?: mDefaultScanColors,
            floatArrayOf(0F, 0.5F, 1F))
        val matrix = Matrix()
        matrix.setRotate(-90F, size.toFloat() / 2, size.toFloat() / 2)
        shader.setLocalMatrix(matrix)
        mSweepPaint.shader = shader
    }

    fun setScanColors(colors: IntArray) {
        this.mScanColors = colors
        setShader(measuredWidth)
        invalidate()
    }

    fun setRadarColor(@ColorInt color: Int) {
        this.mBackgroundColor = color
        this.mOutlinePaint.color = color
        invalidate()
    }

    fun setRadarColor(colorString: String) {
        if (!colorString.startsWith("#") || colorString.length != 7 || colorString.length != 9) {
            Log.e("RadarView", "colorString parse error, please check your enter param")
            return
        }
        val color = Color.parseColor(colorString)
        setRadarColor(color)
    }

    fun setBorderColor(@ColorInt color: Int) {
        this.mBorderColor = color
        invalidate()
    }

    fun setBorderColor(colorString: String) {
        if (!colorString.startsWith("#") || colorString.length != 7 || colorString.length != 9) {
            Log.e("RadarView", "colorString parse error, please check your enter param")
            return
        }
        val color = Color.parseColor(colorString)
        setBorderColor(color)
    }

    fun setRadarGradientColor(colors: IntArray) {
        val shader = SweepGradient(measuredWidth.toFloat() / 2,
            measuredHeight.toFloat() / 2, colors, null)
        mOutlinePaint.shader = shader
        invalidate()
    }

    fun setBorderWidth(width: Float) {
        this.mBorderWidth = width
        invalidate()
    }

    private fun setParamUpdate() {
        mOutlineRect = Rect(0, 0, measuredWidth, measuredHeight)

        mBitmapMaxSize = measuredWidth.toFloat() / mScaleFactor
    }

    private fun measureDimension(spec: Int) =  when (MeasureSpec.getMode(spec)) {
        MeasureSpec.EXACTLY -> {
            // exactly number or match_parent
            MeasureSpec.getSize(spec)
        }
        MeasureSpec.AT_MOST -> {
            // wrap_content
            min(mDefaultSize, MeasureSpec.getSize(spec))
        }
        else -> {
            mDefaultSize
        }
    }

    override fun setBackground(background: Drawable?) {
        // 取消传统背景设置
//        super.setBackground(background)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        // draw outside circle (background)
        canvas?.drawCircle(measuredWidth.toFloat() / 2, measuredHeight.toFloat() / 2, measuredWidth.toFloat() / 2, mOutlinePaint)
        if (mBorderWidth > 0F && mOutlinePaint.shader == null) {
            drawBorder(canvas)
        }

        canvas?.drawArc(mOutlineRect.toRectF(), mStartAngle, mSweepAngle, true, mSweepPaint)

        if (!mTargetList.isNullOrEmpty() && !mIsAnimating) {
            drawTarget(canvas)
        }

        // draw center circle
        canvas?.drawCircle(measuredWidth.toFloat() / 2, measuredHeight.toFloat() / 2, measuredWidth.toFloat() / 2 / mScaleFactor, mPaint)
    }

    private fun drawBorder(canvas: Canvas?) {
        Log.i("RadarView", "drawBorder")
        mOutlinePaint.style = Paint.Style.STROKE
        mOutlinePaint.color = mBorderColor
        mOutlinePaint.strokeWidth = mBorderWidth
        canvas?.drawCircle(measuredWidth.toFloat() / 2, measuredHeight.toFloat() / 2,
            (measuredWidth.toFloat() - mBorderWidth) / 2, mOutlinePaint)
        // 还原
        mOutlinePaint.style = Paint.Style.FILL_AND_STROKE
        mOutlinePaint.color = mBackgroundColor
    }

    private fun drawTarget(canvas: Canvas?) {
        mTargetList?.let {
            Log.e("RadarView", "draw target")
            for (target in it) {
                if (mNeedBitmap) {
                    canvas?.drawBitmap(mBitmap, target.x - mBitmap.width / 2,
                        target.y - mBitmap.height / 2, mTargetPaint)
                } else {
                    canvas?.drawCircle(target.x, target.y, mTargetRadius, mTargetPaint)
                }
            }
        }
    }

    fun setBitmapEnabled(enabled: Boolean, drawable: Drawable) {
        // 这里是为了防止界面还未获取到宽高时 会导致onMeasure走不到 那么maxSize就会为0
        post {
            this.mNeedBitmap = enabled
            this.mBitmapWHRatio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
            mBitmap = if (mBitmapWHRatio >= 1) {
                // 宽比高大
                drawable.toBitmap(
                    width = min(mBitmapMaxSize, drawable.intrinsicWidth.toFloat()).toInt(),
                    height = (min(mBitmapMaxSize, drawable.intrinsicWidth.toFloat()) / mBitmapWHRatio).toInt(),
                    config = Bitmap.Config.ARGB_8888)
            } else {
                // 高比宽大
                drawable.toBitmap(
                    height = min(mBitmapMaxSize, drawable.intrinsicHeight.toFloat()).toInt(),
                    width = (min(mBitmapMaxSize, drawable.intrinsicHeight.toFloat()) * mBitmapWHRatio).toInt(),
                    config = Bitmap.Config.ARGB_8888
                )
            }
        }
    }

    // 随机落点
    fun addTarget(size: Int, type: TYPE = TYPE.RANDOM) {
        val list = ArrayList<PointF>()
        val r = measuredWidth.toFloat() / 2
        val innerRect = Rect((r - r / mScaleFactor).toInt(), (r - r / mScaleFactor).toInt(),
            (r + r / mScaleFactor).toInt(), (r + r / mScaleFactor).toInt())
        // 圆的中心点
        val circle = PointF(measuredWidth.toFloat() / 2, measuredHeight.toFloat() / 2)
        while (list.size < size) {
            val ranX = Random.nextDouble(0.0, r * 2.0).toFloat()
            val ranY = Random.nextDouble(0.0, r * 2.0).toFloat()
            val ranPointF = PointF(ranX, ranY)
            if (innerRect.contains(ranPointF.toPoint())) {
                continue
            }
            // 圆公式
            if (!mNeedBitmap &&
                (ranX - circle.x).pow(2) + (ranY - circle.y).pow(2) <
                  (r - mTargetRadius - mBorderWidth).toDouble().pow(2.0)) {
                // 在圆内
                addTargetFromType(type, list, ranX, ranY, r, ranPointF)
            } else if (mNeedBitmap &&
                (ranX - circle.x).pow(2) + (ranY - circle.y).pow(2) <
                  (r - mBorderWidth - max(mBitmap.width, mBitmap.height) / 2).toDouble().pow(2)) {
                addTargetFromType(type, list, ranX, ranY, r, ranPointF)
            } else {
                continue
            }
        }
        mTargetList = list
        for (target in list) {
            Log.i("RadarView", "target = [${target.x}, ${target.y}]")
        }
        invalidate()
    }

    private fun addTargetFromType(type: TYPE, list: ArrayList<PointF>, ranX: Float, ranY: Float,
                                  r: Float, ranPointF: PointF) {
        when (type) {
            TYPE.RANDOM -> {
                list.add(ranPointF)
            }
            TYPE.FOURTH -> {
                if (ranX in r.toDouble()..2 * r.toDouble() && ranY in r.toDouble()..2 * r.toDouble()) {
                    list.add(ranPointF)
                }
            }
            TYPE.THIRD -> {
                if (ranX in 0.0..r.toDouble() && ranY in r.toDouble()..2 * r.toDouble()) {
                    list.add(ranPointF)
                }
            }
            TYPE.SECOND -> {
                if (ranX in 0.0..r.toDouble() && ranY in 0.0..r.toDouble()) {
                    list.add(ranPointF)
                }
            }
            TYPE.FIRST -> {
                if (ranX in r.toDouble()..2 * r.toDouble() && ranY in 0.0..r.toDouble()) {
                    list.add(ranPointF)
                }
            }
        }
    }

    fun start() {
        Log.i("RadarView", "animation start")
        mIsAnimating = true
        mAnimator.duration = 2000
        mAnimator.repeatCount = ValueAnimator.INFINITE
        mAnimator.addUpdateListener {
            val angle = it.animatedValue as Float
            mStartAngle = angle

            Log.i("RadarView", "mStartAngle = $mStartAngle and curValue = ${it.animatedValue}")
            postInvalidate()
        }
        mAnimator.start()
    }

    fun start(startVal: Float, endVal: Float) {
        mIsAnimating = true
        mAnimator.setFloatValues(startVal, endVal)
        mAnimator.duration = 2000
        mAnimator.repeatCount = ValueAnimator.INFINITE
        mAnimator.addUpdateListener {
            mStartAngle = it.animatedValue as Float

            Log.i("RadarView", "mStartAngle = $mStartAngle and curValue = ${it.animatedValue}")
            postInvalidate()
        }
        mAnimator.start()
    }

    fun stop() {
        mIsAnimating = false
        if (mAnimator.isRunning) {
            mAnimator.cancel()
            mAnimator.removeAllListeners()
        }
        mStartAngle = 0F
    }

}