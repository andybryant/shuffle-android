package org.dodgybits.shuffle.android.list.view.task;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.*;
import android.text.format.DateUtils;
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
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.core.util.FontUtils;
import org.dodgybits.shuffle.android.core.util.TaskLifecycleState;
import org.dodgybits.shuffle.android.core.view.ContextIcon;
import org.dodgybits.shuffle.android.core.view.TextColours;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This custom View is the list item for the TaskRecyclerFragment, and serves two purposes:
 * 1.  It's a container to store task details
 * 2.  It handles internal clicks
 */
public class TaskListItem extends View {
    private static final String TAG = "TaskListItem";

    private TaskListItemCoordinates mCoordinates;
    private android.content.Context mAndroidContext;
    private TaskRecyclerFragment.TaskHolder mHolder;

    private final EntityCache<Context> mContextCache;
    private final EntityCache<Project> mProjectCache;

    private Project mProject;
    private String mSnippet;
    private String mDescription;
    private StaticLayout mDescriptionLayout;
    private StaticLayout mSnippetLayout;
    private boolean mIsCompleted;
    private boolean mIsActive = true;
    private boolean mIsDeleted = false;

    private List<Context> mContexts = Collections.emptyList();

    private boolean mDownEvent;

    @Inject
    public TaskListItem(
            android.content.Context androidContext,
            EntityCache<Context> contextCache,
            EntityCache<Project> projectCache) {
        super(androidContext);
        mContextCache = contextCache;
        mProjectCache = projectCache;
        init(androidContext);
    }

    private static boolean sInit = false;
    private static TextColours sTextColours;
    private static final TextPaint sRegularPaint = new TextPaint();
    private static final TextPaint sBoldPaint = new TextPaint();
    private static final Paint sContextBackgroundPaint = new Paint();
    private static final Paint sContextMorePaint = new Paint();
    static {
        final LightingColorFilter lcf = new LightingColorFilter( 0xFF000000, 0xFFAAAAAA);
        sContextMorePaint.setColorFilter(lcf);
    }
    private static final Paint sSelectedIndicatorPaint = new Paint();

    private static Bitmap sStateInactive;
    private static Bitmap sStateDeleted;
    private static Bitmap sStateCompleted;
    private static Bitmap sMoreHorizontal;
    private static Bitmap sActivated;

    private static Map<String, Bitmap> mContextIconMap;

    // Static colors.
    private static int ACTIVATED_TEXT_COLOR;
    private static int DESCRIPTION_TEXT_COLOR_COMPLETE;
    private static int DESCRIPTION_TEXT_COLOR_INCOMPLETE;
    private static int SNIPPET_TEXT_COLOR_COMPLETE;
    private static int SNIPPET_TEXT_COLOR_INCOMPLETE;
    private static int PROJECT_TEXT_COLOR_COMPLETE;
    private static int PROJECT_TEXT_COLOR_INCOMPLETE;
    private static int DATE_TEXT_COLOR_COMPLETE;
    private static int DATE_TEXT_COLOR_PENDING_INCOMPLETE;
    private static int DATE_TEXT_COLOR_DUE_PAST_INCOMPLETE;
    private static int DATE_TEXT_COLOR_DUE_FUTURE_INCOMPLETE;

    private int mViewWidth = 0;
    private int mViewHeight = 0;

    private static int sItemHeight;

    private CharSequence mFormattedProject;
    private CharSequence mFormattedDate = "";
    private int mDateColor;

    public void setHolder(TaskRecyclerFragment.TaskHolder holder) {
        mHolder = holder;
    }

