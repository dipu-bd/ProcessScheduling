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
package org.alulab.pscheduling;

import org.alulab.pscheduling.model.ProgressCellRender;
import java.awt.EventQueue;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableCellRenderer;
import org.alulab.pscheduling.model.Process;
import javax.swing.table.DefaultTableModel;
import org.alulab.pscheduling.algo.FirstComeFirstServe;
import org.alulab.pscheduling.algo.NonPreemptiveSJF;
import org.alulab.pscheduling.algo.NonPreemtivePS;
import org.alulab.pscheduling.algo.PreemptivePS;
import org.alulab.pscheduling.algo.PreemptiveSJF;
import org.alulab.pscheduling.algo.RoundRobin;
import org.alulab.pscheduling.algo.Scheduler;

/**
 *
 * @author Dipu
 */
public final class MainForm extends javax.swing.JFrame {

    private Thread mThread;
    private Scheduler mSchedular;

    /**
     * Creates new form MainForm
     */
    public MainForm() {
        initComponents();

        // maximize window
        //this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        // select default algorithm
        chooserFCFS.setSelected(true);
        // set header center
        DefaultTableCellRenderer headerRenderer
                = (DefaultTableCellRenderer) processTable.getTableHeader().getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        // make cell center
        DefaultTableCellRenderer cellRenderer
                = (DefaultTableCellRenderer) processTable.getDefaultRenderer(Object.class);
        cellRenderer.setHorizontalAlignment(JLabel.CENTER);

        // load an algorithm
        loadAlgorithm();
    }

    void loadAlgorithm() {
        stopSchedular();
        Process.PROCESS_ID_NUMBER = 1;

        rrQuantumSpinner.setVisible(false);
        rrQuantumLabelHint.setVisible(false);
        rrQuantumSpinner.setValue(RoundRobin.QUANTUM);

        int selected = algoButtonGroup.getSelection().getMnemonic();
        switch (selected) {
            case '0': //fcfs   
                mSchedular = new FirstComeFirstServe();
                break;
            case '1': //non prm sjf
                mSchedular = new NonPreemptiveSJF();
                break;
            case '2': //prm sjf
                mSchedular = new PreemptiveSJF();
                break;
            case '3': //non prm ps
                mSchedular = new NonPreemtivePS();
                break;
            case '4': //prm ps
                mSchedular = new PreemptivePS();
                break;
            case '5': //rr
                mSchedular = new RoundRobin();
                rrQuantumSpinner.setVisible(true);
                rrQuantumLabelHint.setVisible(true);
                break;
        }

        algoNameLabel.setText(mSchedular.getName());

        addDummyProcess();

        loadSchedular();
    }

    void addDummyProcess() {
        EventQueue.invokeLater(() -> {
            addProcess(0, 15, 2);
            addProcess(2, 1, 2);
            addProcess(0, 25, 5);
            addProcess(0, 12, 7);
            addProcess(4, 3, 6);
        });
    }

    synchronized void loadSchedular() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("PID");
        model.addColumn("Arrival Time");
        model.addColumn("Burst Time");
        model.addColumn("Priority");
        model.addColumn("State");
        model.addColumn("Execution Time");
        model.addColumn("Progress");
        model.addColumn("Start Time");
        model.addColumn("Finish Time");

        int t = 0, index = -1;
        for (Process p : mSchedular.getProcessList()) {
            if (p.isRunning()) {
                index = t;
            }
            model.addRow(new Object[]{
                p.getPID(),
                p.getArriveTime(),
                p.getBurstTime(),
                p.getPriority(),
                p.getState(),
                p.getCounter(),
                p.getProgress(),
                p.getStartTime(),
                p.getFinishTime()
            });
            t++;
        }
        // set model
        processTable.setModel(model);
        processTable.getTableHeader().setReorderingAllowed(false);
        // show progress
        processTable.getColumn("Progress").setCellRenderer(new ProgressCellRender());

