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
public abstract class PriorityScheduling extends Scheduler {

    @Override
    public String getName() {
        return "Priority Scheduling";
    }

    Process getNextProcess(int curTime) {
        Process sel = mRunning;
        int maxPrior = 0;
        for (Process p : mProcesses) {
            if (p.getArriveTime() > curTime || p.isFinished()) {
                continue;
            }
            if (p.getPriority() > maxPrior) {
                sel = p;
                maxPrior = p.getPriority();
            }
        }
        return sel;
    }
}
