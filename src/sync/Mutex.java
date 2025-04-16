package sync;

import core.Process;

import java.util.LinkedList;

public class Mutex {

    private LinkedList<Process> blockedList;
    private int ownerID;
    private boolean available;

    public Mutex(){
        ownerID = -1;
        blockedList = new LinkedList<>();
        available = true;
    }

    public boolean semWait(Process proc){
        if(available){
            available = !available;
            ownerID = proc.getID();
        }
        else{
            blockedList.add(proc);
            return true;
        }
        return false;
    }

    public Process semSignal(core.Process proc){
        if(ownerID == proc.getID()){
            if(blockedList.isEmpty())
                available = true;
            else{
                core.Process newOwner = blockedList.poll();
                ownerID = newOwner.getID();
                return newOwner;
            }
        }
        return null;
    }

    public LinkedList<Process> getBlockedList() {
        return blockedList;
    }
}
