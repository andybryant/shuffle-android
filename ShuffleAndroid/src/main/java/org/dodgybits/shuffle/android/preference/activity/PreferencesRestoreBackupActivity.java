package org.dodgybits.shuffle.android.preference.activity;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.EntityPersister;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.model.protocol.*;
import org.dodgybits.shuffle.android.core.util.AnalyticsUtils;
import org.dodgybits.shuffle.android.core.util.StringUtils;
import org.dodgybits.shuffle.android.core.view.AlertUtils;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.android.preference.view.Progress;
import org.dodgybits.shuffle.android.server.sync.SyncUtils;
import org.dodgybits.shuffle.dto.ShuffleProtos.Catalogue;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.*;

import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.LOCAL_CHANGE_SOURCE;

public class PreferencesRestoreBackupActivity extends RoboActivity
	implements View.OnClickListener {
    private static final String RESTORE_BACKUP_STATE = "restoreBackupState";
    private static final String cTag = "PrefRestoreBackup";
    
    private enum State {SELECTING, IN_PROGRESS, COMPLETE, ERROR};
    
    private State mState = State.SELECTING;
    @InjectView(R.id.filename) Spinner mFileSpinner;
    @InjectView(R.id.action_done) Button mRestoreButton;
    @InjectView(R.id.action_cancel) Button mCancelButton;
    @InjectView(R.id.progress_horizontal) ProgressBar mProgressBar;
    @InjectView(R.id.progress_label) TextView mProgressText;
    
    @Inject
    EntityPersister<Context> mContextPersister;
    @Inject
    EntityPersister<Project> mProjectPersister;
    @Inject
    TaskPersister mTaskPersister;
    
    private AsyncTask<?, ?, ?> mTask;
    
    @Override
    protected void onCreate(Bundle icicle) {
        Log.d(cTag, "onCreate+");
        super.onCreate(icicle);
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        setContentView(R.layout.backup_restore);
        findViewsAndAddListeners();
		onUpdateState();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
        setupFileSpinner();
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
        mRestoreButton.setText(R.string.restore_button_title);
        
        mRestoreButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        
        // save progress text when we switch orientation
        mProgressText.setFreezesText(true);
    }
    
    private void setupFileSpinner() {
    	String storage_state = Environment.getExternalStorageState();
    	if (! Environment.MEDIA_MOUNTED.equals(storage_state)) {
    		String message = getString(R.string.warning_media_not_mounted, storage_state);
    		Log.e(cTag, message);
    		AlertUtils.showWarning(this, message);
			setState(State.COMPLETE);
			return;
    	}
    	
		File dir = Environment.getExternalStorageDirectory();
    	String[] files = dir.list(new FilenameFilter() {
    		@Override
    		public boolean accept(File dir, String filename) {
    			// don't show hidden files
    			return !filename.startsWith(".");
    		}
    	});
    	
    	if (files == null || files.length == 0) {
    		String message = getString(R.string.warning_no_files, storage_state);
    		Log.e(cTag, message);
    		AlertUtils.showWarning(this, message);
			setState(State.COMPLETE);
			return;
    	}
    	
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
        		this, android.R.layout.simple_list_item_1, files);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFileSpinner.setAdapter(adapter);    	

    	// select most recent file ending in .bak
    	int selectedIndex = 0;
    	long lastModified = Long.MIN_VALUE;
    	for (int i = 0; i < files.length; i++) {
    		String filename = files[i];
    		File f = new File(dir, filename);
    		if (f.getName().endsWith(".bak") &&
    				f.lastModified() > lastModified) {
    			selectedIndex = i;
    			lastModified = f.lastModified();
    		}
    	}
    	mFileSpinner.setSelection(selectedIndex);
    }

    
    
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.action_done:
            	setState(State.IN_PROGRESS);
            	restoreBackup();
                break;

            case R.id.action_cancel:
            	finish();
                break;
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	
    	outState.putString(RESTORE_BACKUP_STATE, mState.name());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	
    	String stateName = savedInstanceState.getString(RESTORE_BACKUP_STATE);
    	if (stateName == null) {
    		stateName = State.SELECTING.name();
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
	    	case SELECTING:
	    		setButtonsEnabled(true);
	    		mFileSpinner.setEnabled(true);
	            mProgressBar.setVisibility(View.INVISIBLE);
	            mProgressText.setVisibility(View.INVISIBLE);
	            mCancelButton.setText(R.string.cancel_button_title);
	    		break;
	    		
	    	case IN_PROGRESS:
	    		setButtonsEnabled(false);
	        	mFileSpinner.setEnabled(false);
	        	
	        	mProgressBar.setProgress(0);
		        mProgressBar.setVisibility(View.VISIBLE);
	            mProgressText.setVisibility(View.VISIBLE);
	    		break;
	    		
	    	case COMPLETE:
	    		setButtonsEnabled(true);
	    		mFileSpinner.setEnabled(false);
		        mProgressBar.setVisibility(View.VISIBLE);
	            mProgressText.setVisibility(View.VISIBLE);
	        	mRestoreButton.setVisibility(View.GONE);
	        	mCancelButton.setText(R.string.ok_button_title);
	    		break;
	    		
	    	case ERROR:
	    		setButtonsEnabled(true);
	    		mFileSpinner.setEnabled(true);
		        mProgressBar.setVisibility(View.VISIBLE);
	            mProgressText.setVisibility(View.VISIBLE);
	        	mRestoreButton.setVisibility(View.VISIBLE);
	            mCancelButton.setText(R.string.cancel_button_title);
	    		break;	
    	}
    }

    private void setButtonsEnabled(boolean enabled) {
    	mRestoreButton.setEnabled(enabled);
    	mCancelButton.setEnabled(enabled);
    }    
    
    private void restoreBackup() {
		String filename = mFileSpinner.getSelectedItem().toString();
		mTask = new RestoreBackupTask().execute(filename);
    }
    
    private class RestoreBackupTask extends AsyncTask<String, Progress, Void> {

    	public Void doInBackground(String... filename) {
            try {
            	String message = getString(R.string.status_reading_backup);
				Log.d(cTag, message);
            	publishProgress(Progress.createProgress(5, message));
            	
        		File dir = Environment.getExternalStorageDirectory();
        		File backupFile = new File(dir, filename[0]);
    			FileInputStream in = new FileInputStream(backupFile);
    			Catalogue catalogue = Catalogue.parseFrom(in);
    			in.close();
    			
    			if (Log.isLoggable(cTag, Log.DEBUG)) {
        			Log.d(cTag, catalogue.toString());
    			}
    			
    			EntityDirectory<Context> contextLocator = addContexts(catalogue.getContextList(), 10, 20);
    			EntityDirectory<Project> projectLocator = addProjects(catalogue.getProjectList(), contextLocator, 20, 30);
    			addTasks(catalogue.getTaskList(), contextLocator, projectLocator, 30, 100);
    			
            	message = getString(R.string.status_restore_complete);
            	publishProgress(Progress.createProgress(100, message));
            } catch (Exception e) {
            	String message = getString(R.string.warning_restore_failed, e.getMessage());
        		reportError(message);
            }
            
            return null;
        }
    	
    	private EntityDirectory<Context> addContexts(
				List<org.dodgybits.shuffle.dto.ShuffleProtos.Context> protoContexts,
				int progressStart, int progressEnd) {
            ContextProtocolTranslator translator = new ContextProtocolTranslator();
    	    
			Set<String> allContextNames = new HashSet<String>();
			for (org.dodgybits.shuffle.dto.ShuffleProtos.Context protoContext : protoContexts)
			{
				allContextNames.add(protoContext.getName());
			}
			Map<String,Context> existingContexts = fetchContextsByName(allContextNames);
			
			// build up the locator and list of new contacts
			HashEntityDirectory<Context> contextLocator = new HashEntityDirectory<Context>();
			List<Context> newContexts = new ArrayList<Context>();
			Set<String> newContextNames = new HashSet<String>();
	        int i = 0;
	        int total = protoContexts.size();
	        String type = getString(R.string.context_name);
	        
			for (org.dodgybits.shuffle.dto.ShuffleProtos.Context protoContext : protoContexts)
			{
				String contextName = protoContext.getName();
				Context context = existingContexts.get(contextName);
				if (context != null) {
					Log.d(cTag, "Context " + contextName + " already exists - skipping.");
				} else {
					Log.d(cTag, "Context " + contextName + " new - adding.");
					context = translator.fromMessage(protoContext);
					
					newContexts.add(context);
					newContextNames.add(contextName);
				}
				Id contextId = Id.create(protoContext.getId());
				contextLocator.addItem(contextId, contextName, context);
				String text = getString(R.string.restore_progress, type, contextName);
				int percent = calculatePercent(progressStart, progressEnd, ++i, total);
            	publishProgress(Progress.createProgress(percent, text));
			}
            mContextPersister.bulkInsert(newContexts);
			
			// we need to fetch all the newly created contexts to retrieve their new ids
			// and update the locator accordingly
			Map<String,Context> savedContexts = fetchContextsByName(newContextNames);
			for (String contextName : newContextNames) {
				Context savedContext = savedContexts.get(contextName);
				Context restoredContext = contextLocator.findByName(contextName);
				contextLocator.addItem(restoredContext.getLocalId(), contextName, savedContext);
			}
			
			return contextLocator;
		}
	    
        /**
         * Attempts to match existing contexts against a list of context names.
         *
         * @param names  names to match
         * @return any matching contexts in a Map, keyed on the context name
         */
        private Map<String,Context> fetchContextsByName(Collection<String> names) {
            Map<String,Context> contexts = new HashMap<String,Context>();
            if (names.size() > 0)
            {
                String params = StringUtils.repeat(names.size(), "?", ",");
                String[] paramValues = names.toArray(new String[0]);
                Cursor cursor = getContentResolver().query(
                        ContextProvider.Contexts.CONTENT_URI,
                        ContextProvider.Contexts.FULL_PROJECTION,
                        ContextProvider.Contexts.NAME + " IN (" + params + ")",
                        paramValues, ContextProvider.Contexts.NAME + " ASC");
                while (cursor.moveToNext()) {
                    Context context = mContextPersister.read(cursor);
                    contexts.put(context.getName(), context);
                }
                cursor.close();
            }
            return contexts;
        }
        
		private EntityDirectory<Project> addProjects(
				List<org.dodgybits.shuffle.dto.ShuffleProtos.Project> protoProjects,
				EntityDirectory<Context> contextLocator,
				int progressStart, int progressEnd) {
            ProjectProtocolTranslator translator = new ProjectProtocolTranslator(contextLocator);
            
			Set<String> allProjectNames = new HashSet<String>();
			for (org.dodgybits.shuffle.dto.ShuffleProtos.Project protoProject : protoProjects)
			{
				allProjectNames.add(protoProject.getName());
			}
			Map<String,Project> existingProjects = fetchProjectsByName(allProjectNames);
			
			// build up the locator and list of new projects
			HashEntityDirectory<Project> projectLocator = new HashEntityDirectory<Project>();
			List<Project> newProjects = new ArrayList<Project>();
			Set<String> newProjectNames = new HashSet<String>();
	        int i = 0;
	        int total = protoProjects.size();
	        String type = getString(R.string.project_name);
	        
			for (org.dodgybits.shuffle.dto.ShuffleProtos.Project protoProject : protoProjects)
			{
				String projectName = protoProject.getName();
				Project project = existingProjects.get(projectName);
				if (project != null) {
					Log.d(cTag, "Project " + projectName + " already exists - skipping.");
				} else {
					Log.d(cTag, "Project " + projectName + " new - adding.");
					project = translator.fromMessage(protoProject);

					newProjects.add(project);
					newProjectNames.add(projectName);
				}
				Id projectId = Id.create(protoProject.getId());
				projectLocator.addItem(projectId, projectName, project);
				String text = getString(R.string.restore_progress, type, projectName);
				int percent = calculatePercent(progressStart, progressEnd, ++i, total);
            	publishProgress(Progress.createProgress(percent, text));
			}
            mProjectPersister.bulkInsert(newProjects);

			// we need to fetch all the newly created contexts to retrieve their new ids
			// and update the locator accordingly
			Map<String,Project> savedProjects = fetchProjectsByName(newProjectNames);
			for (String projectName : newProjectNames) {
				Project savedProject = savedProjects.get(projectName);
				Project restoredProject = projectLocator.findByName(projectName);
				projectLocator.addItem(restoredProject.getLocalId(), projectName, savedProject);
			}
			
			return projectLocator;
		}
		
	    /**
	     * Attempts to match existing contexts against a list of context names.
	     *
	     * @return any matching contexts in a Map, keyed on the context name
	     */
	    private Map<String,Project> fetchProjectsByName(Collection<String> names) {
	        Map<String,Project> projects = new HashMap<String,Project>();
	        if (names.size() > 0)
	        {
	            String params = StringUtils.repeat(names.size(), "?", ",");
	            String[] paramValues = names.toArray(new String[0]);
	            Cursor cursor = getContentResolver().query(
	                    ProjectProvider.Projects.CONTENT_URI,
	                    ProjectProvider.Projects.FULL_PROJECTION,
	                    ProjectProvider.Projects.NAME + " IN (" + params + ")",
	                    paramValues, ProjectProvider.Projects.NAME + " ASC");
	            while (cursor.moveToNext()) {
	                Project project = mProjectPersister.read(cursor);
	                projects.put(project.getName(), project);
	            }
	            cursor.close();
	        }
	        return projects;
	    }
		
		private void addTasks(
				List<org.dodgybits.shuffle.dto.ShuffleProtos.Task> protoTasks,
				EntityDirectory<Context> contextLocator,
				EntityDirectory<Project> projectLocator,
				int progressStart, int progressEnd) {
            TaskProtocolTranslator translator = new TaskProtocolTranslator(contextLocator, projectLocator);
		    
			// add all tasks back, even if they're duplicates
			
            Set<Id> projectIds = Sets.newHashSet();
	        String type = getString(R.string.task_name);
			List<Task> newTasks = new ArrayList<Task>();
	        int i = 0;
	        int total = protoTasks.size();
			for (org.dodgybits.shuffle.dto.ShuffleProtos.Task protoTask : protoTasks)
			{
			    Task task = translator.fromMessage(protoTask);
				newTasks.add(task);
                if (task.getProjectId().isInitialised()) {
                    projectIds.add(task.getProjectId());
                }
                
				Log.d(cTag, "Adding task " + task.getDescription());
				String text = getString(R.string.restore_progress, type, task.getDescription());
				int percent = calculatePercent(progressStart, progressEnd, ++i, total);
            	publishProgress(Progress.createProgress(percent, text));
			}
			mTaskPersister.bulkInsert(newTasks);

            // reset project task orders for all affected projects in case they have changed
            mTaskPersister.reorderProjects(projectIds);
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
                    AlertUtils.showWarning(PreferencesRestoreBackupActivity.this, details);
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
            if (mState == State.COMPLETE) {
                SyncUtils.scheduleSync(PreferencesRestoreBackupActivity.this, LOCAL_CHANGE_SOURCE);
            }
        }
    	
    }    
        

	

}
