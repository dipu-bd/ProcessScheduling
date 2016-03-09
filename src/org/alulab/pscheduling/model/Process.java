/*
 * Copyright (c) 2016 Dipu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dipu - initial API and implementation and/or initial documentation
 */
package org.alulab.pscheduling.model;

/**
 * A real process
 *
 * @author Dipu
 */
public class Process implements Comparable<Process> {

    public static int PROCESS_ID_NUMBER = 1;

    // process states
    public static final String NEW = "New";
    public static final String READY = "Ready";
    public static final String WAITING = "Waiting";
    public static final String RUNNING = "Running";
    public static final String TERMINATED = "Terminated";

    // process variables
    private final int mPID;
    private final int mBurstTime;
    private final int mPriority;
    private volatile int mCounter;
    private String mState;
    private int mArriveTime;
    private int mStartTime;
    private int mFinishTime;

    public Process(int arrival, int burstTime, int priority) {
        mPID = PROCESS_ID_NUMBER++;
        mArriveTime = arrival;
        mBurstTime = burstTime;
        mPriority = priority;
        mState = READY;
        mCounter = 0;
    }

    public void reset() {
        mState = READY;
        mCounter = 0;
        mStartTime = mFinishTime = 0;
    }

    /**
     * Do one unit of work
     *
     * @param timeId
     */
    public void dowork(int timeId) {
        if (mState != TERMINATED) {
            if (mCounter == 0) {
                mStartTime = timeId;
            }
            mCounter++;
            mState = RUNNING;
            if (mCounter == mBurstTime) {
                mState = TERMINATED;
                mFinishTime = timeId;
            }
        }
    }

    //
    // Getter methods
    //
    public int getPID() {
        return mPID;
    }

    public int getBurstTime() {
        return mBurstTime;
    }

    public String getState() {
        return mState;
    }

    public void setState(String state) {
        mState = state;
    }

    public int getPriority() {
        return mPriority;
    }

    public int getCounter() {
        return mCounter;
    }

    public boolean isFinished() {
        return mCounter == mBurstTime;
    }

    public boolean isRunning() {
        return mState.equals(RUNNING);
    }

    public boolean isWaiting() {
        return mState.equals(WAITING);
    }

    public double getProgress() {
        return mBurstTime == 0 ? 100 : 100.0 * mCounter / (double) mBurstTime;
    }

    public String getProgressFormated() {
        return String.format("%.2f%%", getProgress());
    }

    public int getArriveTime() {
        return mArriveTime;
    }

    public void setArriveTime(int arriveTime) {
        mArriveTime = arriveTime;
    }

    public int getStartTime() {
        return mStartTime;
    }

    public void setStartTime(int startTime) {
        mStartTime = startTime;
    }

    public int getFinishTime() {
        return mFinishTime;
    }

    public void setFinishTime(int finishTime) {
        mFinishTime = finishTime;
    }

    public int getExecTime() {
        return mCounter;
    }

    @Override
    public int compareTo(Process p) {
        return mArriveTime - p.getArriveTime();
    }
}
