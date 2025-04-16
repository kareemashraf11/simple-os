package core;

public class Process {

    private int totalSize;
    private int loPCB;
    private int loData;
    private int id;
    private Object tmpData;

    public Process(int id, int lo, int hi){
        totalSize = hi - lo + 1;
        this.id = id;
        tmpData = null;
    }

    public void setTmpData(Object tmpData) {
        this.tmpData = tmpData;
    }

    public Object getTmpData() {
        return tmpData;
    }

    public int getID() {
        return id;
    }


    public int getLoData() {
        return loData;
    }

    public int getLoPCB() {
        return loPCB;
    }

    public void setLoData(int loData) {
        this.loData = loData;
    }

    public void setLoPCB(int loPCB) {
        this.loPCB = loPCB;
    }

    @Override
    public String toString() {
        return "core.Process{" +
                "id=" + id +
                ", totalSize=" + totalSize +
                '}';
    }
}


