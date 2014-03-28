package org.dodgybits.shuffle.android.preference.activity;

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.EntityPersister;
import org.dodgybits.shuffle.android.core.model.protocol.ContextProtocolTranslator;
import org.dodgybits.shuffle.android.core.model.protocol.ProjectProtocolTranslator;
import org.dodgybits.shuffle.android.core.model.protocol.TaskProtocolTranslator;
import org.dodgybits.shuffle.android.core.util.AnalyticsUtils;
import org.dodgybits.shuffle.android.core.view.AlertUtils;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;
import org.dodgybits.shuffle.android.preference.view.Progress;
import org.dodgybits.shuffle.dto.ShuffleProtos.Catalogue;
import org.dodgybits.shuffle.dto.ShuffleProtos.Catalogue.Builder;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PreferencesCreateBackupActivity extends RoboActivity
	implements View.OnClickListener {
    private static final String CREATE_BACKUP_STATE = "createBackupState";
	private static final String cTag = "PreferencesCreateBackupActivity";
    
    private enum State {EDITING, IN_PROGRESS, COMPLETE, ERROR};
    
    private State mState = State.EDITING;
    @InjectView(R.id.filename) EditText mFilenameWidget;
    @InjectView(R.id.action_done) Button mSaveButton;
    @InjectView(R.id.action_cancel) Button mCancelButton;
    @InjectView(R.id.progress_horizontal) ProgressBar mProgressBar;
    @InjectView(R.id.progress_label) TextView mProgressText;
    
    @Inject EntityPersister<Context> mContextPersister;
    @Inject EntityPersister<Project> mProjectPersister;
    @Inject EntityPersister<Task> mTaskPersister;
    
    private AsyncTask<?, ?, ?> mTask;
    
    @Override
    protected void onCreate(Bundle icicle) {
        Log.d(cTag, "onCreate+");
        super.onCreate(icicle);
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        setContentView(R.layout.backup_create);
        findViewsAndAddListeners();
		onUpdateState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AnalyticsUtils.activityStop(this);
    }

    private void findViewsAndAddListeners() {
        
        mSaveButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        
        // save progress text when we switch orientation
        mProgressText.setFreezesText(true);
    }
    
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.action_done:
            	setState(State.IN_PROGRESS);
            	createBackup();
                break;

            case R.id.action_cancel:
            	finish();
                break;
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	
    	outState.putString(CREATE_BACKUP_STATE, mState.name());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	
    	String stateName = savedInstanceState.getString(CREATE_BACKUP_STATE);
    	if (stateName == null) {
    		stateName = State.EDITING.name();
    	}
    	setState(State.valueOf(stateName));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.RUNNING) {
            mTask.cancel(true);
        }
    }
    
    private void setState(State value) {
    	if (mState != value) {
    		mState = value;
    		onUpdateState();
    	}
    }
    
    private void onUpdateState() {
    	switch (mState) {
	    	case EDITING:
	    		setButtonsEnabled(true);
	    		if (TextUtils.isEmpty(mFilenameWidget.getText())) {
		            Date today = new Date();
		            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		            String defaultText = "shuffle-" + formatter.format(today) + ".bak";
		            mFilenameWidget.setText(defaultText);
		            mFilenameWidget.setSelection(0, defaultText.length() - 4);
	    		}    		
	        	mFilenameWidget.setEnabled(true);
	            mProgressBar.setVisibility(View.INVISIBLE);
	            mProgressText.setVisibility(View.INVISIBLE);
	            mCancelButton.setText(R.string.cancel_button_title);
	    		break;
	    		
	    	case IN_PROGRESS:
	    		setButtonsEnabled(false);
	        	mFilenameWidget.setSelection(0, 0);
	        	mFilenameWidget.setEnabled(false);
	        	
	        	mProgressBar.setProgress(0);
		        mProgressBar.setVisibility(View.VISIBLE);
	            mProgressText.setVisibility(View.VISIBLE);
	    		break;
	    		
	    	case COMPLETE:
	    		setButtonsEnabled(true);
	        	mFilenameWidget.setEnabled(false);
		        mProgressBar.setVisibility(View.VISIBLE);
	            mProgressText.setVisibility(View.VISIBLE);
	        	mSaveButton.setVisibility(View.GONE);
	        	mCancelButton.setText(R.string.ok_button_title);
	    		break;
	    		
	    	case ERROR:
	    		setButtonsEnabled(true);
	        	mFilenameWidget.setEnabled(true);
		        mProgressBar.setVisibility(View.VISIBLE);
	            mProgressText.setVisibility(View.VISIBLE);
	        	mSaveButton.setVisibility(View.VISIBLE);
	            mCancelButton.setText(R.string.cancel_button_title);
	    		break;	
    	}
    }

    private void setButtonsEnabled(boolean enabled) {
    	mSaveButton.setEnabled(enabled);
    	mCancelButton.setEnabled(enabled);
    }
    
    private void createBackup() {
    	String filename = mFilenameWidget.getText().toString();
    	if (TextUtils.isEmpty(filename)) {
    		String message = getString(R.string.warning_filename_empty);
			Log.e(cTag, message);
    		AlertUtils.showWarning(this, message);
			setState(State.EDITING);
    	} else {
    		mTask = new CreateBackupTask().execute(filename);
    	}
    }
    
    private class CreateBackupTask extends AsyncTask<String, Progress, Void> {

    	public Void doInBackground(String... filename) {
            try {
            	String message = getString(R.string.status_checking_media);
				Log.d(cTag, message);
            	publishProgress(Progress.createProgress(0, message));

            	String storage_state = Environment.getExternalStorageState();
            	if (! Environment.MEDIA_MOUNTED.equals(storage_state)) {
            		message = getString(R.string.warning_media_not_mounted, storage_state);
            		reportError(message);
            	} else {
	        		File dir = Environment.getExternalStorageDirectory();
	        		final File backupFile = new File(dir, filename[0]);
	        		message = getString(R.string.status_creating_backup);
        	    	Log.d(cTag, message);
                	publishProgress(Progress.createProgress(5, message));
                	
                	if (backupFile.exists()) {
                    	publishProgress(Progress.createErrorProgress("", new Runnable() {
                    		@Override
                    		public void run() {
                    			showFileExistsWarning(backupFile);
                    		}
                    	}));
                	} else {
	        			backupFile.createNewFile();
	        			FileOutputStream out = new FileOutputStream(backupFile);
	                	writeBackup(out);
                	}
        		}
            } catch (Exception e) {
            	String message = getString(R.string.warning_backup_failed, e.getMessage());
        		reportError(message);
            }
            
            return null;
        }
    	
    	private void showFileExistsWarning(final File backupFile) {
    		OnClickListener buttonListener = new OnClickListener() {
    			public void onClick(DialogInterface dialog, int which) {
    				if (which == DialogInterface.BUTTON1) {
    			    	Log.i(cTag, "Overwriting file " + backupFile.getName());
    		            try {
	            			FileOutputStream out = new FileOutputStream(backupFile);
	                    	writeBackup(out);
    		            } catch (Exception e) {
    		            	String message = getString(R.string.warning_backup_failed, e.getMessage());
    		        		reportError(message);
    		            }
    				} else {
    					Log.d(cTag, "Hit Cancel button.");
    					setState(State.EDITING);
    				}
    			}
    		};
    		OnCancelListener cancelListener = new OnCancelListener() {
    			public void onCancel(DialogInterface dialog) {
					Log.d(cTag, "Hit Cancel button.");
					setState(State.EDITING);
    			}
    		};
    		
			AlertUtils.showFileExistsWarning(PreferencesCreateBackupActivity.this, 
					backupFile.getName(), buttonListener, cancelListener);
    		
    	}
    	
        private void writeBackup(OutputStream out) throws IOException {
        	Builder builder = Catalogue.newBuilder();
        	
        	writeContexts(builder, 10, 20);
        	writeProjects(builder, 20, 30);
        	writeTasks(builder, 30, 100);

        	builder.build().writeTo(out);
        	out.close();

        	String message = getString(R.string.status_backup_complete);
			Progress progress = Progress.createProgress(100, message);
        	publishProgress(progress);
        	
        }
        
        private void writeContexts(Builder builder, int progressStart, int progressEnd)
        {
	    	Log.d(cTag, "Writing contexts");
            Cursor cursor = getContentResolver().query(
            		ContextProvider.Contexts.CONTENT_URI, ContextProvider.Contexts.FULL_PROJECTION, 
            		null, null, null);
            int i = 0;
            int total = cursor.getCount();
            String type = getString(R.string.context_name);
            ContextProtocolTranslator translator = new ContextProtocolTranslator();
        	while (cursor.moveToNext()) {
        	    Context context = mContextPersister.read(cursor);
            	builder.addContext(translator.toMessage(context));
    			String text = getString(R.string.backup_progress, type, context.getName());
    			int percent = calculatePercent(progressStart, progressEnd, ++i, total);
            	publishProgress(Progress.createProgress(percent, text));
        	}
        	cursor.close();
        }

        private void writeProjects(Builder builder, int progressStart, int progressEnd)
        {
	    	Log.d(cTag, "Writing projects");
            Cursor cursor = getContentResolver().query(
            		ProjectProvider.Projects.CONTENT_URI, ProjectProvider.Projects.FULL_PROJECTION, 
            		null, null, null);
            int i = 0;
            int total = cursor.getCount();
            String type = getString(R.string.project_name);
            ProjectProtocolTranslator translator = new ProjectProtocolTranslator(null);
        	while (cursor.moveToNext()) {
        		Project project = mProjectPersister.read(cursor);
            	builder.addProject(translator.toMessage(project));
    			String text = getString(R.string.backup_progress, type, project.getName());
    			int percent = calculatePercent(progressStart, progressEnd, ++i, total);
            	publishProgress(Progress.createProgress(percent, text));
        	}
        	cursor.close();
        }
        
        private void writeTasks(Builder builder, int progressStart, int progressEnd)
        {
	    	Log.d(cTag, "Writing tasks");
            Cursor cursor = getContentResolver().query(
            		TaskProvider.Tasks.CONTENT_URI, TaskProvider.Tasks.FULL_PROJECTION, 
            		null, null, null);
            int i = 0;
            int total = cursor.getCount();
            String type = getString(R.string.task_name);
            TaskProtocolTranslator translator = new TaskProtocolTranslator(null, null);
        	while (cursor.moveToNext()) {
        		Task task = mTaskPersister.read(cursor);
            	builder.addTask(translator.toMessage(task));
    			String text = getString(R.string.backup_progress, type, task.getDescription());
    			int percent = calculatePercent(progressStart, progressEnd, ++i, total);
            	publishProgress(Progress.createProgress(percent, text));
        	}
        	cursor.close();
        }
        
        private int calculatePercent(int start, int end, int current, int total) {
        	return start + (end - start) * current / total;
        }
        
        private void reportError(String message) {
			Log.e(cTag, message);
        	publishProgress(Progress.createErrorProgress(message));
        }
                
		@Override
		public void onProgressUpdate (Progress... progresses) {
		    Progress progress = progresses[0];
            String details = progress.getDetails();
	        mProgressBar.setProgress(progress.getProgressPercent());
	        mProgressText.setText(details);

	        if (progress.isError()) {
	            if (!TextUtils.isEmpty(details)) {
	                AlertUtils.showWarning(PreferencesCreateBackupActivity.this, details);
	            }
        		Runnable action = progress.getErrorUIAction();
	        	if (action != null) {
	        		action.run();
	        	} else {
		        	setState(State.ERROR);
	        	}
	        } else if (progress.isComplete()) {
	        	setState(State.COMPLETE);
	        }
		}
		
        @SuppressWarnings("unused")
        public void onPostExecute() {
            mTask = null;
        }
    	
    }
    
}
