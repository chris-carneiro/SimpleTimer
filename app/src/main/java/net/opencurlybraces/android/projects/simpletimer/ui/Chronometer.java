package net.opencurlybraces.android.projects.simpletimer.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import net.opencurlybraces.android.projects.simpletimer.util.TimeUtils;

import java.util.Formatter;
import java.util.IllegalFormatException;
import java.util.Locale;

/**
 * This class is based on the {@link android.widget.Chronometer}. It was created to display the
 * milliseconds. <BR/> Created by chris on 23/04/15.
 */
public class Chronometer extends TextView {
    private static final String TAG = "Chronometer";

    /**
     * A callback that notifies when the chronometer has incremented on its own.
     */
    public interface OnChronometerTickListener {

        /**
         * Notification that the chronometer has changed.
         */
        void onChronometerTick(Chronometer chronometer);

    }

    private long mBase;
    private boolean mVisible;
    private boolean mPaused;
    private boolean mStarted;
    private boolean mRunning;
    private boolean mLogged;
    private String mFormat;
    private Formatter mFormatter;
    private Locale mFormatterLocale;
    private Object[] mFormatterArgs = new Object[1];
    private StringBuilder mFormatBuilder;
    private OnChronometerTickListener mOnChronometerTickListener;
    private StringBuilder mRecycle = new StringBuilder(8);

    /**
     * Time refresh rate set to One(1) millisecond. Under the hood: used to delay the handler's
     * message by one(1) second
     */
    private static final long MILLISEC = 1;

    private static final int TICK_WHAT = 2;

    private static final int FLICKER_WHAT = 3;

    private static final int FLICKER_RATE = 500;

    /**
     * Initialize this Chronometer object. Sets the base to the current time.
     */
    public Chronometer(Context context) {
        this(context, null, 0);
    }

    /**
     * Initialize with standard view layout information. Sets the base to the current time.
     */
    public Chronometer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Initialize with standard view layout information and style. Sets the base to the current
     * time.
     */
    public Chronometer(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @TargetApi (Build.VERSION_CODES.LOLLIPOP)
    public Chronometer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    {
        init();
    }

    private void init() {
        mBase = SystemClock.elapsedRealtime();
        updateText(mBase);
    }

    /**
     * Set the time that the count-up timer is in reference to.
     *
     * @param base Use the {@link SystemClock#elapsedRealtime} time base.
     */
    public void setBase(long base) {
        mBase = base;
        dispatchChronometerTick();
        updateText(SystemClock.elapsedRealtime());
    }

    /**
     * Return the base time as set through {@link #setBase}.
     */
    public long getBase() {
        return mBase;
    }

    /**
     * Sets the format string used for display.  The Chronometer will display this string, with the
     * first "%s" replaced by the current timer value in "MM:SS" or "H:MM:SS" form.
     * <p/>
     * If the format string is null, or if you never call setFormat(), the Chronometer will simply
     * display the timer value in "MM:SS" or "H:MM:SS" form.
     *
     * @param format the format string.
     */
    public void setFormat(String format) {
        mFormat = format;
        if (format != null && mFormatBuilder == null) {
            mFormatBuilder = new StringBuilder(format.length() * 2);
        }
    }

    /**
     * Returns the current format string as set through {@link #setFormat}.
     */
    public String getFormat() {
        return mFormat;
    }

    /**
     * Sets the listener to be called when the chronometer changes.
     *
     * @param listener The listener.
     */
    public void setOnChronometerTickListener(OnChronometerTickListener listener) {
        mOnChronometerTickListener = listener;
    }

    /**
     * @return The listener (may be null) that is listening for chronometer change events.
     */
    public OnChronometerTickListener getOnChronometerTickListener() {
        return mOnChronometerTickListener;
    }

    /**
     * Start counting up.  This does not affect the base as set from {@link #setBase}, just the view
     * display.
     * <p/>
     * Chronometer works by regularly scheduling messages to the handler, even when the Widget is
     * not visible.  To make sure resource leaks do not occur, the user should make sure that each
     * start() call has a reciprocal call to {@link #stop}.
     */
    public void start() {
        mStarted = true;
        mPaused = false;
        updateRunning();
        updateFlickering();
    }

    /**
     * Stop counting up.  This does not affect the base as set from {@link #setBase}, just the view
     * display.
     * <p/>
     * This stops the messages to the handler, effectively releasing resources that would be held as
     * the chronometer is running, via {@link #start}.
     */
    public void stop() {
        mStarted = false;
        mPaused = true;
        updateRunning();
        updateFlickering();
    }

    /**
     * The same as calling {@link #start} or {@link #stop}.
     *
     * @hide pending API council approval
     */
    public void setStarted(boolean started) {
        mStarted = started;
        updateRunning();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;
        updateRunning();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mVisible = visibility == VISIBLE;
        updateRunning();
    }

    private synchronized void updateText(long now) {

        long millis = now - mBase;

        String text = TimeUtils.formatElapsedTime(mRecycle,
                millis);

        if (mFormat != null) {
            Locale loc = Locale.getDefault();
            if (mFormatter == null || !loc.equals(mFormatterLocale)) {
                mFormatterLocale = loc;
                mFormatter = new Formatter(mFormatBuilder, loc);
            }
            mFormatBuilder.setLength(0);
            mFormatterArgs[0] = text;
            try {
                mFormatter.format(mFormat, mFormatterArgs);
                text = mFormatBuilder.toString();
            } catch (IllegalFormatException ex) {
                if (!mLogged) {
                    Log.w(TAG, "Illegal format string: " + mFormat);
                    mLogged = true;
                }
            }
        }
        setText(text);
    }

    private void updateRunning() {
        boolean running = mVisible && mStarted;
        if (running != mRunning) {
            if (running) {
                updateText(SystemClock.elapsedRealtime());
                dispatchChronometerTick();
                mHandler.sendMessageDelayed(Message.obtain(mHandler, TICK_WHAT), MILLISEC);
            } else {
                mHandler.removeMessages(TICK_WHAT);
            }
            mRunning = running;
        }
    }


    private void updateFlickering() {
            if (mPaused) {
                mHandlerFlicker.sendMessageDelayed(Message.obtain(mHandlerFlicker, FLICKER_WHAT),
                        FLICKER_RATE);
                setVisibility(getVisibility() == VISIBLE ? INVISIBLE : VISIBLE);
            } else {
                mHandlerFlicker.removeMessages(FLICKER_WHAT);
                setVisibility(VISIBLE );
            }



    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message m) {
            if (mRunning) {
                updateText(SystemClock.elapsedRealtime());
                dispatchChronometerTick();
                sendMessageDelayed(Message.obtain(this, TICK_WHAT), MILLISEC);
            }
        }
    };

    private Handler mHandlerFlicker = new Handler() {
        public void handleMessage(Message m) {
            if (mPaused) {
                setVisibility(getVisibility() == VISIBLE ? INVISIBLE : VISIBLE);
                sendMessageDelayed(Message.obtain(this, FLICKER_WHAT), FLICKER_RATE);
            }
        }
    };

    void dispatchChronometerTick() {
        if (mOnChronometerTickListener != null) {
            mOnChronometerTickListener.onChronometerTick(this);
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(Chronometer.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(Chronometer.class.getName());
    }

}
