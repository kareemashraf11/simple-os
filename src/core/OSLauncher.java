package core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class OSLauncher {

    public static void init(){
        HashMap<Integer, ArrayList<String>> arrivalTimes = new HashMap<>();
        arrivalTimes.put(0,new ArrayList<>(Arrays.asList("resources/programs/Program_1.txt")));
        arrivalTimes.put(0,new ArrayList<>(Arrays.asList("resources/programs/Program_2.txt")));
        arrivalTimes.put(4,new ArrayList<>(Arrays.asList("resources/programs/Program_3.txt")));
        new Kernel(6, arrivalTimes);
    }

    public static void main(String[] args) {
        init();
    }
}
