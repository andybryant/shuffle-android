package org.dodgybits.shuffle.android.list.view.task;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.*;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
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
import org.dodgybits.shuffle.android.core.util.OSUtils;
import org.dodgybits.shuffle.android.core.util.TaskLifecycleState;
import org.dodgybits.shuffle.android.core.view.ContextIcon;
import org.dodgybits.shuffle.android.core.view.TextColours;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This custom View is the list item for the TaskListFragment, and serves two purposes:
 * 1.  It's a container to store task details
 * 2.  It handles internal clicks such as the checkbox
 */
public class TaskListItem extends View {
    private static final String TAG = "TaskListItem";

    // Note: adapter directly fiddles with these fields.
    /* package */ long mTaskId;

    
    private TaskListAdaptor mAdapter;
    private TaskListItemCoordinates mCoordinates;
    private android.content.Context mAndroidContext;

    private final EntityCache<Context> mContextCache;
    private final EntityCache<Project> mProjectCache;

    private Project mProject;
    private SpannableStringBuilder mText;
    private String mSnippet;
    private String mDescription;
    private StaticLayout mContentsLayout;
    private boolean mIsCompleted;
    private boolean mIsActive = true;
    private boolean mIsDeleted = false;

    private List<Context> mContexts = Collections.emptyList();
    private boolean mShowContextText;

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
    private static final TextPaint sDefaultPaint = new TextPaint();
    private static final TextPaint sAllCapsPaint = new TextPaint();
    private static final TextPaint sDatePaint = new TextPaint();
    private static final TextPaint sContextPaint = new TextPaint();
    private static final Paint sContextBackgroundPaint = new Paint();

    private static int sContextHorizontalPadding;
    private static int sContextHorizontalSpacing;
    private static int sContextVerticalPadding;
    private static int sContextIconPadding;
    private static int sContextCornerRadius;
    
    private static Bitmap sSelectedIconOn;
    private static Bitmap sSelectedIconOff;
    private static Bitmap sStateInactive;
    private static Bitmap sStateDeleted;
    private static Bitmap sStateCompleted;

    private static String sContentsSnippetDivider;
    
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

    private static int DATE_TEXT_COLOR_INCOMPLETE;

    private int mViewWidth = 0;
    private int mViewHeight = 0;

    private static int sItemHeight;

    // Note: these cannot be shared Drawables because they are selectors which have state.
    private Drawable mCompleteSelector;
    private Drawable mIncompleteSelector;

    private CharSequence mFormattedProject;
    // We must initialize this to something, in case the timestamp of the message is zero (which
    // should be very rare); this is otherwise set in setTimestamp
    private CharSequence mFormattedDate = "";

    private void init(android.content.Context context) {
        mAndroidContext = context;
        
        if (!sInit) {
            sTextColours = TextColours.getInstance(context);
            Resources r = context.getResources();
            sContentsSnippetDivider = r.getString(R.string.task_list_contents_snippet_divider);
            sItemHeight =
                    r.getDimensionPixelSize(R.dimen.list_item_height);

            FontUtils.setCustomFont(sDefaultPaint, context.getAssets(), FontUtils.REGULAR);
            sDefaultPaint.setAntiAlias(true);
            FontUtils.setCustomFont(sDatePaint, context.getAssets(), FontUtils.REGULAR);
            sDatePaint.setAntiAlias(true);
            FontUtils.setCustomFont(sAllCapsPaint, context.getAssets(), FontUtils.REGULAR);
            sAllCapsPaint.setAntiAlias(true);
            sAllCapsPaint.setShadowLayer(0f, 1.0f, 1.0f, R.color.white);
            FontUtils.setCustomFont(sContextPaint, context.getAssets(), FontUtils.ALL_CAPS);
            sContextPaint.setAntiAlias(true);

            sContextHorizontalPadding = r.getDimensionPixelSize(R.dimen.context_small_horizontal_padding);
            sContextHorizontalSpacing = r.getDimensionPixelSize(R.dimen.context_small_horizontal_spacing);
            sContextVerticalPadding = r.getDimensionPixelSize(R.dimen.context_small_vertical_padding);
            sContextIconPadding = r.getDimensionPixelSize(R.dimen.context_small_icon_padding);
            sContextCornerRadius = r.getDimensionPixelSize(R.dimen.context_small_corner_radius);
            
            sSelectedIconOff =
                    BitmapFactory.decodeResource(r, R.drawable.ic_check_box_outline_blank_black_24dp);
            sSelectedIconOn =
                    BitmapFactory.decodeResource(r, R.drawable.ic_check_box_black_24dp);

            sStateInactive =
                    BitmapFactory.decodeResource(r, R.drawable.ic_pause_circle_filled_black_24dp);
            sStateDeleted =
                    BitmapFactory.decodeResource(r, R.drawable.ic_delete_black_24dp);
            sStateCompleted =
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
            DATE_TEXT_COLOR_INCOMPLETE = r.getColor(R.color.date_text_color_incomplete);

            sInit = true;
        }
    }
    
