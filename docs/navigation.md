# shuffle-android Navigation

## Introduction

This document describes the different architectural options for handling navigation in Shuffle. The latest version of Shuffle supports both phones and tablets. For the later, it supports screens with both task lists and individual tasks displayed at the same time. I started off by modeling the approach around the [UnifiedMail](https://android.googlesource.com/platform/packages/apps/UnifiedEmail/) app. However instead of using the monolithic controller approach, I went with using RoboGuice events to reduce coupling between classes having separate independent classes for different functional areas such as fragment loading, menus, layout etc. UnifiedMail handles navigation within a single activity, using the FragmentManager to show and hide different components of the interface. Events in RoboGuice don't place nicely in this setup, as the binding of event handlers is tied to the lifecycle of the activity, not individual fragments. So when a fragment is removed from the display, it continues to receive events even though it is detached from the activity. This means conditions need to be added to every event handler to protect against "zombie" fragments.

## Options

### Single activity, replace fragments on navigation

What Shuffle currently does.

#### Pros 

* Potentially more efficient as don't need to destroy and recreate activity

#### Cons

* Not much reuse of fragments - manager tends to create new ones
* Roboguice event handling means all zombie fragments keep getting events
* Have to manage back nav manually
* Issues with task pager not displaying correctly after nav
* Issues with tag/project counts not working after nav

### Single activity, hide and show fragments on navigation

For both phone and tablet, have all potential fragments available in view. Can still lazily load them, but once there, leave them there.
Need to communicate to each fragment to let it know if it's activated.

#### Pros

* No more zombie fragments
* Still should be fast to load as only load fragments as needed

#### Cons

* Have to manage back nav manually

### Load new activity on navigation

#### Pros

* Get back nav cheaply (should still specify parent activity - see http://developer.android.com/training/implementing-navigation/temporal.html#SynthesizeBackStack)
* Plays nicely with RoboGuice lifecycles
* Standard Android pattern

#### Cons

* Potentially slower
* Need to create separate activities for each view, although these could be lightweight subclasses (or could just reload same activity with different intents)
* May make supporting phone and tablet more tricky

#### How to implement

* Make `MainActivity` abstract
* Add subclasses for each screen or simply load activity with appropriate intent on navigation. If use separate subclasses, can handle searching projects and contexts correctly, and use parent activty settings in manifest.
* Events
*  * `NavigationRequestEvent` - user has requested to navigate to the given MainView
*  * `ViewUpdatedEvent` - new view now showing
* Change all references for `MainViewUpdatingEvent` and `MainViewUpdatedEvent` to one of new events
* Add NavigationController that listens on  NavigationRequestEvent and takes appropriate action depending on current view. 
* Add util method to determine if task view currently showing for list views and use everywhere
*  * On tablet if both views visible, clicking on list shouldn't reload full page


