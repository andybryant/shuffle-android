package org.dodgybits.shuffle.android.list.view.project;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.util.FontUtils;
import org.dodgybits.shuffle.android.core.util.TaskLifecycleState;
import org.dodgybits.shuffle.android.core.view.ContextIcon;
import org.dodgybits.shuffle.android.core.view.TextColours;
import org.dodgybits.shuffle.android.list.view.task.TaskListItemCoordinates;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This custom View is the list item for the ProjectListFragment, and serves two purposes:
 * 1.  It's a container to store project details
 * 2.  It handles internal clicks
 */
public class ProjectListItem extends View {
    private ProjectListItemCoordinates mCoordinates;
    private android.content.Context mAndroidContext;
    private ProjectListFragment.ProjectHolder mHolder;

    private Project mProject;
    private String mName;
    private StaticLayout mNameLayout;
    private boolean mIsParallel;
    private boolean mIsActive = true;
    private boolean mIsDeleted = false;
    private String mCount;
    private SparseIntArray mTaskCountArray;

    private boolean mDownEvent;

    @Inject
    public ProjectListItem(
            android.content.Context androidContext) {
        super(androidContext);
        init(androidContext);
    }

    private static boolean sInit = false;
    private static TextColours sTextColours;
    private static final TextPaint sRegularPaint = new TextPaint();
    private static final TextPaint sBoldPaint = new TextPaint();
    private static final Paint sSelectorBackgroundPaint = new Paint();

    private static Bitmap sStateInactive;
    private static Bitmap sStateDeleted;
    private static Bitmap sParallel;
    private static Bitmap sSequential;
    private static Bitmap sActivated;

    // Static colors.
    private static int ACTIVATED_TEXT_COLOR;
    private static int NAME_TEXT_COLOR_ACTIVE;
    private static int NAME_TEXT_COLOR_INACTIVE;
    private static int COUNT_TEXT_COLOR_ACTIVE;
    private static int COUNT_TEXT_COLOR_INACTIVE;

    private int mViewWidth = 0;
    private int mViewHeight = 0;

    private static int sItemHeight;

    public void setHolder(ProjectListFragment.ProjectHolder holder) {
        mHolder = holder;
    }

    private void init(android.content.Context context) {
        mAndroidContext = context;

        if (!sInit) {
            sTextColours = TextColours.getInstance(context);
            Resources r = context.getResources();
            sItemHeight = r.getDimensionPixelSize(R.dimen.list_item_height);

            FontUtils.setCustomFont(sRegularPaint, context.getAssets(), FontUtils.REGULAR);
            sRegularPaint.setAntiAlias(true);
            FontUtils.setCustomFont(sBoldPaint, context.getAssets(), FontUtils.BOLD);
            sBoldPaint.setAntiAlias(true);

            sStateInactive =
                    BitmapFactory.decodeResource(r, R.drawable.ic_visibility_off_black_24dp);
            sStateDeleted =
                    BitmapFactory.decodeResource(r, R.drawable.ic_delete_black_24dp);
            sParallel =
                    BitmapFactory.decodeResource(r, R.drawable.parallel);
            sSequential =
                    BitmapFactory.decodeResource(r, R.drawable.sequence);
            sActivated =
                    BitmapFactory.decodeResource(r, R.drawable.ic_brightness_1_black_24dp);

            ACTIVATED_TEXT_COLOR = r.getColor(android.R.color.black);
            NAME_TEXT_COLOR_ACTIVE = r.getColor(R.color.name_text_color_active);
            NAME_TEXT_COLOR_INACTIVE = r.getColor(R.color.name_text_color_inactive);
            COUNT_TEXT_COLOR_ACTIVE = r.getColor(R.color.count_text_color_active);
            COUNT_TEXT_COLOR_INACTIVE = r.getColor(R.color.count_text_color_inactive);

            sInit = true;
        }
    }

    /**
     * Invalidate all drawing caches associated with drawing task list items.
     * This is an expensive operation, and should be done rarely, such as when system font size
     * changes occurs.
     */
    public static void resetDrawingCaches() {
        ProjectListItemCoordinates.resetCaches();
        sInit = false;
    }

    public void updateView(Project project, SparseIntArray taskCountArray) {
        mProject = project;
        mTaskCountArray = taskCountArray;
        mIsParallel = project.isParallel();
        mIsActive = project.isActive();
        mIsDeleted = project.isDeleted();
        updateCount(project);

        if (mName == null || !mName.equals(project.getName())) {
            mName = project.getName();
            requestLayout();
        }
    }

    private void updateCount(Project project) {
        if (mTaskCountArray != null) {
            int count = mTaskCountArray.get((int)project.getLocalId().getId());
            mCount = String.valueOf(count);
        } else {
            mCount = "";
        }
    }

    private Drawable mCurrentBackground = null; // Only used by updateBackground()

    private void updateBackground() {
        final Drawable newBackground = getResources().getDrawable(R.drawable.list_selector_background);
        if (newBackground != mCurrentBackground) {
            // setBackgroundDrawable is a heavy operation.  Only call it when really needed.
            setBackgroundDrawable(newBackground);
            mCurrentBackground = newBackground;
        }
    }

