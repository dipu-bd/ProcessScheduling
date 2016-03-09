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
package org.alulab.pscheduling.algo;

import org.alulab.pscheduling.model.Process;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author Dipu
 */
public abstract class Scheduler {

    Process mRunning;
    int mCurrentTime;
    int mTotalBurstTime;
    final ArrayList<Process> mProcesses;

    public Scheduler() {
        mProcesses = new ArrayList<>();
        mCurrentTime = 0;
        mTotalBurstTime = 0;
        initialize();
    }

    public abstract String getName();

    abstract void initialize();

    abstract Process nextRunning(int curTime);

    /**
     * Add a process to the list of processes
     *
     * @param p
     * @param arriveTime
     */
    public void addProcess(Process p) {
        mProcesses.add(p);
        calculateTotalBurst();
    }

    /**
     * Use FCFS to calculate
     */
    public void calculateTotalBurst() {
        // sort by arrive time
        Collections.sort(mProcesses, (Process a, Process b) -> {
            return a.getArriveTime() - b.getArriveTime();
        });
        // run fcfs
        mTotalBurstTime = 0;
        for (Process p : mProcesses) {
            if (p.getArriveTime() > mTotalBurstTime) {
                mTotalBurstTime += p.getArriveTime();
            }
            mTotalBurstTime += p.getBurstTime();
        }
    }

    /**
     * Resets the scheduler
     */
    public void reset() {
        mProcesses.clear();
        mCurrentTime = 0;
        mTotalBurstTime = 0;
        mRunning = null;
        initialize();
    }

    public void restart() {
        for (Process p : mProcesses) {
            p.reset();
        }
        mCurrentTime = 0;
        mRunning = null;
        initialize();
    }

    /**
     * Take step forward of giving amount of time unit
     *
     * @param amount
     */
    public void stepForward(int amount) {
        for (int i = 0; i < amount && mCurrentTime < mTotalBurstTime; ++i) {
            if (mRunning != null && mRunning.isRunning()) {
                mRunning.setState(Process.WAITING);
            }
            mRunning = nextRunning(mCurrentTime);
            mRunning.setState(Process.RUNNING);
            if (mRunning != null) {
                mRunning.dowork(mCurrentTime);
                ++mCurrentTime;
            }
        }
    }

    /**
     * Gets the current CPU time
     *
     * @return
     */
    public int getCurrentTime() {
        return mCurrentTime;
    }

    public boolean isFinished() {
        return mCurrentTime == mTotalBurstTime;
    }

    /**
     * Gets the list of all processes
     *
     * @return
     */
    public ArrayList<Process> getProcessList() {
        return mProcesses;
    }

    public int getTotalBurst() {
        return mTotalBurstTime;
    }

    public Process getRunning() {
        return mRunning;
    }

}
