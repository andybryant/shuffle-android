# Shuffle locations

I changed the main navigable locations in Shuffle. There intention should now be more obvious, and the change also let me simplify the task list settings configuration. Instead of showing a separate dialog with many options, the user now sees at most a single toggle switch in the toolbar.

# Location details

 * __Inbox__ - Where new tasks are added. Entries contain no project or tags. Sorted by user (default to most recently created at top)
 * __Top actions__ - Top incomplete action per active project. All active actions for projects in series and those with no project. Ordered by project (no proj last), then order within project.
 * __Due actions__ - Active undeleted actions with a defined due date. Ordered by due date, earliest at top.
 * __Projects__ - Lists undeleted projects. Clicking project shows actions for that project.
 * __Tags__ - Lists undeleted tags. Clicking project shows actions for that tag.
 * __Deferred__ - All active undeleted actions with start date in future.
 * __Deleted__ - All deleted projects, tags and actions.

# Configuration

## Features

 * __Quick add box__ - only available on Inbox
 * __Rearrange__ - (Not implemented yet) Inbox, Project, Tag, Project actions views
 * __Edit menu from list__ - Project, Tag
 * __Search__ - all views

## Settings

 * __Toggle completed__ - Inbox, Due actions, Project actions, Tag actions, Deferred, Inactive, Deleted
 * __Toggle active__ - Project, Tag

# Technical refactor

## Previous architecture 

Previously list specific configuration was spread amongst the following classes:

 * `Location` - Represents any view in the appliction, including all list views.
 * `TaskListContext` - Old representation of a location. Includes helper methods for creating title, `TaskSelector` and `ListSettings` and determing if move or edit actions should be shown.
 * `TaskSelector` - Represents logic query for the task list taking into account current list settings. Capable of generating SQL to represent the query. Constructed effectively from `TaskListContext` and `ListSettings`.
 * `ListSettings` - Which settings are available and default values for lists. Includes mechanism for fetching current settings from prefs.
 * `ListSettingsCache` - Static cache of ListSettings for all supported lists. Assigns availability and default values to each concrete `ListSettings` instance.

## Updated architecture

 * `TaskListContext` is redundant now we have `Location` so was deleted. Title helper methods moved to `TitleUpdater`. List helper methods moved to `LocationFeatures`
 * `ListSettings` - Simplified to only have to remaining settings - completed and active.
 * `ListSettingsCache` - updated with latest lists and default values.
 * `TaskSelector` - simplified to only support two remaining settings. Updated all inbuilt list queries.
 * `ListFeatures` - identifies which features are available for a given list location

