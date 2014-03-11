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

import java.util.List;
import java.util.Set;

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

}
