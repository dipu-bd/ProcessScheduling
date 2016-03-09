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

/**
 *
 * @author Dipu
 */
public class FirstComeFirstServe extends Scheduler {

    private int mCurPos = 0;

    @Override
    public String getName() {
        return "First Come First Server";
    }

    @Override
    void initialize() {
        mCurPos = 0;
    }

    @Override
    Process nextRunning(int time) {
        Process cur = mRunning;
        if (cur == null || cur.isFinished()) {
            cur = mProcesses.get(mCurPos);
            mCurPos = (mCurPos + 1) % mProcesses.size();
        }
        return cur;
    }

}