        // select running process
        if (index >= 0) {
            processTable.setRowSelectionInterval(index, index);
        }
    }

    synchronized void showRunning() {
        // time slide
        timeSlider.setMinimum(0);
        timeSlider.setMaximum(mSchedular.getTotalBurst());
        timeSlider.setValue(mSchedular.getCurrentTime());
        arrivalTimeSpinner.setValue(mSchedular.getCurrentTime());

        // cpu time
        cpuTimeLabel.setText(String.valueOf(mSchedular.getCurrentTime()));

        // running process
        Process p = mSchedular.getRunning();
        if (p != null) {
            pidLabel.setText(String.valueOf(p.getPID()));
            priorityLabel.setText(String.valueOf(p.getPriority()));
            startTimeLabel.setText(String.valueOf(p.getStartTime()));
            execTimeLabel.setText(String.valueOf(p.getExecTime()));
            burstTimeLabel.setText(String.valueOf(p.getBurstTime()));
            progressPercentage.setText(p.getProgressFormated());
            progressBar.setValue((int) p.getProgress());
        } else {
            pidLabel.setText("X");
            priorityLabel.setText("-");
            startTimeLabel.setText("-");
            execTimeLabel.setText("-");
            burstTimeLabel.setText("-");
            progressPercentage.setText("-");
            progressBar.setValue(0);
        }
    }

    synchronized void showStatistics() {
        if (mSchedular.isFinished()) {
            int wait = 0;
            int turn = 0;
            int n = mSchedular.getProcessList().size();
            for (Process p : mSchedular.getProcessList()) {
                wait += p.getStartTime();
                turn += p.getFinishTime();
            }
            double avgWait = (double) wait / n;
            double avgTurn = (double) turn / n;
            averageWaitingLabel.setText(String.format("%.2f unit", avgWait));
            averageTurnaroundLabel.setText(String.format("%.2f unit", avgTurn));
        } else {
            averageWaitingLabel.setText("-");
            averageTurnaroundLabel.setText("-");
        }
    }

    void refreshValues() {
        java.awt.EventQueue.invokeLater(() -> {
            loadSchedular();
            showRunning();
            showStatistics();
        });
    }

    void startSchedular() {
        //disable some
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        addProcessPanel.setEnabled(false);
        forwardButton.setEnabled(false);

        // restart scheduling if finished
        if (mSchedular.getCurrentTime() == mSchedular.getTotalBurst()) {
            mSchedular.restart();
        }

        // create and run thread
        mThread = new Thread(() -> {
            try {
                while (!mSchedular.isFinished()) {
                    mSchedular.stepForward(1);
                    refreshValues();
                    Thread.sleep((int) 50 * animeSpeed.getValue());
                }
                // calculate average
                refreshValues();
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
            mThread = null;
            stopSchedular();
        });
        mThread.start();
    }

    boolean isRunning() {
        return mThread != null && mThread.isAlive();
    }

    void stopSchedular() {
        try {
            while (isRunning()) {
                mThread.interrupt();
            }
        } catch (Exception ex) {
        }

        // enable all
        mThread = null;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        addProcessPanel.setEnabled(true);
        forwardButton.setEnabled(true);
    }

    void resetAll() {
        stopSchedular();
        mSchedular.reset();
        refreshValues();
    }

    void restart() {
        stopSchedular();
        mSchedular.restart();
        refreshValues();
    }

    void addProcess(int arrival, int burst, int priority) {
        if (mThread == null) {
            Process p = new Process(arrival, burst, priority);
            mSchedular.addProcess(p);
            refreshValues();
        }
    }

    void stepForward() {
        mSchedular.stepForward(1);
        refreshValues();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        algoButtonGroup = new javax.swing.ButtonGroup();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        algoNameLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        forwardButton = new javax.swing.JButton();
        resetButton = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        animeSpeedLabel = new javax.swing.JLabel();
        animeSpeed = new javax.swing.JSlider();
        rrQuantumLabelHint = new javax.swing.JLabel();
        rrQuantumSpinner = new javax.swing.JSpinner();
        jPanel3 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        cpuTimeLabel = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        pidLabel = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        progressPercentage = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        startTimeLabel = new javax.swing.JLabel();
        burstTimeLabel = new javax.swing.JLabel();
        execTimeLabel = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        priorityLabel = new javax.swing.JLabel();
        timeSlider = new javax.swing.JSlider();
        jLabel9 = new javax.swing.JLabel();
        algoHolderPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        chooserFCFS = new javax.swing.JRadioButton();
        chooserNPreSJF = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();
        chooserPreSJF = new javax.swing.JRadioButton();
        jLabel3 = new javax.swing.JLabel();
        chooserPrePS = new javax.swing.JRadioButton();
        chooserNPrePS = new javax.swing.JRadioButton();
        chooserRR = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        processTable = new javax.swing.JTable();
        addProcessPanel = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        arrivalTimeSpinner = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        burstTimeSpinner = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        prioritySpinner = new javax.swing.JSpinner();
        addButton = new javax.swing.JButton();
        addRandomButton = new javax.swing.JButton();
        clearAllButton = new javax.swing.JButton();
        statisticsPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        averageWaitingLabel = new javax.swing.JLabel();
        averageTurnaroundLabel = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("CPU Scheduling Algorithm Simulation");

        jPanel1.setBackground(new java.awt.Color(0, 102, 102));

        algoNameLabel.setBackground(new java.awt.Color(0, 204, 204));
        algoNameLabel.setFont(new java.awt.Font("Segoe Print", 0, 24)); // NOI18N
        algoNameLabel.setForeground(new java.awt.Color(204, 204, 0));
        algoNameLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        algoNameLabel.setText("Algo Name");

        jPanel2.setBackground(new java.awt.Color(153, 255, 204));
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Controls", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 11), new java.awt.Color(0, 51, 102))); // NOI18N

        startButton.setText("Start");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        stopButton.setText("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        forwardButton.setText(">> One Step Forward >>");
        forwardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                forwardButtonActionPerformed(evt);
            }
        });

        resetButton.setText("Reset");
        resetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetButtonActionPerformed(evt);
            }
        });

        jLabel8.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel8.setText("Animation Speed :");

        jLabel15.setText("ms/unit.");

        animeSpeedLabel.setFont(new java.awt.Font("Courier New", 0, 14)); // NOI18N
        animeSpeedLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        animeSpeedLabel.setText("500");
        animeSpeedLabel.setToolTipText("");
        animeSpeedLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        animeSpeed.setMaximum(200);
        animeSpeed.setMinimum(1);
        animeSpeed.setPaintTicks(true);
        animeSpeed.setValue(10);
        animeSpeed.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                animeSpeedStateChanged(evt);
            }
        });

        rrQuantumLabelHint.setText("Round Robin Quantum / Time Slice unit :");

        rrQuantumSpinner.setFont(new java.awt.Font("Courier New", 0, 14)); // NOI18N
        rrQuantumSpinner.setModel(new javax.swing.SpinnerNumberModel(5, 1, 1000, 1));
        rrQuantumSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rrQuantumSpinnerStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(animeSpeed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 52, Short.MAX_VALUE)
                        .addComponent(animeSpeedLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel15)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(forwardButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(startButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(stopButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(resetButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGap(1, 1, 1))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(rrQuantumLabelHint)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(rrQuantumSpinner)
                        .addContainerGap())))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(stopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(startButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(resetButton, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(forwardButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel15)
                    .addComponent(animeSpeedLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addComponent(animeSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rrQuantumLabelHint)
                    .addComponent(rrQuantumSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5))
        );

        jPanel3.setBackground(new java.awt.Color(204, 255, 255));
        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Running Process"));

        jLabel7.setText("Current CPU Time:");

        cpuTimeLabel.setFont(new java.awt.Font("Courier New", 0, 14)); // NOI18N
        cpuTimeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        cpuTimeLabel.setText("0");
        cpuTimeLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jPanel5.setBackground(new java.awt.Color(193, 255, 193));
        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Process Control Block", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 11), new java.awt.Color(51, 51, 51))); // NOI18N

        jLabel10.setText("PID:");

        pidLabel.setFont(new java.awt.Font("Courier New", 0, 14)); // NOI18N
        pidLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pidLabel.setText("X");
        pidLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel11.setText("Start Time :");

        jLabel12.setText("Burst Time :");

        jLabel13.setText("Execution Time:");

        jLabel14.setText("Progress:");

        progressPercentage.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        progressPercentage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        progressPercentage.setText("X%");
        progressPercentage.setToolTipText("");
        progressPercentage.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        startTimeLabel.setFont(new java.awt.Font("Courier New", 0, 14)); // NOI18N
        startTimeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        startTimeLabel.setText("0");
        startTimeLabel.setToolTipText("");
        startTimeLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        burstTimeLabel.setFont(new java.awt.Font("Courier New", 0, 14)); // NOI18N
        burstTimeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        burstTimeLabel.setText("0");
        burstTimeLabel.setToolTipText("");
        burstTimeLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        execTimeLabel.setFont(new java.awt.Font("Courier New", 0, 14)); // NOI18N
        execTimeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        execTimeLabel.setText("0");
        execTimeLabel.setToolTipText("");
        execTimeLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel19.setText("Priority:");

        priorityLabel.setFont(new java.awt.Font("Courier New", 0, 14)); // NOI18N
        priorityLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        priorityLabel.setText("0");
        priorityLabel.setToolTipText("");
        priorityLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(progressPercentage, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(priorityLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel19)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pidLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(11, 11, 11)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel12)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(burstTimeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel11)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(startTimeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(execTimeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))))))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(startTimeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(1, 1, 1)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel12)
                            .addComponent(burstTimeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(1, 1, 1)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel13)
                            .addComponent(execTimeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(pidLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10))
                        .addGap(1, 1, 1)
                        .addComponent(jLabel19)
                        .addGap(0, 0, 0)
                        .addComponent(priorityLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(progressPercentage, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel14))
                    .addComponent(progressBar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cpuTimeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(cpuTimeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(3, 3, 3))
        );

        timeSlider.setValue(0);
        timeSlider.setEnabled(false);

        jLabel9.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(204, 204, 0));
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel9.setText("Time Line :");

        algoHolderPanel.setBackground(new java.awt.Color(68, 228, 228));

        jLabel1.setBackground(new java.awt.Color(0, 204, 204));
        jLabel1.setFont(new java.awt.Font("Segoe Print", 0, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 0, 102));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel1.setText("Choose an Algorithm");

        chooserFCFS.setBackground(new java.awt.Color(70, 230, 230));
        algoButtonGroup.add(chooserFCFS);
        chooserFCFS.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        chooserFCFS.setMnemonic('0');
        chooserFCFS.setText("1) First Come First Serve");
        chooserFCFS.setToolTipText("");
        chooserFCFS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooserFCFSActionPerformed(evt);
            }
        });

        chooserNPreSJF.setBackground(new java.awt.Color(70, 230, 230));
        algoButtonGroup.add(chooserNPreSJF);
        chooserNPreSJF.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        chooserNPreSJF.setMnemonic('1');
        chooserNPreSJF.setText("a) Non Pre-emptive");
        chooserNPreSJF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooserNPreSJFActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel2.setText("2) Shortest Job First");

        chooserPreSJF.setBackground(new java.awt.Color(70, 230, 230));
        algoButtonGroup.add(chooserPreSJF);
        chooserPreSJF.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        chooserPreSJF.setMnemonic('2');
        chooserPreSJF.setText("b) Pre-emptive");
        chooserPreSJF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooserPreSJFActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel3.setText("3) Priority Scheduling");

        chooserPrePS.setBackground(new java.awt.Color(70, 230, 230));
        algoButtonGroup.add(chooserPrePS);
        chooserPrePS.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        chooserPrePS.setMnemonic('4');
        chooserPrePS.setText("b) Pre-emptive");
        chooserPrePS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooserPrePSActionPerformed(evt);
            }
        });

        chooserNPrePS.setBackground(new java.awt.Color(70, 230, 230));
        algoButtonGroup.add(chooserNPrePS);
        chooserNPrePS.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        chooserNPrePS.setMnemonic('3');
        chooserNPrePS.setText("a) Non Pre-emptive");
        chooserNPrePS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooserNPrePSActionPerformed(evt);
            }
        });

        chooserRR.setBackground(new java.awt.Color(70, 230, 230));
        algoButtonGroup.add(chooserRR);
        chooserRR.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        chooserRR.setMnemonic('5');
        chooserRR.setText("4) Round Robin");
        chooserRR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooserRRActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout algoHolderPanelLayout = new javax.swing.GroupLayout(algoHolderPanel);
        algoHolderPanel.setLayout(algoHolderPanelLayout);
        algoHolderPanelLayout.setHorizontalGroup(
            algoHolderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(algoHolderPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(algoHolderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(algoHolderPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(algoHolderPanelLayout.createSequentialGroup()
                        .addGroup(algoHolderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chooserFCFS, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(algoHolderPanelLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(algoHolderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(algoHolderPanelLayout.createSequentialGroup()
                                        .addGap(10, 10, 10)
                                        .addGroup(algoHolderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(chooserPrePS, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(chooserPreSJF, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(chooserNPreSJF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(chooserNPrePS, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                            .addComponent(chooserRR, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(78, 78, 78))))
        );
        algoHolderPanelLayout.setVerticalGroup(
            algoHolderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(algoHolderPanelLayout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chooserFCFS)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chooserNPreSJF)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chooserPreSJF)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chooserNPrePS)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chooserPrePS)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chooserRR)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(algoHolderPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(timeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 572, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(algoNameLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 631, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(algoHolderPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(algoNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel9))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(timeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))))
            .addComponent(jSeparator1)
        );

        jPanel4.setBackground(new java.awt.Color(0, 255, 204));
        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("List of all processes"));
        jPanel4.setMinimumSize(new java.awt.Dimension(100, 200));

        processTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4", "Title 5", "Title 6", "Title 7", "Title 8"
            }
        ));
        processTable.setDoubleBuffered(true);
        processTable.setOpaque(false);
        processTable.setRowHeight(24);
        processTable.setShowHorizontalLines(false);
        processTable.setShowVerticalLines(false);
        jScrollPane2.setViewportView(processTable);

        addProcessPanel.setBackground(new java.awt.Color(255, 255, 204));
        addProcessPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel17.setText("Arrival Time:");
        addProcessPanel.add(jLabel17);

        arrivalTimeSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 10000, 1));
        addProcessPanel.add(arrivalTimeSpinner);

        jLabel6.setText("Burst Time:");
        addProcessPanel.add(jLabel6);

        burstTimeSpinner.setModel(new javax.swing.SpinnerNumberModel(5, 1, 1000, 1));
        addProcessPanel.add(burstTimeSpinner);

        jLabel4.setText("Priority:");
        addProcessPanel.add(jLabel4);

        prioritySpinner.setModel(new javax.swing.SpinnerNumberModel(5, 1, 100, 1));
        addProcessPanel.add(prioritySpinner);

        addButton.setText("Add");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });
        addProcessPanel.add(addButton);

        addRandomButton.setText("Add Random Process");
        addRandomButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addRandomButtonActionPerformed(evt);
            }
        });
        addProcessPanel.add(addRandomButton);

        clearAllButton.setBackground(new java.awt.Color(255, 102, 0));
        clearAllButton.setText("Clear All");
        clearAllButton.setToolTipText("Use with caution");
        clearAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearAllButtonActionPerformed(evt);
            }
        });
        addProcessPanel.add(clearAllButton);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addComponent(jScrollPane2))
            .addComponent(addProcessPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addComponent(addProcessPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        statisticsPanel.setBackground(new java.awt.Color(153, 255, 204));
        statisticsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Statistics"));

        jLabel5.setText("Average Turnaround Time");

        jLabel16.setText("Average Waiting Time:");

        averageWaitingLabel.setFont(new java.awt.Font("Courier New", 0, 14)); // NOI18N
        averageWaitingLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        averageWaitingLabel.setText("0");
        averageWaitingLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        averageTurnaroundLabel.setFont(new java.awt.Font("Courier New", 0, 14)); // NOI18N
        averageTurnaroundLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        averageTurnaroundLabel.setText("0");
        averageTurnaroundLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout statisticsPanelLayout = new javax.swing.GroupLayout(statisticsPanel);
        statisticsPanel.setLayout(statisticsPanelLayout);
        statisticsPanelLayout.setHorizontalGroup(
            statisticsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statisticsPanelLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(averageWaitingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(averageTurnaroundLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        statisticsPanelLayout.setVerticalGroup(
            statisticsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statisticsPanelLayout.createSequentialGroup()
                .addGroup(statisticsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(averageWaitingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16)
                    .addComponent(jLabel5)
                    .addComponent(averageTurnaroundLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0))
        );

        jSeparator2.setBackground(new java.awt.Color(0, 153, 153));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statisticsPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jSeparator2)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(statisticsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void chooserFCFSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chooserFCFSActionPerformed
        loadAlgorithm();
    }//GEN-LAST:event_chooserFCFSActionPerformed

    private void chooserNPreSJFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chooserNPreSJFActionPerformed
        loadAlgorithm();
    }//GEN-LAST:event_chooserNPreSJFActionPerformed

    private void chooserPreSJFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chooserPreSJFActionPerformed
        loadAlgorithm();
    }//GEN-LAST:event_chooserPreSJFActionPerformed

    private void chooserNPrePSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chooserNPrePSActionPerformed
        loadAlgorithm();
    }//GEN-LAST:event_chooserNPrePSActionPerformed

    private void chooserPrePSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chooserPrePSActionPerformed
        loadAlgorithm();
    }//GEN-LAST:event_chooserPrePSActionPerformed

    private void chooserRRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chooserRRActionPerformed
        loadAlgorithm();
    }//GEN-LAST:event_chooserRRActionPerformed

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        startSchedular();
    }//GEN-LAST:event_startButtonActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        stopSchedular();
    }//GEN-LAST:event_stopButtonActionPerformed

    private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetButtonActionPerformed
        restart();
    }//GEN-LAST:event_resetButtonActionPerformed

    private void forwardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_forwardButtonActionPerformed
        stepForward();
    }//GEN-LAST:event_forwardButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        addProcess((int) arrivalTimeSpinner.getValue(),
                (int) burstTimeSpinner.getValue(),
                (int) prioritySpinner.getValue());
    }//GEN-LAST:event_addButtonActionPerformed

    private void addRandomButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addRandomButtonActionPerformed
        addProcess((int) Math.ceil(100 * Math.random()),
                (int) Math.ceil(100 * Math.random()),
                (int) Math.ceil(10 * Math.random()));
    }//GEN-LAST:event_addRandomButtonActionPerformed

    private void clearAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearAllButtonActionPerformed
        resetAll();
    }//GEN-LAST:event_clearAllButtonActionPerformed

    private void animeSpeedStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_animeSpeedStateChanged
        animeSpeedLabel.setText(String.valueOf((int) animeSpeed.getValue() * 50));
    }//GEN-LAST:event_animeSpeedStateChanged

    private void rrQuantumSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rrQuantumSpinnerStateChanged
        RoundRobin.QUANTUM = (int) rrQuantumSpinner.getValue();
    }//GEN-LAST:event_rrQuantumSpinnerStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JPanel addProcessPanel;
    private javax.swing.JButton addRandomButton;
    private javax.swing.ButtonGroup algoButtonGroup;
    private javax.swing.JPanel algoHolderPanel;
    private javax.swing.JLabel algoNameLabel;
    private javax.swing.JSlider animeSpeed;
    private javax.swing.JLabel animeSpeedLabel;
    private javax.swing.JSpinner arrivalTimeSpinner;
    private javax.swing.JLabel averageTurnaroundLabel;
    private javax.swing.JLabel averageWaitingLabel;
    private javax.swing.JLabel burstTimeLabel;
    private javax.swing.JSpinner burstTimeSpinner;
    private javax.swing.JRadioButton chooserFCFS;
    private javax.swing.JRadioButton chooserNPrePS;
    private javax.swing.JRadioButton chooserNPreSJF;
    private javax.swing.JRadioButton chooserPrePS;
    private javax.swing.JRadioButton chooserPreSJF;
    private javax.swing.JRadioButton chooserRR;
    private javax.swing.JButton clearAllButton;
    private javax.swing.JLabel cpuTimeLabel;
    private javax.swing.JLabel execTimeLabel;
    private javax.swing.JButton forwardButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel pidLabel;
    private javax.swing.JLabel priorityLabel;
    private javax.swing.JSpinner prioritySpinner;
    private javax.swing.JTable processTable;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel progressPercentage;
    private javax.swing.JButton resetButton;
    private javax.swing.JLabel rrQuantumLabelHint;
    private javax.swing.JSpinner rrQuantumSpinner;
    private javax.swing.JButton startButton;
    private javax.swing.JLabel startTimeLabel;
    private javax.swing.JPanel statisticsPanel;
    private javax.swing.JButton stopButton;
    private javax.swing.JSlider timeSlider;
    // End of variables declaration//GEN-END:variables
}
