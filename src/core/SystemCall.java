package core;

import util.Pair;

import java.io.*;
import java.util.ArrayList;

public class SystemCall {

    public static ArrayList<String> readFromDisk(String path) throws IOException {
        FileReader fr = new FileReader(path);
        BufferedReader br = new BufferedReader(fr);
        ArrayList<String> data = new ArrayList<>();
        String s;
        while ((s = br.readLine()) != null) {
            if(path.startsWith("src/Program")) {
                String[] row = s.split(" ");
                if(row[0].equals("assign")) {
                    if (row.length >= 3) {
                        data.add(row[2] + (row.length == 4 ? " " + row[3] : ""));
                        data.add(row[0] + " " + row[1]);
                    }
                }
                     else
                        data.add(s);
            }
            else
                data.add(s);
        }
        br.close();
        return data;
    }

    public static void writeToDisk(String name, String data) throws IOException {
        FileWriter fw = new FileWriter("src/resources/processes/" + name);
        fw.write(data);
        fw.close();
    }

    public static String takeUserInput() throws IOException{
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Write your input, please");
        return br.readLine();
    }

    public static void printData(String data){
        System.out.println(data);
    }

    public static ArrayList<Object> readFromMemory(int lo, int hi){
        ArrayList<Object> data = new ArrayList<>();
        for (int i = lo; i <= hi; i++)
            data.add(Memory.get(i));
        return data;
    }

    public static Object readFromMemory(int index){
        return Memory.get(index);
    }

    public static void writeToMemory(int index, Object data, boolean skip) {
        if (data instanceof ArrayList) {
            ArrayList<String> instructions = (ArrayList<String>) data;
            if (skip)
                index += 3;
            for (int i = 0; i < instructions.size(); i++) {
                if(instructions.get(i).startsWith("util.Pair")) {
                    String[] ins = instructions.get(i).split(" ");
                    writeToMemory(index++, new Pair(ins[1], ins[2]), skip);
                }
                else
                    writeToMemory(index++, instructions.get(i), skip);
            }
            return;
        }
        Memory.set(index, data);
    }

}