    private void init(android.content.Context context) {
        mAndroidContext = context;
        
        if (!sInit) {
            sTextColours = TextColours.getInstance(context);
            Resources r = context.getResources();
            sItemHeight = r.getDimensionPixelSize(R.dimen.task_list_item_height);

            FontUtils.setCustomFont(sRegularPaint, context.getAssets(), FontUtils.REGULAR);
            sRegularPaint.setAntiAlias(true);
            FontUtils.setCustomFont(sBoldPaint, context.getAssets(), FontUtils.BOLD);
            sBoldPaint.setAntiAlias(true);

            sStateInactive =
                    BitmapFactory.decodeResource(r, R.drawable.ic_visibility_off_black_24dp);
            sStateDeleted =
                    BitmapFactory.decodeResource(r, R.drawable.ic_delete_black_24dp);
            sStateCompleted =
                    BitmapFactory.decodeResource(r, R.drawable.ic_done_green_24dp);
            sMoreHorizontal =
                    BitmapFactory.decodeResource(r, R.drawable.ic_more_horiz_black_24dp);
            sActivated =
                    BitmapFactory.decodeResource(r, R.drawable.ic_done_black_24dp);

            mContextIconMap = Maps.newHashMap();        
    
            ACTIVATED_TEXT_COLOR = r.getColor(android.R.color.black);
            DESCRIPTION_TEXT_COLOR_COMPLETE = r.getColor(R.color.description_text_color_complete);
            DESCRIPTION_TEXT_COLOR_INCOMPLETE = r.getColor(R.color.description_text_color_incomplete);
            SNIPPET_TEXT_COLOR_COMPLETE = r.getColor(R.color.snippet_text_color_complete);
            SNIPPET_TEXT_COLOR_INCOMPLETE = r.getColor(R.color.snippet_text_color_incomplete);
            PROJECT_TEXT_COLOR_COMPLETE = r.getColor(R.color.project_text_color_complete);
            PROJECT_TEXT_COLOR_INCOMPLETE = r.getColor(R.color.project_text_color_incomplete);
            DATE_TEXT_COLOR_COMPLETE = r.getColor(R.color.date_text_color_complete);
            DATE_TEXT_COLOR_DUE_PAST_INCOMPLETE = r.getColor(R.color.date_text_color_due_past_incomplete);
            DATE_TEXT_COLOR_DUE_FUTURE_INCOMPLETE = r.getColor(R.color.date_text_color_due_future_incomplete);
            DATE_TEXT_COLOR_PENDING_INCOMPLETE = r.getColor(R.color.deferred);

            sInit = true;
        }
    }
    
    /**
     * Invalidate all drawing caches associated with drawing task list items.
     * This is an expensive operation, and should be done rarely, such as when system font size
     * changes occurs.
     */
    public static void resetDrawingCaches() {
        TaskListItemCoordinates.resetCaches();
        sInit = false;
    }

    private boolean mProjectNameVisible = true;

    public void setTask(Task task, boolean projectNameVisible, boolean isSelected) {
        mProjectNameVisible = projectNameVisible;
        mIsCompleted = task.isComplete();
        mProject = mProjectCache.findById(task.getProjectId());
        List<Context> contexts = mContextCache.findById(task.getContextIds());
        mIsActive = TaskLifecycleState.getActiveStatus(task, contexts, mProject) == TaskLifecycleState.Status.yes;
        mIsDeleted = TaskLifecycleState.getDeletedStatus(task, mProject) != TaskLifecycleState.Status.no;
        setTimestamp(task.getStartDate(), task.getDueDate());
        setSelected(isSelected);

        boolean changed = setContexts(contexts);
        changed |= setText(task.getDescription(), task.getDetails());
        
        if (changed) {
            requestLayout();
        }
    }

    private boolean setContexts(List<Context> contexts) {
        boolean changed = true;

        // don't show deleted contexts
        contexts = Lists.newArrayList(Iterables.filter(contexts, new Predicate<Context>() {
            @Override
            public boolean apply(Context context) {
                return !context.isDeleted();
            }
        }));

        if (contexts.size() == mContexts.size()) {
            Set<Id> currentIds = Sets.newHashSet();
            for (Context context : mContexts) {
                currentIds.add(context.getLocalId());
            }
            
            Set<Id> newIds = Sets.newHashSet();
            for (Context context : contexts) {
                newIds.add(context.getLocalId());
            }

            changed = !currentIds.equals(newIds);
        }
        Collections.sort(contexts);
        mContexts = contexts;

        return changed;
    }
    
