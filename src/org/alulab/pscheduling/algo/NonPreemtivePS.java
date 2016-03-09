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
public class NonPreemtivePS extends PriorityScheduling {

    @Override
    void initialize() {
    }

    @Override
    public String getName() {
        return "Non Pre-emptive Priority Scheduling";
    }

    @Override
    Process nextRunning(int curTime) {
        if (mRunning == null || mRunning.isFinished()) {
            return getNextProcess(curTime);
        }
        return mRunning;
    }

}
