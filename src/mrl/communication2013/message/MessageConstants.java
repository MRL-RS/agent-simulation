package mrl.communication2013.message;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * Author: Mostafa Movahedi
 * Date: Aug 26, 2010
 * Time: 12:31:01 PM
 * To change this template use File | Settings | File Templates.
 */

//constants that need for creating braveCircles.abstractMessageClass
public class MessageConstants {
    protected static final int MsgType_Bit_num = 4;
    protected static Map<String, Integer> msgType = new HashMap<String, Integer>();
    protected Map<Integer, String> inverseMsgType = new HashMap<Integer, String>();

    //Commands Constants
    //Stock
    protected int Stuck_Bit_num = 0;//will be calculated in constructor
    protected final int Stuck_Location_Bit_num = 16;

    //RescueMe
    protected int RescueMe_Bit_num = 0;//will be calculated in constructor
    protected final int RescueMe_ID_Bit_num = 16;
    protected final int RescueMe_Location_Bit_num = 16;
    protected final int RescueMe_Buriedness_Bit_num = 7;
    protected final int RescueMe_Damage_Bit_num = 12;
    protected final int RescueMe_HP_Bit_num = 14;
    protected final int RescueMe_Priority_Bit_num = 2;


    //Messages Constants
    //Building constants
    protected int Building_Bit_num = 0;//will be calculated in constructor
    protected final int Building_ID_Bit_num = 16;
    protected final int Building_Fireyness_Bit_num = 4;
    protected final int Building_Brokeness_Bit_num = 7;
    protected final int Building_Temprature_Bit_num = 9;

    //Civilian constants
    protected int Civilian_Bit_num = 0;//will be calculated in constructor
    protected final int Civilian_ID_Bit_num = 16;
    protected final int Civilian_Damage_Bit_num = 12;
    protected final int Civilian_Burriedness_Bit_num = 7;
    protected final int Civilian_HP_Bit_num = 14;
    protected final int Civilian_Position_Bit_num = 16;

    //AmbulanceTeam constants
    protected int AmbulanceTeam_Bit_num = 0;//will be calculated in constructor
    protected final int AmbulanceTeam_ID_Bit_num = 16;
    protected final int AmbulanceTeam_Damage_Bit_num = 12;
    protected final int AmbulanceTeam_Burriedness_Bit_num = 7;
    protected final int AmbulanceTeam_HP_Bit_num = 14;
    protected final int AmbulanceTeam_Position_Bit_num = 16;

    //FireBrigade constants
    protected int FireBrigade_Bit_num = 0;//will be calculated in constructor
    protected final int FireBrigade_ID_Bit_num = 16;
    protected final int FireBrigade_Damage_Bit_num = 12;
    protected final int FireBrigade_Burriedness_Bit_num = 7;
    protected final int FireBrigade_HP_Bit_num = 14;
    protected final int FireBrigade_Position_Bit_num = 16;

    //PoliceForce constants
    protected int PoliceForce_Bit_num = 0;//will be calculated in constructor
    protected final int PoliceForce_ID_Bit_num = 16;
    protected final int PoliceForce_Damage_Bit_num = 12;
    protected final int PoliceForce_Burriedness_Bit_num = 7;
    protected final int PoliceForce_HP_Bit_num = 14;
    protected final int PoliceForce_Position_Bit_num = 16;

    //Blockade Constants
    protected int Blockade_Bit_num = 0;//will be calculated in constructor
    protected final int Blockade_ID_Bit_num = 16;
    protected final int Blockade_Position_Bit_num = 16;
    protected final int Blockade_RepairCost_Bit_num = 8;
    protected final int Blockade_X_Bit_num = 20;
    protected final int Blockade_Y_Bit_num = 20;

    //Road Constants
    protected int Road_Bit_num = 0;//will be calculated in constructor
    protected final int Road_ID_bit_num = 16;
    protected final int Road_Blockade_count = 6;
    protected final int Road_Blockade_ID_num = 16;
    protected final int Road_X_Bit_num = 20;
    protected final int Road_Y_Bit_num = 20;

    protected Message msg;

    public MessageConstants() {
        Stuck_Bit_num = Stuck_Location_Bit_num;
        RescueMe_Bit_num = RescueMe_Location_Bit_num + RescueMe_Buriedness_Bit_num + RescueMe_Damage_Bit_num + RescueMe_HP_Bit_num + RescueMe_Priority_Bit_num;

        Building_Bit_num = Building_ID_Bit_num + Building_Fireyness_Bit_num + Building_Brokeness_Bit_num + Building_Temprature_Bit_num;
        Civilian_Bit_num = Civilian_ID_Bit_num + Civilian_Damage_Bit_num + Civilian_Burriedness_Bit_num + Civilian_HP_Bit_num + Civilian_Position_Bit_num;
        Blockade_Bit_num = Blockade_ID_Bit_num + Blockade_Position_Bit_num + Blockade_RepairCost_Bit_num + Blockade_X_Bit_num + Blockade_Y_Bit_num;
        Road_Bit_num = Road_ID_bit_num + Road_Blockade_ID_num + Road_Blockade_count + Road_X_Bit_num + Road_Y_Bit_num;

        msgType.put("NOP", 0);
        msgType.put("building", 1);
        msgType.put("civilian", 2);
        msgType.put("ambulanceTeam", 3);
        msgType.put("fireBrigade", 4);
        msgType.put("policeForce", 5);
        msgType.put("blockade", 6);
        msgType.put("road", 7);
        msgType.put("stuck", 8);
        msgType.put("rescue me", 9);

        /*msgType.put("NOP",0);
        msgType.put("building", 1);
        msgType.put("civilian", 2);
        msgType.put("blockade", 3);
        msgType.put("road",4);
        msgType.put("stuck",5);
        msgType.put("rescue me",6);
*/
        inverseMsgType.put(0, "NOP");
        inverseMsgType.put(1, "building");
        inverseMsgType.put(2, "civilian");
        inverseMsgType.put(3, "blockade");
        inverseMsgType.put(4, "road");
    }
}
