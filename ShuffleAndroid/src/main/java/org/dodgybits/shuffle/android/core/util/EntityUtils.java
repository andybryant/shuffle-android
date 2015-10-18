/*
 * Copyright (C) 2012 Android Shuffle Open Source Project
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
package org.dodgybits.shuffle.android.core.util;

import com.google.common.collect.Sets;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.DefaultEntityCache;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.dodgybits.shuffle.android.core.util.ObjectUtils.compareInts;
import static org.dodgybits.shuffle.android.core.util.ObjectUtils.compareLongs;

public class EntityUtils {

    public static boolean idsMatch(List<Id> ids1, List<Id> ids2) {
        final int oldSize = ids1.size();
        final int newSize = ids2.size();

        if (newSize == 0 && oldSize == 0) {
            return true;
        }
        if (newSize != oldSize) {
            return false;
        }

        // check all ids are the same
        Set<Id> newIds = Sets.newHashSet(ids1);
        for (Id id : ids2) {
            if (!newIds.contains(id)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Sort by following criteria: project order (no project last) asc,
     * display order asc, due date asc, created desc
     */
    public static Comparator<Task> createComparator(final DefaultEntityCache<Project> projectCache) {
        return new Comparator<Task>() {
            @Override
            public int compare(Task lhs, Task rhs) {
                int result;
                Project lhsProject = projectCache.findById(lhs.getProjectId());
                Project rhsProject = projectCache.findById(rhs.getProjectId());
                if (lhsProject != null) {
                    if (rhsProject != null) {
                        result = compareInts(lhsProject.getOrder(), rhsProject.getOrder());
                    } else {
                        return -1;
                    }
                } else {
                    if (rhsProject != null) {
                        return 1;
                    } else {
                        result = 0;
                    }
                }

                if (result == 0) {
                    result = compareInts(lhs.getOrder(), rhs.getOrder());
                    if (result == 0) {
                        result = compareLongs(lhs.getDueDate(), rhs.getDueDate());
                        if (result == 0) {
                            result = compareLongs(rhs.getCreatedDate(), lhs.getCreatedDate());
                        }
                    }
                }
                return result;
            }
        };
    }

}
