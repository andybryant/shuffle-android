
package org.dodgybits.shuffle.android.core.model;

public interface Entity {

    /**
     * @return primary key for entity in local sqlite DB.
     */
    Id getLocalId();

    /**
     * @return primary key for entity in google app engine datastore.
     */
    Id getGaeId();

    /**
     * @return ms since epoch entity was last modified.
     */
    long getModifiedDate();
    
    boolean isActive();
    
    boolean isDeleted();
    
    boolean isValid();

}
