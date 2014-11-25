package org.dodgybits.shuffle.android.core.model.persistence.selector;

import android.util.Log;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.util.StringUtils;
import org.dodgybits.shuffle.android.preference.model.ListSettings;

import java.util.ArrayList;
import java.util.List;

import static org.dodgybits.shuffle.android.core.model.persistence.selector.Flag.ignored;

public abstract class AbstractEntitySelector<E extends EntitySelector<E>> implements EntitySelector<E> {
    private static final String cTag = "AbstractEntitySelector";

    protected Flag mActive = ignored;
    protected Flag mDeleted = ignored;
    protected String mSortOrder;
    
    @Override
    public Flag getActive() {
        return mActive;
    }

    @Override
    public Flag getDeleted() {
        return mDeleted;
    }
    
    @Override
    public String getSortOrder() {
        return mSortOrder;
    }

    @Override
    public String getSelection(android.content.Context context) {
        List<String> expressions = getSelectionExpressions(context);
        String selection = StringUtils.join(expressions, " AND ");
        Log.d(cTag, selection);
        return selection;
    }

    protected List<String> getSelectionExpressions(android.content.Context context) {
        List<String> expressions = new ArrayList<String>();
        return expressions;
    }    

    protected void addFlagExpression(List<String> expressions, String field, Flag flag) {
        if (flag != Flag.ignored) {
            String expression = field + "=" + (flag == Flag.yes ? "1" : "0");
            expressions.add(expression);
        }
    }

    protected void addIdCheckExpression(List<String> expressions, String field, Id id) {
        if (id.isInitialised()) {
            expressions.add(field + "=?");
        }
    }

    protected void addIdArg(List<String> args, Id id) {
        if (id.isInitialised()) {
            args.add(String.valueOf(id.getId()));
        }
    }


    public abstract static class AbstractBuilder<E extends AbstractEntitySelector<E>> implements EntitySelector.Builder<E> {
        protected E mResult;
        
        @Override
        public Flag getDeleted() {
            return mResult.getDeleted();
        }
        
        @Override
        public Flag getActive() {
            return mResult.getActive();
        }
        
        @Override
        public String getSortOrder() {
            return mResult.getSortOrder();
        }

        @Override
        public AbstractBuilder<E> setSortOrder(String value) {
            mResult.mSortOrder = value;
            return this;
        }

        @Override
        public AbstractBuilder<E> setActive(Flag value) {
            mResult.mActive = value;
            return this;
        }

        @Override
        public AbstractBuilder<E> setDeleted(Flag value) {
            mResult.mDeleted = value;
            return this;
        }

        @Override
        public E build() {
            if (mResult == null) {
                throw new IllegalStateException(
                        "build() has already been called on this Builder.");
            }
            E returnMe = mResult;
            mResult = null;

            if (Log.isLoggable(cTag, Log.DEBUG)) {
                Log.d(cTag, returnMe.toString());
            }
            return returnMe;
        }

        @Override
        public AbstractBuilder<E> mergeFrom(E selector) {
            setActive(selector.mActive);
            setDeleted(selector.mDeleted);
            setSortOrder(selector.mSortOrder);

            return this;
        }

        @Override
        public AbstractBuilder<E> applyListPreferences(android.content.Context context, ListSettings settings) {
            setActive(settings.getActive(context));
            setDeleted(settings.getDeleted(context));

            return this;
        }

        
    }
    
    
}