    private void calculateDrawingData() {
        TextPaint namePaint = sBoldPaint;
        namePaint.setColor(getFontColor(mIsActive ? NAME_TEXT_COLOR_ACTIVE : NAME_TEXT_COLOR_INACTIVE));
        namePaint.setTextSize(mCoordinates.nameFontSize);
        mNameLayout = new StaticLayout(mName, namePaint,
                mCoordinates.nameWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, false /* includePad */);
        if (mCoordinates.nameLineCount < mNameLayout.getLineCount()) {
            int end = mNameLayout.getLineVisibleEnd(mCoordinates.nameLineCount - 1) + 1;
            do {
                end--;
                mNameLayout = new StaticLayout(mName.subSequence(0, end) + "…",
                        namePaint, mCoordinates.nameWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, true);
            } while (mCoordinates.nameLineCount < mNameLayout.getLineCount());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (widthMeasureSpec != 0 || mViewWidth == 0) {
            mViewWidth = MeasureSpec.getSize(widthMeasureSpec);
            mViewHeight = measureHeight(heightMeasureSpec);
        }
        setMeasuredDimension(mViewWidth, mViewHeight);
    }

    /**
     * Determine the height of this view
     *
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text
            result = sItemHeight;
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by
                // measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    public void draw(Canvas canvas) {
        updateBackground();
        super.draw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mCoordinates = ProjectListItemCoordinates.forWidth(mAndroidContext, mViewWidth);
        mCoordinates.selectorSourceIconRect = new Rect(0, 0, sParallel.getWidth(), sParallel.getHeight());
        calculateDrawingData();
    }

    private int getFontColor(int defaultColor) {
        return isActivated() ? ACTIVATED_TEXT_COLOR : defaultColor;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawName(canvas);
        drawCount(canvas);
        drawState(canvas);
        if (isActivated()) {
            drawActivatedIndicator(canvas);
        } else {
            drawSelector(canvas);
        }
    }

    private void drawName(Canvas canvas) {
        canvas.save();
        canvas.translate(
                mCoordinates.nameX,
                mCoordinates.nameY);
        mNameLayout.draw(canvas);
        canvas.restore();
    }

    private void drawCount(Canvas canvas) {
        TextPaint countPaint = sRegularPaint;
        countPaint.setTextSize(mCoordinates.countFontSize);
        countPaint.setColor(getFontColor(mIsActive ? COUNT_TEXT_COLOR_ACTIVE : COUNT_TEXT_COLOR_INACTIVE));
        canvas.drawText(mCount, 0, mCount.length(),
                mCoordinates.countX, mCoordinates.countY - mCoordinates.countAscent, countPaint);
    }

    private void drawState(Canvas canvas) {
        if (mIsDeleted) {
            canvas.drawBitmap(sStateDeleted,
                    mCoordinates.stateX, mCoordinates.stateY, null);
        } else if (!mIsActive) {
            canvas.drawBitmap(sStateInactive,
                    mCoordinates.stateX, mCoordinates.stateY, null);
        }
    }

    private void drawActivatedIndicator(Canvas canvas) {
        sSelectorBackgroundPaint.setShader(null);
        sSelectorBackgroundPaint.setColor(getResources().getColor(R.color.white));
        canvas.drawOval(mCoordinates.selectorRect, sSelectorBackgroundPaint);
        canvas.drawBitmap(sActivated, mCoordinates.selectorSourceIconRect,
                mCoordinates.activatedRect, null);
    }


    private void drawSelector(Canvas canvas) {
        int bgColor = sTextColours.getBackgroundColour(17);
        sSelectorBackgroundPaint.setShader(getShader(bgColor, mCoordinates.selectorRect, 0f));
        canvas.drawOval(mCoordinates.selectorRect, sSelectorBackgroundPaint);
        canvas.drawBitmap(mIsParallel ? sParallel : sSequential,
                mCoordinates.selectorSourceIconRect,
                mCoordinates.activatedRect, null);
    }

    private Shader getShader(int colour, RectF rect) {
        return getShader(colour, rect, 0.03f);
    }

    private Shader getShader(int colour, RectF rect, float offset) {
        final float startOffset = 1f + offset;
        final float endOffset = 1f - offset;
        
        int[] colours = new int[2];
        float[] hsv1 = new float[3];
        float[] hsv2 = new float[3];
        Color.colorToHSV(colour, hsv1);
        Color.colorToHSV(colour, hsv2);
        hsv1[2] *= startOffset;
        hsv2[2] *= endOffset;
        colours[0] = Color.HSVToColor(hsv1);
        colours[1] = Color.HSVToColor(hsv2);

        return new LinearGradient(rect.left, rect.top, rect.left, rect.bottom,
                colours, null, Shader.TileMode.CLAMP);
    }

    private static final int TOUCH_SLOP = 24;
    private static int sScaledTouchSlop = -1;

    private void initializeSlop(android.content.Context context) {
        if (sScaledTouchSlop == -1) {
            final Resources res = context.getResources();
            final Configuration config = res.getConfiguration();
            final float density = res.getDisplayMetrics().density;
            final float sizeAndDensity;
            if (config.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_XLARGE)) {
                sizeAndDensity = density * 1.5f;
            } else {
                sizeAndDensity = density;
            }
            sScaledTouchSlop = (int) (sizeAndDensity * TOUCH_SLOP + 0.5f);
        }
    }


    /**
     * Overriding this method allows us to "catch" clicks in the checkbox or star
     * and process them accordingly.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initializeSlop(getContext());

        boolean handled = false;
        int touchX = (int) event.getX();
        int checkRight = mCoordinates.selectorX
                + mCoordinates.selectorWidth + sScaledTouchSlop;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (touchX < checkRight) {
                    mDownEvent = true;
                    handled = true;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                mDownEvent = false;
                break;

            case MotionEvent.ACTION_UP:
                if (mDownEvent) {
                    if (touchX < checkRight) {
                        mHolder.clickSelector();
                        handled = true;
                    }
                }
                break;
        }

        if (handled) {
            invalidate();
        } else {
            handled = super.onTouchEvent(event);
        }

        return handled;
    }


    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        event.setClassName(getClass().getName());
        event.setPackageName(getContext().getPackageName());
        event.setEnabled(true);
        event.setContentDescription(getContentDescription());
        return true;
    }

}