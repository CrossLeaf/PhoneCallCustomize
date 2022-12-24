package com.hoho.phonecallapp;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * author: aJIEw
 * description: Activity 栈的管理
 */
public class ActivityStack {

    private static final ActivityStack INSTANCE = new ActivityStack();

    private List<AppCompatActivity> activities = new ArrayList<>();

    public static ActivityStack getInstance() {
        return INSTANCE;
    }

    public void addActivity(AppCompatActivity activity) {
        activities.add(activity);
    }

    public AppCompatActivity getTopActivity() {
        if (activities.isEmpty()) {
            return null;
        }
        return activities.get(activities.size() - 1);
    }

    public void finishTopActivity() {
        if (!activities.isEmpty()) {
            activities.remove(activities.size() - 1).finish();
        }
    }

    public void finishActivity(AppCompatActivity activity) {
        if (activity != null) {
            activities.remove(activity);
            activity.finish();
        }
    }

    public void finishActivity(Class activityClass) {
        for (AppCompatActivity activity : activities) {
            if (activity.getClass().equals(activityClass)) {
                finishActivity(activity);
            }
        }
    }

    public void finishAllActivity() {
        if (!activities.isEmpty()) {
            for (AppCompatActivity activity : activities) {
                activity.finish();
                activities.remove(activity);
            }
        }
    }
}
