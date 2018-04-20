package mrl.world.routing.highway;

import mrl.common.MRLConstants;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Mostafa Shabani.
 * Date: Feb 11, 2011
 * Time: 12:43:56 PM
 */
public class HighwayFactory {
    MrlWorld world;

    public HighwayFactory(MrlWorld world) {
        this.world = world;
    }

    public Highways createHighways(String fileName) {

        Highways highways = new Highways();

        try {
            BufferedReader bf = new BufferedReader(new FileReader(fileName));
            String str = bf.readLine();
            int id;
            Highway highway = null;
            while (str != null) {
                if (!str.isEmpty()) {
                    if (str.startsWith("id: ")) {
                        id = Integer.parseInt(str.replaceFirst("id: ", ""));
                        highway = new Highway(new EntityID(id));
                    } else if (highway != null) {

                        String[] values = str.split(",");
                        int x = Integer.parseInt(values[0]);
                        int y = Integer.parseInt(values[1]);
                        Road road = null;
                        try {
                            road = world.getRoadInPoint(new Point(x, y));
                        } catch (Exception e1) {
                            System.err.println("road = world.getRoadInPoint(new Point(x, y))  " + e1);
//                            e1.printStackTrace();
                        }
                        if (road != null) {
                            highway.add(road);
                        }
                    } else if (highway != null) {
                        highway = null;
                    }
                } else {
                    if (highway != null) {
                        highways.add(highway);
                    }
                }
                str = bf.readLine();
            }
            if (highway != null) {
                highways.add(highway);
            }
        } catch (FileNotFoundException e) {
            if (MRLConstants.HIGHWAY_STRATEGY) {
                System.err.println("ERROR IN Highway Factory " + e.getMessage());
            }
        } catch (IOException e) {
            if (MRLConstants.HIGHWAY_STRATEGY) {
                System.err.println("ERROR IN Highway Factory " + e.getMessage());
            }
        }
        return highways;
    }

}
