package com.occuhunt.student;

import java.util.Calendar;

public final class Fair
{
    private long mId;
    private String mName, mLogoFilePath, mVenue;
    private Calendar mStartTime, mEndTime;
    
    public Fair(long id, String name, String logoFilePath, String venue, Calendar startTime, Calendar endTime) {
        setId(id);
        setName(name);
        setLogoFilePath(logoFilePath);
        setVenue(venue);
        setStartTime(startTime);
        setEndTime(endTime);
    }
    
    public long getId() {
        return mId;
    }
    public void setId(long id) {
        mId = id;
    }
    public String getName() {
        return mName;
    }
    public void setName(String name) {
        mName = name;
    }
    public String getLogoFilePath() {
        return mLogoFilePath;
    }
    public void setLogoFilePath(String logoFilePath) {
        mLogoFilePath = logoFilePath;
    }
    public String getVenue() {
        return mVenue;
    }
    public void setVenue(String venue) {
        mVenue = venue;
    }
    public Calendar getStartTime() {
        return mStartTime;
    }
    public void setStartTime(Calendar startTime) {
        mStartTime = startTime;
    }
    public Calendar getEndTime() {
        return mEndTime;
    }
    public void setEndTime(Calendar endTime) {
        mEndTime = endTime;
    }
}