    /**
     * Sets message contents and snippet safely, ensuring the cache is invalidated.
     */
    private boolean setText(String description, String snippet) {
        boolean changed = false;
        if (!Objects.equal(mDescription, description)) {
            mDescription = description;
            changed = true;
        }

        String singleLine = stripLineEndings(snippet);
        if (!Objects.equal(mSnippet, singleLine)) {
            mSnippet = singleLine;
            changed = true;
        }
        
        return changed;
    }

    long mTimestampMillis = 0L;
    private void setTimestamp(long start, long due) {
        long timestamp;
        long now = System.currentTimeMillis();
        if (isDone()) {
            timestamp = 0L;
            mDateColor = DATE_TEXT_COLOR_COMPLETE;
        } else if (start > now) {
            timestamp = start;
            mDateColor = DATE_TEXT_COLOR_PENDING_INCOMPLETE;
        } else {
            timestamp = due;
            if (due > now) {
                mDateColor = DATE_TEXT_COLOR_DUE_FUTURE_INCOMPLETE;
            } else {
                mDateColor = DATE_TEXT_COLOR_DUE_PAST_INCOMPLETE;
            }
        }
        if (mTimestampMillis != timestamp) {
            mFormattedDate = timestamp == 0L ? "" :
                    DateUtils.getRelativeTimeSpanString(mAndroidContext, timestamp).toString();
            mTimestampMillis = timestamp;
        }
    }

    private String stripLineEndings(String val) {
        return TextUtils.isEmpty(val) ? "" :
                val.replace("\n", " ").replace("\r", "");
    }

