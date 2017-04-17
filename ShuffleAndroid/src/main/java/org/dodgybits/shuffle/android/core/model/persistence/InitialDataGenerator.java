/*
 * Copyright (C) 2009 Android Shuffle Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dodgybits.shuffle.android.core.model.persistence;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;
import org.dodgybits.shuffle.sync.model.TaskChangeSet;
import roboguice.inject.ContentResolverProvider;
import roboguice.inject.ResourcesProvider;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class InitialDataGenerator {
	private static final String cTag = "InitialDataGenerator";

	private static final int AT_HOME_INDEX = 0;
	private static final int AT_WORK_INDEX = 1;
	private static final int AT_COMPUTER_INDEX = 2;
	private static final int ERRANDS_INDEX = 3;
	private static final int COMMUNICATION_INDEX = 4;
	private static final int READ_INDEX = 5;

	private Context[] mPresetContexts = null;

	private EntityPersister<Context> mContextPersister;
	private EntityPersister<Project> mProjectPersister;
	private EntityPersister<Task> mTaskPersister;
	private ContentResolver mContentResolver;
	private Resources mResources;
	
	@Inject 
    public InitialDataGenerator(EntityPersister<Context> contextPersister,
            EntityPersister<Project> projectPersister,
            EntityPersister<Task> taskPersister,
            ContentResolverProvider provider,
            ResourcesProvider resourcesProvider
    ) {
	    mContentResolver = provider.get();
	    mResources = resourcesProvider.get();
	    mContextPersister = contextPersister;
	    mProjectPersister = projectPersister;
	    mTaskPersister = taskPersister;
	    
        initPresetContexts();
    }

    public Context getSampleContext() {
        return mPresetContexts[ERRANDS_INDEX];
    }
    
    /**
     * Delete any existing projects, contexts and tasks and create the standard
     * contexts.
     * 
     * @param handler the android message handler
     */
    public void cleanSlate(Handler handler) {
        initPresetContexts();
        int deletedRows = mContentResolver.delete(
                TaskProvider.Tasks.CONTENT_URI, null, null);
        Log.d(cTag, "Deleted " + deletedRows + " tasks.");
        deletedRows = mContentResolver.delete(
                ProjectProvider.Projects.CONTENT_URI, null, null);
        Log.d(cTag, "Deleted " + deletedRows + " projects.");
        deletedRows = mContentResolver.delete(
                ContextProvider.Contexts.CONTENT_URI, null, null);
        Log.d(cTag, "Deleted " + deletedRows + " contexts.");
        for (int i = 0; i < mPresetContexts.length; i++) {
            mPresetContexts[i] = insertContext(mPresetContexts[i]);
        }
        if (handler != null)
            handler.sendEmptyMessage(0);
    }    
	
	/**
	 * Clean out the current data and populate the database with a set of sample
	 * data.
     * @param handler the message handler
     */
	public void createSampleData(Handler handler) {
		cleanSlate(null);
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		long now = cal.getTimeInMillis();
		cal.add(Calendar.DAY_OF_YEAR, -1);
		long yesterday = cal.getTimeInMillis();
		cal.add(Calendar.DAY_OF_YEAR, 3);
		long twoDays = cal.getTimeInMillis();
		cal.add(Calendar.DAY_OF_YEAR, 5);
		long oneWeek = cal.getTimeInMillis();
		cal.add(Calendar.WEEK_OF_YEAR, 1);
		long twoWeeks = cal.getTimeInMillis();
		
		Project sellBike = createProject("Sell old Powerbook", Id.NONE);
		sellBike = insertProject(sellBike);
		insertTask( 
				createTask("Backup data", null, 
						AT_COMPUTER_INDEX, sellBike, 
						now, now + DateUtils.HOUR_IN_MILLIS));
		insertTask( 
				createTask("Reformat HD", "Install Leopard and updates", 
						AT_COMPUTER_INDEX, sellBike, 
						twoDays, twoDays + 2 * DateUtils.HOUR_IN_MILLIS));
		insertTask( 
				createTask("Determine good price", "Take a look on ebay for similar systems", 
						AT_COMPUTER_INDEX, sellBike, 
						oneWeek, oneWeek + 2 * DateUtils.HOUR_IN_MILLIS));
		insertTask( 
				createTask("Put up ad", AT_COMPUTER_INDEX, sellBike, twoWeeks));

		Project cleanGarage = createProject("Clean out garage", Id.NONE);
		cleanGarage = insertProject(cleanGarage);
		insertTask( 
				createTask("Sort out contents", "Split into keepers and junk", 
						AT_HOME_INDEX, cleanGarage, 
						yesterday, yesterday));
		insertTask( 
				createTask("Advertise garage sale", "Local paper(s) and on craigslist", 
						new int[] {AT_HOME_INDEX, AT_COMPUTER_INDEX}, cleanGarage,
						oneWeek, oneWeek + 2 * DateUtils.HOUR_IN_MILLIS));
		insertTask( 
				createTask("Contact local charities", "See what they want or maybe just put in charity bins", 
						COMMUNICATION_INDEX, cleanGarage, 
						now, now));
		insertTask( 
				createTask("Take rest to tip", "Hire trailer?", 
						ERRANDS_INDEX, cleanGarage, 
						now, now));

		Project skiTrip = createProject("Organise ski trip", Id.NONE);
		skiTrip = insertProject(skiTrip);
		insertTask( 
				createTask("Send email to determine best week", null, 
						new int[] {AT_COMPUTER_INDEX, COMMUNICATION_INDEX}, skiTrip, 
						now, now + 2 * DateUtils.HOUR_IN_MILLIS));
		insertTask( 
				createTask("Look up package deals", 
						AT_COMPUTER_INDEX, skiTrip, 0L));
		insertTask( 
				createTask("Book chalet", 
						AT_COMPUTER_INDEX, skiTrip, 0L)); 
		insertTask( 
				createTask("Book flights", 
						AT_COMPUTER_INDEX, skiTrip, 0L));
		insertTask( 
				createTask("Book hire car", 
						AT_COMPUTER_INDEX, skiTrip, 0L));
		insertTask( 
				createTask("Get board waxed", 
						ERRANDS_INDEX, skiTrip, 0L));

		Project discussI8n = createProject("Discuss internationalization", mPresetContexts[AT_WORK_INDEX].getLocalId());
		discussI8n = insertProject(discussI8n);
		insertTask( 
				createTask("Read up on options", null, 
						AT_COMPUTER_INDEX, discussI8n, 
						twoDays, twoDays + 2 * DateUtils.HOUR_IN_MILLIS));
		insertTask( 
				createTask("Kickoff meeting", null, 
						COMMUNICATION_INDEX, discussI8n, 
						oneWeek, oneWeek + 2 * DateUtils.HOUR_IN_MILLIS));
		insertTask( 
				createTask("Produce report", null, 
						new int[] {AT_WORK_INDEX, AT_COMPUTER_INDEX}, discussI8n, 
						twoWeeks, twoWeeks + 2 * DateUtils.HOUR_IN_MILLIS));

		// a few stand alone tasks
		insertTask( 
				createTask("Organise music collection", 
						AT_COMPUTER_INDEX, null, 0L));
		insertTask( 
				createTask("Make copy of door keys", 
						ERRANDS_INDEX, null, yesterday));
		insertTask( 
				createTask("Read Falling Man", 
						READ_INDEX, null, 0L));
		insertTask( 
				createTask("Buy Tufte books", 
						ERRANDS_INDEX, null, oneWeek));
		if (handler != null)
			handler.sendEmptyMessage(0);
	}
	
    
    private void initPresetContexts() {
        if (mPresetContexts == null) {
            mPresetContexts = new Context[] {
                    createContext(mResources.getText(R.string.context_athome).toString(), 5, "go_home"), // 0
                    createContext(mResources.getText(R.string.context_atwork).toString(), 19, "system_file_manager"), // 1
                    createContext(mResources.getText(R.string.context_online).toString(), 1, "applications_internet"), // 2
                    createContext(mResources.getText(R.string.context_errands).toString(), 14, "applications_development"), // 3
                    createContext(mResources.getText(R.string.context_contact).toString(), 22, "system_users"), // 4
                    createContext(mResources.getText(R.string.context_read).toString(), 16, "format_justify_fill") // 5
            };
        }
    }

    private Context createContext(String name, int colourIndex, String iconName) {
		long created = System.currentTimeMillis();
        Context.Builder builder = Context.newBuilder();
        builder
            .setName(name)
            .setActive(true)
            .setDeleted(false)
			.setModifiedDate(created)
            .setColourIndex(colourIndex)
            .setIconName(iconName);
        return builder.build();
    }

    private Task createTask(String description, int contextIndex, Project project, long start) {
        return createTask(description, null, contextIndex, project, start, start);
    }

	private Task createTask(String description, int[] contextIndices, Project project, long start) {
		return createTask(description, null, contextIndices, project, start, start);
	}

    private Task createTask(String description, String details,
                            int contextIndex, Project project, long start, long due) {
        return createTask(description, details, new int[] {contextIndex}, project, start, due);
    }
    
	private int ORDER = 1;
	
	private Task createTask(String description, String details, 
			int[] contextIndices, Project project, long start, long due) {
		long created = System.currentTimeMillis();
        String timezone = TimeZone.getDefault().getID();

		Task.Builder builder = Task.newBuilder();
		builder
		    .setDescription(description)
		    .setDetails(details)
		    .setProjectId(project == null ? Id.NONE : project.getLocalId())
		    .setCreatedDate(created)
		    .setModifiedDate(created)
		    .setStartDate(start)
		    .setDueDate(due)
		    .setTimezone(timezone)
            .setActive(true)
            .setDeleted(false)
		    .setOrder(ORDER++);

        List<Id> contextIds = Lists.newArrayList();
        for (int contextIndex : contextIndices) {
            Id contextId = mPresetContexts[contextIndex].getLocalId();
            contextIds.add(contextId);
        }
        builder.setContextIds(contextIds);
        
		return builder.build();
	}
	
    private Project createProject(String name, Id defaultContextId) {
        Project.Builder builder = Project.newBuilder();
        builder
            .setName(name)
            .setActive(true)
            .setDeleted(false)
            .setDefaultContextId(defaultContextId)
            .setModifiedDate(System.currentTimeMillis());
        return builder.build();
    }

	private Context insertContext(
			org.dodgybits.shuffle.android.core.model.Context context) {
		Uri uri = mContextPersister.insert(context);
		long id = ContentUris.parseId(uri);
		Log.d(cTag, "Created context id=" + id + " uri=" + uri);
		Context.Builder builder = Context.newBuilder();
		builder.mergeFrom(context);
		builder.setLocalId(Id.create(id));
		context = builder.build();
		return context;
	}

	private Project insertProject(
			Project project) {
		Uri uri = mProjectPersister.insert(project);
		long id = ContentUris.parseId(uri);
		Log.d(cTag, "Created project id=" + id + " uri=" + uri);
		Project.Builder builder = Project.newBuilder();
		builder.mergeFrom(project);
		builder.setLocalId(Id.create(id));
		project = builder.build();
		return project;
	}

	private Task insertTask(
			Task task) {
		Uri uri = mTaskPersister.insert(task);
		long id = ContentUris.parseId(uri);
		Log.d(cTag, "Created task id=" + id);
		Task.Builder builder = Task.newBuilder();
		builder.mergeFrom(task);
		builder.setLocalId(Id.create(id));
		task = builder.build();
		return task;
	}

}