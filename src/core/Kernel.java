package core;

import sync.Mutex;
import util.Pair;
import util.State;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

public class Kernel {
    private int processesCount;
    private int timeSlice;
    private LinkedList<Process> readyList;
    private LinkedList<Process> blockedList;
    private LinkedList<Process> allProcesses;
    private Mutex userInput;
    private Mutex userOutput;
    private Mutex file;
    private HashMap<Integer, ArrayList<String>> arrivalTimes;
    private Process cur;

    public Kernel(int timeSlice, HashMap<Integer, ArrayList<String>> arrivalTimes) {
        processesCount = 0;
        this.timeSlice = timeSlice;
        this.arrivalTimes = arrivalTimes;
        this.cur = null;
        this.readyList = new LinkedList<>();
        this.blockedList = new LinkedList<>();
        userInput = new Mutex();
        userOutput = new Mutex();
        file = new Mutex();
        allProcesses = new LinkedList<>();
        try {
            startScheduler();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isBlocked(Process proc) {
        return blockedList.contains(proc);
    }

    public boolean block(Process proc, int mu){
        System.out.println("Process "+cur.getID()+" has been blocked for requesting "+ (mu == 0 ? "file access" : mu == 1 ? "user input" : "user output") +"and added to the blocked queue\n");
        System.out.println("Blocked List of file access" + (mu == 0 ? file.getBlockedList() : mu == 1 ? userInput.getBlockedList() : userOutput.getBlockedList()));
        SystemCall.writeToMemory(proc.getLoPCB() + 1, State.BLOCKED, false);
        blockedList.add(proc);
        return true;
    }

    public void unblock(Process proc) throws IOException {
        if(proc.getLoPCB() == -1) {
            writeProcessToMemory(proc, State.READY, false);
            System.out.println("Process "+ proc.getID() + " has been swapped out of disk\n");
        }
        blockedList.remove(proc);
        SystemCall.writeToMemory(proc.getLoPCB() + 1, State.READY, false);
        readyList.add(proc);
    }

    public boolean isComplete(Process proc) {
        int pc = (int) SystemCall.readFromMemory(proc.getLoPCB() + 2);
        int lowerBound = (int) SystemCall.readFromMemory(proc.getLoPCB() + 3);
        int upperBound = (int) SystemCall.readFromMemory(proc.getLoPCB() + 4);
        return pc + lowerBound - 1 == upperBound;
    }

    public Process createProcess(String path) throws IOException {
        boolean swap = false;
        ArrayList<String> instructions = SystemCall.readFromDisk(path);
        Process proc;
        int start;
        int pointer = Memory.getPointer();
        if (Memory.noOfProcesses == 0) {
            proc = new Process(getNextProcID(), pointer, pointer + 3 + instructions.size() - 1);
            proc.setLoData(pointer);
            start = Memory.getPCBLo();
        } else if (Memory.noOfProcesses == 1) {
            proc = new Process(getNextProcID(), pointer, pointer + 3 + instructions.size() - 1);
            proc.setLoData(pointer);
            start = Memory.getPCBLo();
        } else {
            start = Memory.swap(cur, blockedList, allProcesses);
            proc = new Process(getNextProcID(), Memory.pointer, Memory.pointer + 3 + instructions.size() - 1);
            proc.setLoData(Memory.pointer);
            swap = true;
        }
        if(start != -1) {
            proc.setLoPCB(start);
            SystemCall.writeToMemory(start++, getNextProcID(), false);
            SystemCall.writeToMemory(start++, State.READY, false);
            SystemCall.writeToMemory(start++, 3, false);
            SystemCall.writeToMemory(start++, proc.getLoData(), false);
            SystemCall.writeToMemory(start++, proc.getLoData() + instructions.size() + 2, false);
            SystemCall.writeToMemory(proc.getLoData(), instructions, true);
            processesCount++;
            readyList.add(proc);
            allProcesses.add(proc);
            if (!swap)
                Memory.noOfProcesses++;
        }
        return proc;
    }

    public boolean semWait(Process proc, String type){
        if(type.equals("file")){
            if(file.semWait(proc))
                return block(proc, 0);
        }
        else if(type.equals("userInput")){
            if(userInput.semWait(proc))
                return block(proc, 1);
        }
        else{
            if(userOutput.semWait(proc))
                return block(proc, 2);
        }
        return false;
    }

    public void semSignal(Process proc, String type) throws IOException {
        Process newOwner;
        if(type.equals("file")){
            newOwner = file.semSignal(proc);
            if(newOwner != null)
                unblock(newOwner);
        }
        else if(type.equals("userInput")){
            newOwner = userInput.semSignal(proc);
            if(newOwner != null)
                unblock(newOwner);
        }
        else{
            newOwner = userOutput.semSignal(proc);
            if(newOwner != null)
                unblock(newOwner);
        }
    }

    private void startScheduler() throws IOException {
        int time = timeSlice;
        int clockCycle = 0;
        while (true) {
            System.out.println("Cycle " + clockCycle + "\n");
            if (arrivalTimes.containsKey(clockCycle)) {
                for(String path : arrivalTimes.get(clockCycle))
                    createProcess("src/" + path);
            }
            if (cur != null) {
                // there is a process running
                if (isComplete(cur)) {
                    // process finished execution
                    allProcesses.remove(cur);
                    SystemCall.writeToMemory(cur.getLoPCB() + 1, State.FINISHED, false);
                    System.out.println("Process "+cur.getID()+" has finished execution\n");
                    cur = readyList.poll();
                    System.out.println("Ready List: " + readyList + "\n");
                    System.out.println("Blocked List: " + blockedList + "\n");
                    time = timeSlice;
                    if (cur != null) {
                        System.out.println("Process "+cur.getID()+" has been chosen from the ready queue\n");
                        if (cur.getLoPCB() == -1) {
                            writeProcessToMemory(cur, State.RUNNING, false);
                            System.out.println("Process " + cur.getID() + " has been swapped out of disk\n");
                        } else
                            SystemCall.writeToMemory(cur.getLoPCB() + 1, State.RUNNING, false);
                        System.out.println("Executing Process: " + cur.getID() + "\n");
                        System.out.println("Memory contents: \n" + Memory.print() + "\n");
                        executeNext(cur);
                        --time;
                    }
                } else if (isBlocked(cur)) {
                    // process is blocked
                    cur = readyList.poll();
                    System.out.println("Ready List: " + readyList + "\n");
                    System.out.println("Blocked List: " + blockedList + "\n");
                    time = timeSlice;
                    if (cur != null) {
                        System.out.println("Process "+cur.getID()+" has been chosen from the ready queue\n");
                        if (cur.getLoPCB() == -1) {
                            writeProcessToMemory(cur, State.RUNNING, false);
                            System.out.println("Process " + cur.getID() + " has been swapped out of disk\n");
                        } else
                            SystemCall.writeToMemory(cur.getLoPCB() + 1, State.RUNNING, false);
                        System.out.println("Executing Process: " + cur.getID() + "\n");
                        System.out.println("Memory contents: \n" + Memory.print() + "\n");
                        executeNext(cur);
                        --time;
                    }
                } else if (time == 0) {
                    // time slice is finished
                    readyList.add(cur);
                    System.out.println("Process "+cur.getID()+" has finished its time slice and added to the ready list\n");
                    SystemCall.writeToMemory(cur.getLoPCB() + 1, State.READY, false);
                    time = timeSlice;
                    cur = readyList.poll();
                    System.out.println("Ready List: " + readyList + "\n");
                    System.out.println("Blocked List: " + blockedList + "\n");
                    if (cur != null) {
                        System.out.println("Process "+cur.getID()+" has been chosen from the ready queue\n");
                        if (cur.getLoPCB() == -1) {
                            writeProcessToMemory(cur, State.RUNNING, false);
                            System.out.println("Process " + cur.getID() + " has been swapped out of disk\n");
                        } else
                            SystemCall.writeToMemory(cur.getLoPCB() + 1, State.RUNNING, false);
                        System.out.println("Executing Process: " + cur.getID() + "\n");
                        System.out.println("Memory contents: \n" + Memory.print() + "\n");
                        executeNext(cur);
                        --time;
                    }
                } else {
                    System.out.println("Ready List: " + readyList + "\n");
                    System.out.println("Blocked List: " + blockedList + "\n");
                    System.out.println("Executing Process: " + cur.getID() + "\n");
                    System.out.println("Memory contents: \n" + Memory.print() + "\n");
                    executeNext(cur);
                    --time;
                }
            } else {
                // no process is running
                // check ready list
                cur = readyList.poll();
                System.out.println("Ready List: " + readyList + "\n");
                System.out.println("Blocked List: " + blockedList + "\n");
                if (cur != null) {
                    System.out.println("Process "+cur.getID()+" has been chosen from the ready queue\n");
                    if (cur.getLoPCB() == -1) {
                        writeProcessToMemory(cur, State.RUNNING, false);
                        System.out.println("Process " + cur.getID() + " has been swapped out of disk\n");
                    } else
                        SystemCall.writeToMemory(cur.getLoPCB() + 1, State.RUNNING, false);
                    System.out.println("Executing Process: " + cur.getID() + "\n");
                    System.out.println("Memory contents: \n" + Memory.print() + "\n");
                    executeNext(cur);
                    --time;
                }
            }
            clockCycle++;
            System.out.println("\n----------------------------------------------------------------------------------------\n");
            if (readyList.isEmpty() && cur == null) {
                Object[] arr = arrivalTimes.keySet().toArray();
                Arrays.sort(arr);
                if(clockCycle > (int) arr[arr.length-1])
                    break;
            }
        }
    }

    public void writeProcessToMemory(Process proc, State state, boolean skip) throws IOException {
        ArrayList<String> data = SystemCall.readFromDisk("src/Process" + proc.getID() + ".txt");
        boolean swap = false;
        int start;
        if (Memory.noOfProcesses == 0) {
            start = Memory.getPCBLo();
        } else if (Memory.noOfProcesses == 1) {
            start = Memory.getPCBLo();
        } else {
            start = Memory.swap(cur, blockedList, allProcesses);
            swap = true;
        }
        int pointer = Memory.getPointer();
        if (start != -1) {
            proc.setLoPCB(start);
            proc.setLoData(pointer);
            SystemCall.writeToMemory(start++, Integer.parseInt(data.get(0)), false);
            SystemCall.writeToMemory(start++, state, false);
            SystemCall.writeToMemory(start++, Integer.parseInt(data.get(2)), false);
            SystemCall.writeToMemory(start++, pointer, false);
            SystemCall.writeToMemory(start++, pointer + data.size() - 6, false);
            SystemCall.writeToMemory(pointer, new ArrayList<>(data.subList(5, data.size())), skip);
            if (!swap)
                Memory.noOfProcesses++;
        }

    }

    public void executeNext(Process proc) throws IOException {
        int loPCB = proc.getLoPCB();
        int loData = proc.getLoData();
        int pc = (int) SystemCall.readFromMemory(loPCB + 2) + loData;
        System.out.println("pc: " + pc+"\n");
        System.out.println("Instruction being executed: "+ SystemCall.readFromMemory(pc)+"\n");
        SystemCall.writeToMemory(loPCB + 2, pc - loData + 1, false);
        String[] instruction = ((String) SystemCall.readFromMemory(pc)).split(" ");
        if (instruction[0].equals("semWait")) {
            if(!semWait(proc, instruction[1])) {
                System.out.println("^^^^^^^^^^^^^^^^^^^");
                System.out.println("Process " + proc.getID() + " acquired " + instruction[1]);
                System.out.println("^^^^^^^^^^^^^^^^^^^");
            }
        } else if (instruction[0].equals("semSignal")) {
            semSignal(proc, instruction[1]);
            System.out.println("^^^^^^^^^^^^^^^^^^^");
            System.out.println("Process "+proc.getID()+" released "+instruction[1]);
            System.out.println("^^^^^^^^^^^^^^^^^^^");
        } else if (instruction[0].equals("print")) {
            System.out.println("^^^^^^^^^^^^^^^^^^^");
            System.out.println("Printing output:");
            Pair p = (Pair) SystemCall.readFromMemory(getVariableIndex(proc, instruction[1]));
            SystemCall.printData((p.getVal()));
            System.out.println("^^^^^^^^^^^^^^^^^^^");
        } else if (instruction[0].equals("printFromTo")) {
            int lo = Integer.parseInt(((Pair) SystemCall.readFromMemory(getVariableIndex(proc, instruction[1]))).getVal());
            int hi = Integer.parseInt(((Pair) SystemCall.readFromMemory(getVariableIndex(proc, instruction[2]))).getVal());
            System.out.println("^^^^^^^^^^^^^^^^^^^");
            System.out.println("Printing numbers from "+lo +" to "+hi);
            for (int i = lo; i <= hi; i++)
                SystemCall.printData(String.valueOf(i));
            System.out.println("^^^^^^^^^^^^^^^^^^^");

        } else if (instruction[0].equals("writeFile")) {
            Pair p1 = (Pair) SystemCall.readFromMemory(getVariableIndex(proc, instruction[1]));
            Pair p2 = (Pair) SystemCall.readFromMemory(getVariableIndex(proc, instruction[2]));
            System.out.println("^^^^^^^^^^^^^^^^^^^");
            System.out.println("File "+p1.getVal()+".txt has been written to disk");
            System.out.println("^^^^^^^^^^^^^^^^^^^");
            SystemCall.writeToDisk(p1.getVal()+".txt", p2.getVal());
        } else if (instruction[0].equals("assign")) {
            SystemCall.writeToMemory(getVariableIndex(proc, instruction[1]), new Pair(instruction[1], (String) proc.getTmpData()), false);
            System.out.println("^^^^^^^^^^^^^^^^^^^");
            System.out.println("Variable "+instruction[1]+" is assigned value "+proc.getTmpData());
            System.out.println("^^^^^^^^^^^^^^^^^^^");
            proc.setTmpData(null);
        } else if (instruction[0].equals("input")) {
            System.out.println("^^^^^^^^^^^^^^^^^^^");
            System.out.println("Reading user input");
            Object data = SystemCall.takeUserInput();
            System.out.println("^^^^^^^^^^^^^^^^^^^");
            proc.setTmpData(data);
        } else if (instruction[0].equals("readFile")) {
            Pair p = (Pair) SystemCall.readFromMemory(getVariableIndex(proc, instruction[1]));
            Object data = SystemCall.readFromDisk("src/"+p.getVal()+".txt");
            StringBuilder sb = new StringBuilder();
            for (String s : (ArrayList<String>)data)
                sb.append(s+"\n");
            SystemCall.writeToMemory(getVariableIndex(proc, instruction[1]), new Pair(instruction[1], sb.toString()), false);
            proc.setTmpData(sb.toString());
        }
    }

    public int getVariableIndex(Process proc, String var){
        for(int i = proc.getLoData(); i < proc.getLoData() + 3; i++){
            Object o = SystemCall.readFromMemory(i);
            if(o == null || o.equals("null"))
                return i;
            if(o instanceof Pair && ((Pair) o).getKey().equals(var))
                return i;
        }
        return -1;
    }

    public int getNextProcID(){
        return processesCount + 1;
    }
}
