package mrl.ambulance.marketLearnerStrategy;

import java.io.*;
import java.util.ArrayList;

/**
 * User: pooyad
 * Date: Mar 1, 2011
 * Time: 11:46:16 AM
 */
public class LearnerIO {

    private FileWriter writer = null;
    private boolean initWrite = false;

    private String fileName;
    private boolean append;

    public LearnerIO(String fileName, boolean append) {
        this.fileName = fileName;
        this.append = append;
    }

    public void simplePrintToFile(String fileName, String data) {
        if (!initWrite) {
            initWrite(fileName, append);
            initWrite = true;
        }
        out(writer, data);
    }

    public void printToFile_LA(String data) {
        if (!initWrite) {
            initWrite(fileName, false);
            initWrite = true;
        }
        out(writer, data);
    }

    public double[] readFromFile_LA(int numberOfStates) {

        double[] stateProbabilities = new double[numberOfStates];

        try {
            FileReader fr = new FileReader(fileName);
            BufferedReader br = new BufferedReader(fr);//Can also use a Scanner to read the file

            String line = "";
            ArrayList[] data = new ArrayList[3];//consider using ArrayList<int>
            int state = 0, time = 0, action = 0;
            double reward = 0, valueFunction = 0;

            line = br.readLine();
            String[] theline = line.split("\t");

            for (int i = 0; i < numberOfStates; i++) {

                stateProbabilities[i] = Double.parseDouble(theline[i + 1]);
            }

        } catch (FileNotFoundException fN) {
//            fN.printStackTrace();
            System.err.println("File Not Exist!!! " + fileName);
            return new double[numberOfStates];
        } catch (IOException e) {
            System.out.println(e);
        }

        return stateProbabilities;
    }

    public void simplePrintToFile(String data) {
        if (!initWrite) {
            initWrite(fileName, append);
            initWrite = true;
        }
        out(writer, data);
    }

    private void initWrite(String fileName, boolean append) {
        try {
            writer = new FileWriter(fileName, append);
        } catch (Exception ex) {
            System.err.println("ERROR: LearnerIO.initWrite()  " + ex.getMessage());
//            ex.printStackTrace();
        }
    }

    private void closeWriter() {
        try {
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void writeTableToFile(double[][] qTable, int stNum, int action, double reward, double valueFunction, int time) {
        if (!initWrite) {
            initWrite(fileName, append);
            initWrite = true;
        }
        String str = "";
        for (int i = 0; i < qTable.length; i++) {
            for (int j = 0; j < qTable.length; j++) {
                str += ("\t" + String.valueOf(qTable[i][j]));
            }
            str += "\n";
        }
        str += "\n";
        str += " State  :\t" + stNum + "\t   time:\t" + time + "\n";
        str += " Action :\t" + action + "\t  valueFunction:\t" + valueFunction + "\n";
        str += " Reward :\t" + reward + "\n";

        out(writer, str);
    }

    public LearnerVariables readTableFromFile() {

        LearnerVariables learnerVariables = new LearnerVariables();

        try {
            FileReader fr = new FileReader("C:\\mytest\\test2.txt");
            BufferedReader br = new BufferedReader(fr);//Can also use a Scanner to read the file

            String line = "";
            ArrayList[] data = new ArrayList[3];//consider using ArrayList<int>
            double[][] qTable = new double[5][5];
            int state = 0, time = 0, action = 0;
            double reward = 0, valueFunction = 0;


            for (int i = 0; i <= 8; i++) {
                line = br.readLine();
                String[] theline = line.split("\t");

                if (i <= 4) {
                    for (int j = 0; j <= 4; j++) {
                        qTable[i][j] = Double.parseDouble(theline[j + 1]);
                    }
                } else if (i == 5)
                    continue;
                else if (i == 6) {
                    state = Integer.parseInt(theline[1]);
                    time = Integer.parseInt(theline[3]);
                } else if (i == 7) {
                    action = Integer.parseInt(theline[1]);
                    valueFunction = Double.parseDouble(theline[3]);
                } else {
                    reward = Double.parseDouble(theline[1]);
                }
            }

            learnerVariables.setQTable(qTable);
            learnerVariables.setState(state);
            learnerVariables.setAction(action);
            learnerVariables.setReward(reward);
            learnerVariables.setValueFunction(valueFunction);
            learnerVariables.setTime(time);

        } catch (FileNotFoundException fN) {
            fN.printStackTrace();
        } catch (IOException e) {
            System.out.println(e);
        }

        return learnerVariables;
    }

    private void out(FileWriter writer, String str) {
        if (writer == null)
            return;
        try {
            writer.write(str + "\n");
            writer.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


}
