package core;

import util.Pair;
import util.State;

import java.io.IOException;
import java.util.LinkedList;

public class Memory {

    public static int pointer = 11;
    private static final int SIZE = 40;
    private static Object[] memory = new Object[SIZE];
    public  static int noOfProcesses = 0;


    public static int getPointer() {
        return memory[14] == null ? 11 : 26;
    }

    public static int getPCBLo() {
        return memory[0] == null ? 0 : 5;
    }

    public static int swap(Process executing, LinkedList<Process> blockedList, LinkedList<Process> allProcesses) throws IOException {
        String data = null;
        int id = -1, lo = -1;
        boolean cont = true;
        if(executing == null){
            for(Process proc : allProcesses){
                if(proc.getLoPCB() != -1 && memory[proc.getLoPCB() + 1] == State.FINISHED){
                    if (proc.getLoPCB() == 0){
                        id = (Integer) memory[5];
                        lo = 5;
                        pointer = 26;
                        data = extractProcess(5, 9, (Integer) memory[8], (Integer) memory[9]);
                        cont = false;
                    }
                    else if(proc.getLoPCB() == 5){
                        id = (Integer) memory[0];
                        lo = 0;
                        pointer = 11;
                        data = extractProcess(0, 4, (Integer) memory[3], (Integer) memory[4]);
                        cont = false;
                    }
                }
            }
            if(cont) {
                if (blockedList.isEmpty()) {
                    // choose anyone
                    if (memory[8] != null && memory[9] != null) {
                        id = (Integer) memory[5];
                        lo = 5;
                        pointer = 26;
                        data = extractProcess(5, 9, (Integer) memory[8], (Integer) memory[9]);
                    } else {
                        id = (Integer) memory[0];
                        lo = 0;
                        pointer = 11;
                        data = extractProcess(0, 4, (Integer) memory[3], (Integer) memory[4]);
                    }
                } else {
                    // choose last blocked one
                    Process proc = blockedList.getLast();
                    if (proc.getLoPCB() == 0) {
                        id = (Integer) memory[5];
                        lo = 5;
                        pointer = 26;
                        data = extractProcess(5, 9, (Integer) memory[8], (Integer) memory[9]);
                    } else if (proc.getLoPCB() == 5) {
                        id = (Integer) memory[0];
                        lo = 0;
                        pointer = 11;
                        data = extractProcess(0, 4, (Integer) memory[3], (Integer) memory[4]);
                    }
                }
            }
        }
        else {
            //choose other one
            if (executing.getLoPCB() == 0) {
                id = (Integer) memory[5];
                lo = 5;
                pointer = 26;
                data = extractProcess(5, 9, (Integer) memory[8], (Integer) memory[9]);
            } else {
                id = (Integer) memory[0];
                lo = 0;
                pointer = 11;
                data = extractProcess(0, 4, (Integer) memory[3], (Integer) memory[4]);
            }
            for (Process proc : allProcesses) {
                if (proc.getID() == id) {
                    proc.setLoData(-1);
                    proc.setLoPCB(-1);
                }
            }
        }
        if(id != -1 && data != null) {
            SystemCall.writeToDisk("core.Process" + id + ".txt", data);
            System.out.println("core.Process " + id + " has been swapped into disk");
        }
        return lo;
    }


    private static String extractProcess(int loPCB, int hiPCB, int loData, int hiData){
        StringBuilder sb = new StringBuilder();
        for (int i = loPCB; i <= hiPCB; i++) {
            sb.append(memory[i] + "\n");
            memory[i] = null;
        }
        for (int i = loData; i <= hiData; i++) {
            if(memory[i] != null && memory[i] instanceof Pair)
                sb.append("util.Pair "+((Pair)memory[i]).getKey() + " " + (((Pair) memory[i]).getVal())+'\n');
            else
                sb.append(memory[i] + "\n");
            memory[i] = null;
        }
        return sb.toString();
    }

    public static void set(int index, Object data){
        memory[index] = data;
    }

    public static Object get(int index){
        return memory[index];
    }

    public static String print() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < memory.length; i++) {
            sb.append(memory[i]+((i!=39)?", ":""));
        }
        sb.append("]");
        return sb.toString();
    }
}