    private Bitmap getContextIcon(String iconName) {
        Bitmap icon = mContextIconMap.get(iconName);
        if (icon == null) {
            ContextIcon contextIcon = ContextIcon.createIcon(iconName, mAndroidContext.getResources());
            icon = BitmapFactory.decodeResource(mAndroidContext.getResources(), contextIcon.smallIconId);
            mContextIconMap.put(iconName, icon);
        }
        return icon;
    }

    /**
     * Invalidate all drawing caches associated with drawing message list items.
     * This is an expensive operation, and should be done rarely, such as when system font size
     * changes occurs.
     */
    public static void resetDrawingCaches() {
        TaskListItemCoordinates.resetCaches();
        sInit = false;
    }

    private int mOrder;

    private boolean mProjectNameVisible = true;

    public void setTask(Task task, boolean projectNameVisible) {
        mTaskId = task.getLocalId().getId();
        mProjectNameVisible = projectNameVisible;
        mIsCompleted = task.isComplete();
        mProject = mProjectCache.findById(task.getProjectId());
        mOrder = task.getOrder();
        List<Context> contexts = mContextCache.findById(task.getContextIds());
        mIsActive = TaskLifecycleState.getActiveStatus(task, contexts, mProject) == TaskLifecycleState.Status.yes;
        mIsDeleted = TaskLifecycleState.getDeletedStatus(task, mProject) != TaskLifecycleState.Status.no;
        
        setTimestamp(task.getDueDate());

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
        Collections.sort(contexts, Collections.reverseOrder());
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

        if (!Objects.equal(mSnippet, snippet)) {
            mSnippet = snippet;
            changed = true;
        }

        if (changed || (mDescription == null && mSnippet == null) /* first time */) {
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            boolean hasContents = false;
            if (!TextUtils.isEmpty(mDescription)) {
                SpannableString ss = new SpannableString(mDescription + " (" + mOrder + ")");
                ss.setSpan(new StyleSpan(mIsCompleted ? Typeface.NORMAL : Typeface.BOLD), 0, ss.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.append(ss);
                hasContents = true;
            }
            if (!TextUtils.isEmpty(mSnippet)) {
                if (hasContents) {
                    ssb.append(sContentsSnippetDivider);
                }
                String singleLine = stripLineEndings(mSnippet);
                ssb.append(singleLine);
            }
            mText = ssb;
            changed = true;
        }
        
        return changed;
    }

    long mDueMillis = 0L;
    private void setTimestamp(long timestamp) {
        if (mDueMillis != timestamp) {
            mFormattedDate = timestamp == 0L ? "" :
                    DateUtils.getRelativeTimeSpanString(mAndroidContext, timestamp).toString();
            mDueMillis = timestamp;
        }
    }

    private String stripLineEndings(String val) {
        return val.replace("\n", " ").replace("\r", "");
    }

    private boolean isDone() {
        return mIsCompleted || mIsDeleted;
    }

    private Drawable mCurrentBackground = null; // Only used by updateBackground()

    private void updateBackground() {
        final Drawable newBackground;
        if (isDone()) {
            if (mCompleteSelector == null) {
                mCompleteSelector = getContext().getResources()
                        .getDrawable(R.drawable.task_complete_selector);
            }
            newBackground = mCompleteSelector;
        } else {
            if (mIncompleteSelector == null) {
                mIncompleteSelector = getContext().getResources()
                        .getDrawable(R.drawable.task_incomplete_selector);
            }
            newBackground = mIncompleteSelector;
        }
        if (newBackground != mCurrentBackground) {
            // setBackgroundDrawable is a heavy operation.  Only call it when really needed.
            setBackgroundDrawable(newBackground);
            mCurrentBackground = newBackground;
        }
    }

    private void calculateContentsText() {
        if (mText == null || mText.length() == 0) {
            return;
        }
        int snippetStart = 0;
        if (!TextUtils.isEmpty(mDescription)) {
            int contentsColor = getFontColor(isDone() ? DESCRIPTION_TEXT_COLOR_COMPLETE
                    : DESCRIPTION_TEXT_COLOR_INCOMPLETE);
            mText.setSpan(new ForegroundColorSpan(contentsColor), 0, mDescription.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            snippetStart = mDescription.length() + 1;
        }
        if (!TextUtils.isEmpty(mSnippet)) {
            int snippetColor = getFontColor(isDone() ? SNIPPET_TEXT_COLOR_COMPLETE
                    : SNIPPET_TEXT_COLOR_INCOMPLETE);
            mText.setSpan(new ForegroundColorSpan(snippetColor), snippetStart, mText.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void calculateDrawingData() {
        sDefaultPaint.setTextSize(mCoordinates.contentsFontSize);
        calculateContentsText();
        mContentsLayout = new StaticLayout(mText, sDefaultPaint,
                mCoordinates.contentsWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, false /* includePad */);
        if (mCoordinates.contentsLineCount < mContentsLayout.getLineCount()) {
            int end = mContentsLayout.getLineEnd(mCoordinates.contentsLineCount - 1);
            mContentsLayout = new StaticLayout(mText.subSequence(0, end),
                    sDefaultPaint, mCoordinates.contentsWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, true);
        }

        // Date width first
        TextPaint datePaint = sDatePaint;
        datePaint.setTextSize(mCoordinates.dateFontSize);
        int dateWidth = (int)datePaint.measureText(mFormattedDate, 0, mFormattedDate.length()) +
                sContextHorizontalPadding;

        // Calculate the size the context wants to be

        // Mash together all the names to get an idea how big the text wants to be
        StringBuilder builder = new StringBuilder();
        for (Context context : mContexts) {
            builder.append(context.getName());
        }
        TextPaint contextPaint = sContextPaint;
        contextPaint.setTextSize(mCoordinates.contextsFontSize);
        int contextTextWidth = (int)contextPaint.measureText(builder.toString(), 0, builder.length());

        final int count = mContexts.size();
        int desiredContextWidth = contextTextWidth + // all the text
                count * mCoordinates.contextIconWidth + // each icon
                (count - 1) * sContextHorizontalSpacing + // between contexts
                2 * count * sContextHorizontalPadding +  // at each end of context
                count * sContextIconPadding; // between icon and text

        // And the project...
        TextPaint projectPaint = sAllCapsPaint;
        String projectName = (mProject == null || !mProjectNameVisible) ? "" : mProject.getName();
        projectPaint.setTextSize(mCoordinates.projectFontSize);
        projectPaint.setColor(getFontColor(isDone() ? PROJECT_TEXT_COLOR_COMPLETE
                : PROJECT_TEXT_COLOR_INCOMPLETE));
        int projectTextWidth = (int)projectPaint.measureText(projectName, 0, projectName.length());
        
        // if project and/or date needs less that it's given, give that to the context...
        int spareWidth = Math.max(0, mCoordinates.projectWidth - projectTextWidth) +
                Math.max(0, mCoordinates.dateWidth - dateWidth);
        int availableContextWidth = mCoordinates.contextsWidth + spareWidth;

        // if it fits, show context text
        mShowContextText = (availableContextWidth > desiredContextWidth);

        if (!mShowContextText) {
            desiredContextWidth = count * mCoordinates.contextIconWidth + // each icon
                    2 * count * sContextHorizontalPadding;  // at each end of icon
        }

        // give or take the difference in space from the project
        int projectWidth = mCoordinates.projectWidth + (availableContextWidth - desiredContextWidth);
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
        setSelected(mAdapter.isSelected(this));
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
        // Draw the checkbox
        canvas.drawBitmap(mAdapter.isSelected(this) ? sSelectedIconOn : sSelectedIconOff,
                mCoordinates.checkmarkX, mCoordinates.checkmarkY, null);

        // Draw the project name
        Paint projectPaint = sAllCapsPaint;
        projectPaint.setColor(getFontColor(isDone() ? PROJECT_TEXT_COLOR_COMPLETE
                : PROJECT_TEXT_COLOR_INCOMPLETE));
        projectPaint.setTextSize(mCoordinates.projectFontSize);
        canvas.drawText(mFormattedProject, 0, mFormattedProject.length(),
                mCoordinates.projectX, mCoordinates.projectY - mCoordinates.projectAscent,
                projectPaint);

        // Draw the task state. Most important being deleted, then complete, then inactive
        if (mIsDeleted) {
            canvas.drawBitmap(sStateDeleted,
                    mCoordinates.stateX, mCoordinates.stateY, null);
        } else if (mIsCompleted) {
            canvas.drawBitmap(sStateCompleted,
                    mCoordinates.stateX, mCoordinates.stateY, null);
        } else if (!mIsActive) {
            canvas.drawBitmap(sStateInactive,
                    mCoordinates.stateX, mCoordinates.stateY, null);
        }

        // Contents and snippet.
        sDefaultPaint.setTextSize(mCoordinates.contentsFontSize);
        canvas.save();
        canvas.translate(
                mCoordinates.contentsX,
                mCoordinates.contentsY);
        mContentsLayout.draw(canvas);
        canvas.restore();

        // Draw the date
        sDatePaint.setTextSize(mCoordinates.dateFontSize);
        sDatePaint.setColor(mIsCompleted ? DATE_TEXT_COLOR_COMPLETE : DATE_TEXT_COLOR_INCOMPLETE);
        int dateX = mCoordinates.dateXEnd
                - (int) sDatePaint.measureText(mFormattedDate, 0, mFormattedDate.length());

        canvas.drawText(mFormattedDate, 0, mFormattedDate.length(),
                dateX, mCoordinates.dateY - mCoordinates.dateAscent, sDatePaint);

        // Draw the contexts
        final int top = mCoordinates.contextsY - sContextVerticalPadding;
        final int bottom = mCoordinates.contextsY + mCoordinates.contextsHeight + sContextVerticalPadding;
        if (!mContexts.isEmpty()) {
            sContextPaint.setTextSize(mCoordinates.contextsFontSize);
            int right = dateX - sContextHorizontalSpacing;
            if (mShowContextText) {
                for (Context context : mContexts) {
                    sContextPaint.setColor(sTextColours.getTextColour(context.getColourIndex()));
                    final String name = context.getName();
                    int contextTextWidth = (int)sContextPaint.measureText(name, 0, name.length());
                    int textX = right - (sContextHorizontalPadding + contextTextWidth);
                    boolean hasIcon = !TextUtils.isEmpty(context.getIconName());
                    int left = textX - sContextHorizontalPadding;
                    if (hasIcon) {
                        left -= mCoordinates.contextIconWidth + sContextIconPadding;
                    }
                    RectF bgRect = new RectF(left, top, right, bottom);
                    int bgColor = sTextColours.getBackgroundColour(context.getColourIndex());
                    sContextBackgroundPaint.setShader(getShader(bgColor, bgRect));
                    canvas.drawRoundRect(bgRect, sContextCornerRadius, sContextCornerRadius, sContextBackgroundPaint);

                    if (hasIcon) {
                        Bitmap contextIcon = getContextIcon(context.getIconName());
                        canvas.drawBitmap(contextIcon, left + sContextIconPadding, mCoordinates.contextsY, null);
                    }

                    canvas.drawText(name, 0, name.length(),
                            textX, mCoordinates.contextsY - mCoordinates.contextsAscent,
                            sContextPaint);

                    right = left - sContextHorizontalSpacing;
                }
            } else {
                for (Context context : mContexts) {
                    int iconX = right - sContextHorizontalPadding - mCoordinates.contextIconWidth;
                    int left = iconX - sContextHorizontalPadding;

                    RectF bgRect = new RectF(left, top, right, bottom);
                    int bgColor = sTextColours.getBackgroundColour(context.getColourIndex());
                    sContextBackgroundPaint.setShader(getShader(bgColor, bgRect));
                    canvas.drawRect(bgRect, sContextBackgroundPaint);

                    Bitmap contextIcon = getContextIcon(context.getIconName());
                    canvas.drawBitmap(contextIcon, iconX, mCoordinates.contextsY, null);

                    right = left;
                }
            }
        }
    }
    
    private Shader getShader(int colour, RectF rect) {
        final float startOffset = 1.1f;
        final float endOffset = 0.9f;
        
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

    /**
     * Called by the adapter at bindView() time
     *
     * @param adapter the adapter that creates this view
     */
    public void bindViewInit(TaskListAdaptor adapter) {
        mAdapter = adapter;
        requestLayout();
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
        int checkRight = mCoordinates.checkmarkX
                + mCoordinates.checkmarkWidthIncludingMargins + sScaledTouchSlop;

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
                        mAdapter.toggleSelected(this);
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