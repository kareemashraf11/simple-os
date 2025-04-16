package util;

public class Pair {

    private String key, val;

    public Pair(String key, String val) {
        this.key = key;
        this.val = val;
    }

    public String getKey() {
        return key;
    }

    public String getVal() {
        return val;
    }

    @Override
    public String toString() {
        return "util.Pair{" +
                "key='" + key + '\'' +
                ", val='" + val + '\'' +
                '}';
    }
}
