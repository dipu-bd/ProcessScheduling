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

import java.util.Iterator;
import org.alulab.pscheduling.model.Process;

/**
 *
 * @author Dipu
 */
public class RoundRobin extends Scheduler {

    public static int QUANTUM = 5;

    private int mCurPos = 0;

    @Override
    void initialize() {
        mCurPos = 0;
    }

    @Override
    public String getName() {
        return "Round Robin";
    }

    @Override
    Process nextRunning(int time) {
        if (time % QUANTUM == 0 || mRunning == null || mRunning.isFinished()) {
            // get next process to run            
            Process now = mRunning;
            while (true) {
                Process run = mProcesses.get(mCurPos);
                mCurPos = (mCurPos + 1) % mProcesses.size();
                if (!run.isFinished() || run == now) {
                    return run;
                }
            }
        }
        return mRunning;
    }
}
