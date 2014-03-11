package org.dodgybits.android.shuffle.util;


import org.dodgybits.shuffle.android.core.util.DateUtils;
import org.junit.Assert;
import org.junit.Test;

public class DateUtilsTest {

    @Test
    public void testParser() throws Exception {
        Assert.assertEquals(1250337003000L, DateUtils.parseIso8601Date("2009-08-15T12:50:03+01:00"));
        Assert.assertEquals(1263144688000L, DateUtils.parseIso8601Date("2010-01-10T17:31:28+00:00"));
        Assert.assertEquals(1270644578000L, DateUtils.parseIso8601Date("2010-04-07T08:49:38-04:00"));
        Assert.assertEquals(1270566552000L, DateUtils.parseIso8601Date("2010-04-06T11:09:12-04:00"));
        Assert.assertEquals(1270612800000L, DateUtils.parseIso8601Date("2010-04-07T04:00:00Z"));
    }
    
    @Test
    public void testFormatter() throws Exception {
        Assert.assertEquals(1250337003000L, DateUtils.parseIso8601Date(DateUtils.formatIso8601Date(1250337003000L)));
        Assert.assertEquals(1263144688000L, DateUtils.parseIso8601Date(DateUtils.formatIso8601Date(1263144688000L)));
        Assert.assertEquals(1270644578000L, DateUtils.parseIso8601Date(DateUtils.formatIso8601Date(1270644578000L)));
        Assert.assertEquals(1270566552000L, DateUtils.parseIso8601Date(DateUtils.formatIso8601Date(1270566552000L)));
        Assert.assertEquals(1270612800000L, DateUtils.parseIso8601Date(DateUtils.formatIso8601Date(1270612800000L)));
    }
    
}
