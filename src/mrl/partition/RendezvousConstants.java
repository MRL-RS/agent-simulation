package mrl.partition;


import mrl.world.MrlWorld;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by P.D.G.
 * User: pooyaD
 * Date: Feb 12, 2010
 * Time: 12:02:08 PM
 */
public final class RendezvousConstants {

    MrlWorld world;
    public final static int rendezvousCheckPeriod = 15;
    public final static int currentRendezvousIndex = 0;
    public final static int previousRendezvousIndex = 0;
    public final static boolean shouldMoveOnToRendezvous = false;
    public final static int stayInRendezvousCounter = 100;
    public final static int rendezvousIndex = 0;


    public RendezvousConstants(MrlWorld world) {
        this.world = world;
        KOBE_REND_6 = new ArrayList<DefaultRendezvous>(
                Arrays.asList(
                        new DefaultRendezvous(world, new EntityID(1976), 0, false, 0, 1),
                        new DefaultRendezvous(world, new EntityID(32787), 1, false, 1, 2),
                        new DefaultRendezvous(world, new EntityID(31204), 0, false, 2, 3),
                        new DefaultRendezvous(world, new EntityID(33815), 1, false, 3, 4),
                        new DefaultRendezvous(world, new EntityID(32737), 0, false, 4, 5),
                        new DefaultRendezvous(world, new EntityID(352), 1, false, 5, 0)
                )
        );


        KOBE_REND_4 = new ArrayList<DefaultRendezvous>(
                Arrays.asList(
                        new DefaultRendezvous(world, new EntityID(32765), 0, false, 0, 1),
                        new DefaultRendezvous(world, new EntityID(31204), 1, false, 1, 2),
                        new DefaultRendezvous(world, new EntityID(8106), 0, false, 2, 3),
                        new DefaultRendezvous(world, new EntityID(352), 1, false, 3, 0)
                )
        );

        KOBE_REND_2 = new ArrayList<DefaultRendezvous>(
                Arrays.asList(
                        new DefaultRendezvous(world, new EntityID(35362), 0, false, 0, 1)
                )
        );


        VC_REND_4 = new ArrayList<DefaultRendezvous>(
                Arrays.asList(
                        new DefaultRendezvous(world, new EntityID(1510), 0, false, 0, 1),
                        new DefaultRendezvous(world, new EntityID(53441), 1, false, 1, 2),
                        new DefaultRendezvous(world, new EntityID(977), 0, false, 2, 3),
                        new DefaultRendezvous(world, new EntityID(51078), 1, false, 3, 0)
                ));
        VC_REND_2 = new ArrayList<DefaultRendezvous>(
                Arrays.asList(
                        new DefaultRendezvous(world, new EntityID(1053), 0, false, 0, 1)
                ));


        BERLIN_REND_6 = new ArrayList<DefaultRendezvous>(
                Arrays.asList(
                        new DefaultRendezvous(world, new EntityID(1976), 0, false, 0, 1),
                        new DefaultRendezvous(world, new EntityID(32787), 1, false, 1, 2),
                        new DefaultRendezvous(world, new EntityID(31204), 0, false, 2, 3),
                        new DefaultRendezvous(world, new EntityID(33815), 1, false, 3, 4),
                        new DefaultRendezvous(world, new EntityID(32737), 0, false, 4, 5),
                        new DefaultRendezvous(world, new EntityID(352), 1, false, 5, 0)
                )
        );


    }

    /**
     * the order of points are counter-clockwise and every point is between only two partition
     */
    public ArrayList<DefaultRendezvous> KOBE_REND_6;
    public ArrayList<DefaultRendezvous> KOBE_REND_4;
    public ArrayList<DefaultRendezvous> KOBE_REND_2;
    public ArrayList<DefaultRendezvous> VC_REND_4;
    public ArrayList<DefaultRendezvous> VC_REND_2;
    public ArrayList<DefaultRendezvous> BERLIN_REND_6;
    public ArrayList<DefaultRendezvous> BERLIN_REND_4;
    public ArrayList<DefaultRendezvous> BERLIN_REND_2;
    public ArrayList<DefaultRendezvous> PARIS_REND_6;
    public ArrayList<DefaultRendezvous> PARIS_REND_4;
    public ArrayList<DefaultRendezvous> PARIS_REND_2;
}


