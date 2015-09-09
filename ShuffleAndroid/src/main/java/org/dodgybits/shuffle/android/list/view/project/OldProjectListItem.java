package org.dodgybits.shuffle.android.list.view.project;

import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.util.FontUtils;
import org.dodgybits.shuffle.android.core.view.TextColours;
import org.dodgybits.shuffle.android.list.view.StatusView;

public class OldProjectListItem extends LinearLayout {
    protected TextColours mTextColours;
    private ImageView mParallelIcon;
    private TextView mName;
    private TextView mCount;
    private StatusView mStatus;
    private SparseIntArray mTaskCountArray;

    @Inject
    public OldProjectListItem(android.content.Context context) {
        super(context);
        init(context);
    }

    public void init(android.content.Context androidContext) {
        LayoutInflater vi = (LayoutInflater)androidContext.
                getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup view = (ViewGroup) vi.inflate(R.layout.project_list_item, this, true);
        FontUtils.setCustomFont(view, androidContext.getAssets());

        mName = (TextView) findViewById(R.id.name);
        mCount = (TextView) findViewById(R.id.count);
        mStatus = (StatusView)findViewById(R.id.status);
//        mParallelIcon = (ImageView) findViewById(R.id.parallel_image);
        mTextColours = TextColours.getInstance(androidContext);
    }

    public void setTaskCountArray(SparseIntArray taskCountArray) {
        mTaskCountArray = taskCountArray;
    }

    public void updateView(Project project) {
        updateName(project);
        updateCount(project);
        updateStatus(project);
        updateParallelIcon(project);
    }

    private void updateName(Project project) {
        mName.setText(project.getName());
    }
    
    private void updateCount(Project project) {
        if (mTaskCountArray != null) {
            Integer count = mTaskCountArray.get((int)project.getLocalId().getId());
            if (count == null) count = 0;
            mCount.setText(String.valueOf(count));
        } else {
            mCount.setText("");
        }
    }

    private void updateStatus(Project project) {
        if (mStatus != null) {
            mStatus.updateStatus(project.isActive(), project.isDeleted(), false);
        }
    }

    private void updateParallelIcon(Project project) {
        if (mParallelIcon != null) {
            if (project.isParallel()) {
                mParallelIcon.setImageResource(R.drawable.parallel);
            } else {
                mParallelIcon.setImageResource(R.drawable.sequence);
            }
        }
    }

}