    private boolean isDone() {
        return mIsCompleted || mIsDeleted;
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
        TextPaint descriptionPaint = isDone() ? sRegularPaint : sBoldPaint;
        descriptionPaint.setTextSize(mCoordinates.descriptionFontSize);
        mDescriptionLayout = new StaticLayout(mDescription, descriptionPaint,
                mCoordinates.descriptionWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, false /* includePad */);
        if (mCoordinates.descriptionLineCount < mDescriptionLayout.getLineCount()) {
            int end = mDescriptionLayout.getLineEnd(mCoordinates.descriptionLineCount - 1);
            mDescriptionLayout = new StaticLayout(mDescription.subSequence(0, end) + "…",
                    descriptionPaint, mCoordinates.descriptionWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, true);
        }

        sRegularPaint.setTextSize(mCoordinates.detailsFontSize);
        mSnippetLayout = new StaticLayout(mSnippet, sRegularPaint,
                mCoordinates.detailsWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, false /* includePad */);
        if (mCoordinates.detailsLineCount < mSnippetLayout.getLineCount()) {
            int end = mSnippetLayout.getLineEnd(mCoordinates.detailsLineCount - 1);
            mSnippetLayout = new StaticLayout(mSnippet.subSequence(0, end),
                    sRegularPaint, mCoordinates.detailsWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, true);
        }

        // And the project...
        TextPaint projectPaint = sBoldPaint;
        String projectName = (mProject == null || !mProjectNameVisible) ? "" : mProject.getName();
        projectPaint.setTextSize(mCoordinates.projectFontSize);
        int projectTextWidth = (int)projectPaint.measureText(projectName, 0, projectName.length());
        
        // give or take the difference in space from the project
        int projectWidth = Math.min(projectTextWidth, mCoordinates.projectWidth);
        mFormattedProject = TextUtils.ellipsize(projectName, projectPaint, projectWidth,
                TextUtils.TruncateAt.END);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (widthMeasureSpec != 0 || mViewWidth == 0) {
            mViewWidth = View.MeasureSpec.getSize(widthMeasureSpec);
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
        int result = 0;
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize = View.MeasureSpec.getSize(measureSpec);

        if (specMode == View.MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text
            result = sItemHeight;
            if (specMode == View.MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by
                // measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    public void draw(Canvas canvas) {
        // Update the background, before View.draw() draws it.
//        setSelected(mAdapter.isSelected(this));
        updateBackground();
        super.draw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mCoordinates = TaskListItemCoordinates.forWidth(mAndroidContext, mViewWidth);
        calculateDrawingData();
    }

    private int getFontColor(int defaultColor) {
        return  isActivated() ? ACTIVATED_TEXT_COLOR : defaultColor;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int yOffset = (mFormattedProject.length() == 0 && mFormattedDate.length() == 0) ?
                mCoordinates.projectOffset : 0;
        drawProject(canvas);
        drawState(canvas, yOffset);
        drawDescription(canvas, yOffset);
        drawSnippet(canvas, yOffset);
        drawDate(canvas);
        if (isActivated()) {
            drawActivatedIndicator(canvas);
        } else {
            drawContexts(canvas);
        }
        if (isSelected()) {
            drawSelectedIndicator(canvas);
        }
    }

    private void drawProject(Canvas canvas) {
        Paint projectPaint = sBoldPaint;
        projectPaint.setColor(getFontColor(isDone() ? PROJECT_TEXT_COLOR_COMPLETE
                : PROJECT_TEXT_COLOR_INCOMPLETE));
        projectPaint.setTextSize(mCoordinates.projectFontSize);
        canvas.drawText(mFormattedProject, 0, mFormattedProject.length(),
                mCoordinates.projectX, mCoordinates.projectY - mCoordinates.projectAscent,
                projectPaint);
    }

    private void drawState(Canvas canvas, int yOffset) {
        if (mIsDeleted) {
            canvas.drawBitmap(sStateDeleted,
                    mCoordinates.stateX, mCoordinates.stateY + yOffset, null);
        } else if (mIsCompleted) {
            canvas.drawBitmap(sStateCompleted,
                    mCoordinates.stateX, mCoordinates.stateY + yOffset, null);
        } else if (!mIsActive) {
            canvas.drawBitmap(sStateInactive,
                    mCoordinates.stateX, mCoordinates.stateY + yOffset, null);
        }
    }

    private void drawDescription(Canvas canvas, int yOffset) {
        TextPaint descriptionPaint = isDone() ? sRegularPaint : sBoldPaint;
        descriptionPaint.setColor(getFontColor(isDone() ? DESCRIPTION_TEXT_COLOR_COMPLETE
                : DESCRIPTION_TEXT_COLOR_INCOMPLETE));
        descriptionPaint.setTextSize(mCoordinates.descriptionFontSize);
        canvas.save();
        canvas.translate(
                mCoordinates.descriptionX,
                mCoordinates.descriptionY + yOffset);
        mDescriptionLayout.draw(canvas);
        canvas.restore();
    }

    private void drawSnippet(Canvas canvas, int yOffset) {
        TextPaint snippetPaint = sRegularPaint;
        snippetPaint.setColor(getFontColor(isDone() ? SNIPPET_TEXT_COLOR_COMPLETE
                : SNIPPET_TEXT_COLOR_INCOMPLETE));
        snippetPaint.setTextSize(mCoordinates.descriptionFontSize);
        canvas.save();
        canvas.translate(
                mCoordinates.detailsX,
                mCoordinates.detailsY + yOffset);
        mSnippetLayout.draw(canvas);
        canvas.restore();
    }

    private void drawDate(Canvas canvas) {
        TextPaint datePaint = isDone() ? sRegularPaint : sBoldPaint;
        datePaint.setTextSize(mCoordinates.dateFontSize);
        datePaint.setColor(getFontColor(mDateColor));
        int dateX = mCoordinates.dateXEnd
                - (int) datePaint.measureText(mFormattedDate, 0, mFormattedDate.length());
        canvas.drawText(mFormattedDate, 0, mFormattedDate.length(),
                dateX, mCoordinates.dateY - mCoordinates.dateAscent, datePaint);
    }

    private void drawContexts(Canvas canvas) {
        int numContexts = Math.min(4, mContexts.size());
        if (mContexts.isEmpty()) {
            int bgColor = sTextColours.getBackgroundColour(17);
            sContextBackgroundPaint.setShader(getShader(bgColor, mCoordinates.contextRects[0][0], 0f));
            canvas.drawOval(mCoordinates.contextRects[0][0],sContextBackgroundPaint);
        } else {
            for (int i = 0; i < numContexts; i++) {
                Context context = mContexts.get(i);
                int bgColor = sTextColours.getBackgroundColour(context.getColourIndex());
                RectF contextRect = mCoordinates.contextRects[numContexts][i];
                sContextBackgroundPaint.setShader(getShader(bgColor, contextRect));
                sContextBackgroundPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
                canvas.drawOval(contextRect, sContextBackgroundPaint);
                Bitmap contextIcon = getContextIcon(context.getIconName());
                if (contextIcon == null) {
                    int textColor = sTextColours.getTextColour(context.getColourIndex());
                    int leftPadding, topPadding;
                    sRegularPaint.setColor(textColor);
                    String name = context.getName().toUpperCase();
                    if (numContexts == 1) {
                        sRegularPaint.setTextSize(mCoordinates.contextsSingleFontSize);
                        leftPadding = mCoordinates.contextsSingleLabelLeft;
                        topPadding = mCoordinates.contextsSingleLabelTop;
                    } else {
                        sRegularPaint.setTextSize(mCoordinates.contextsMultiFontSize);
                        leftPadding = mCoordinates.contextsLabelLeft;
                        topPadding = mCoordinates.contextsLabelTop;
                    }
                    int contextTextWidth = (int)sRegularPaint.measureText(name, 0, 1);
                    canvas.drawText(name, 0, 1,
                            contextRect.left + leftPadding - contextTextWidth / 2,
                            contextRect.top - mCoordinates.contextsAscent + topPadding,
                            sRegularPaint);
                } else {
                    RectF destIconRect = mCoordinates.contextDestIconRects[numContexts][i];
                    canvas.drawBitmap(contextIcon, mCoordinates.contextSourceIconRect, destIconRect, null);
                }
            }
        }
        if (mContexts.size() > 4) {
            canvas.drawBitmap(sMoreHorizontal, mCoordinates.contextSourceIconRect,
                    mCoordinates.contextMoreRect, sContextMorePaint);
        }
    }

    private void drawActivatedIndicator(Canvas canvas) {
        sContextBackgroundPaint.setShader(null);
        sContextBackgroundPaint.setColor(getResources().getColor(R.color.white));
        canvas.drawOval(mCoordinates.contextRects[0][0], sContextBackgroundPaint);
//        sContextBackgroundPaint.setColor(sTextColours.getBackgroundColour(0));
//        int radius = sContextCornerLargeRadius;
//        canvas.drawRoundRect(mCoordinates.activatedRect, radius, radius, sContextBackgroundPaint);
        canvas.drawBitmap(sActivated, mCoordinates.contextSourceIconRect,
                mCoordinates.contextDestIconRects[0][0], null);
    }

    private void drawSelectedIndicator(Canvas canvas) {
        sSelectedIndicatorPaint.setColor(getResources().getColor(R.color.list_selected_indicator));
        canvas.drawRect(mCoordinates.selectedRect, sSelectedIndicatorPaint);
    }

    private Bitmap getContextIcon(String iconName) {
        Bitmap icon = mContextIconMap.get(iconName);
        if (icon == null) {
            ContextIcon contextIcon = ContextIcon.createIcon(iconName, mAndroidContext.getResources(), true);
            if (contextIcon != null) {
                icon = BitmapFactory.decodeResource(mAndroidContext.getResources(), contextIcon.largeIconId);
                mContextIconMap.put(iconName, icon);
                if (mCoordinates.contextSourceIconRect == null) {
                    mCoordinates.contextSourceIconRect = new Rect(0, 0, icon.getWidth(), icon.getHeight());
                }
            }
        }
        return icon;
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
        int checkRight = mCoordinates.contextsX
                + mCoordinates.contextsWidth + sScaledTouchSlop;

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
                        mHolder.clickTag();
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