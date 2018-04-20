package mrl.mrlPersonal.viewer;

import mrl.mrlPersonal.viewer.layers.*;
import rescuecore2.standard.view.AreaIconLayer;
import rescuecore2.standard.view.AreaNeighboursLayer;
import rescuecore2.standard.view.PositionHistoryLayer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Mostafa Shabani
 * Date: Dec 10, 2010
 */
public class MrlAnimatedWorldModelViewer extends MrlStandardWorldModelViewer {
    private static final int FRAME_COUNT = 10;
    private static final int ANIMATION_TIME = 750;
    private static final int FRAME_DELAY = ANIMATION_TIME / FRAME_COUNT;

    private MrlAnimatedHumanLayer humans;
    private final Object lock = new Object();
    private boolean done;

    /**
     * Construct an animated world model viewer.
     */
    public MrlAnimatedWorldModelViewer() {
        super();
        Timer timer = new Timer(FRAME_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (lock) {
                    if (done) {
                        return;
                    }
                    done = true;
                    if (humans.nextFrame()) {
                        done = false;
                        repaint();
                    }
                }
            }
        });
        timer.setRepeats(true);
        timer.start();
    }

    @Override
    public String getViewerName() {
        return "Animated world model viewer";
    }

    @Override
    public void addDefaultLayers() {
        addLayer(new MrlRoadLayer());

        addLayer(new MrlBuildingLayer());
        addLayer(new MrlRoadBlockageLayer());

        MrlGraphLayer graphLayer = new MrlGraphLayer();
        graphLayer.setVisible(false);
        addLayer(graphLayer);

        MrlGridsLayer gridsLayer = new MrlGridsLayer();
        gridsLayer.setVisible(false);
        addLayer(gridsLayer);

        MrlBlockadeLayer blockadeLayer = new MrlBlockadeLayer();
        blockadeLayer.setVisible(true);
        addLayer(blockadeLayer);

        MrlAmbulanceImprtantBuildingsLayer mrlAmbulanceImprtantBuildingsLayer = new MrlAmbulanceImprtantBuildingsLayer();
        mrlAmbulanceImprtantBuildingsLayer.setVisible(false);
        addLayer(mrlAmbulanceImprtantBuildingsLayer);

        MrlUnvisitedBuildingLayer mrlUnvisitedBuildingLayer = new MrlUnvisitedBuildingLayer();
        mrlUnvisitedBuildingLayer.setVisible(false);
        addLayer(mrlUnvisitedBuildingLayer);

        MrlAreaVisibilityLayer mrlVisibleFromLayer = new MrlAreaVisibilityLayer();
        mrlVisibleFromLayer.setVisible(false);
        addLayer(mrlVisibleFromLayer);

        MrlExtinguishableFromLayer mrlExtinguishFromLayer = new MrlExtinguishableFromLayer();
        mrlExtinguishFromLayer.setVisible(false);
        addLayer(mrlExtinguishFromLayer);

        MrlForbiddenLocationsLayer mrlForbiddenLocationsLayer = new MrlForbiddenLocationsLayer();
        mrlForbiddenLocationsLayer.setVisible(false);
        addLayer(mrlForbiddenLocationsLayer);

        MrlAdvantageRatioLayer mrlAdvantageRatioLayer = new MrlAdvantageRatioLayer();
        mrlAdvantageRatioLayer.setVisible(false);
        addLayer(mrlAdvantageRatioLayer);

        MrlUnvisitedFireBasedBuildingLayer mrlUnvisitedFireBasedBuildingLayer = new MrlUnvisitedFireBasedBuildingLayer();
        mrlUnvisitedFireBasedBuildingLayer.setVisible(false);
        addLayer(mrlUnvisitedFireBasedBuildingLayer);

        MrlConvexHullLayer mrlConvexHullLayer = new MrlConvexHullLayer();
        mrlConvexHullLayer.setVisible(false);
        addLayer(mrlConvexHullLayer);

        MrlRayMoveLayer mrlRayMoveLayer = new MrlRayMoveLayer();
        mrlRayMoveLayer.setVisible(false);
        addLayer(mrlRayMoveLayer);

        MrlKmeansLayer mrlKmeansLayer = new MrlKmeansLayer();
        mrlKmeansLayer.setVisible(false);
        addLayer(mrlKmeansLayer);


//        MrlFireClusterLayer mrlFireClusterLayer = new MrlFireClusterLayer();
//        mrlFireClusterLayer.setVisible(false);
//        addLayer(mrlFireClusterLayer);

        MrlPathLayer mrlPathLayer = new MrlPathLayer();
        mrlPathLayer.setVisible(false);
        addLayer(mrlPathLayer);

        MrlCivilianClusterLayer mrlCivilianClusterLayer = new MrlCivilianClusterLayer();
        mrlCivilianClusterLayer.setVisible(false);
        addLayer(mrlCivilianClusterLayer);

        MrlBuildingValuesLayer mrlBuildingValuesLayer = new MrlBuildingValuesLayer();
        mrlBuildingValuesLayer.setVisible(false);
        addLayer(mrlBuildingValuesLayer);

        MrlConnectedBuildingsLayer connectedBuildingsLayer = new MrlConnectedBuildingsLayer();
        connectedBuildingsLayer.setVisible(false);
        addLayer(connectedBuildingsLayer);

        MrlShouldCheckInsideBuildingsLayer mrlShouldCheckInsideBuildingsLayer = new MrlShouldCheckInsideBuildingsLayer();
        mrlShouldCheckInsideBuildingsLayer.setVisible(false);
        addLayer(mrlShouldCheckInsideBuildingsLayer);

        MrlBurningBuildingLayer mrlBurningBuildingLayer = new MrlBurningBuildingLayer();
        mrlBurningBuildingLayer.setVisible(false);
        addLayer(mrlBurningBuildingLayer);

        MrlEstimatedLayer mrlEstimatedLayer = new MrlEstimatedLayer();
        mrlEstimatedLayer.setVisible(false);
        addLayer(mrlEstimatedLayer);

//        MrlZIOLayer zioLayer = new MrlZIOLayer();
//        zioLayer.setVisible(false);
//        addLayer(zioLayer);

        MrlObjectsValueLayer objectsValueLayer = new MrlObjectsValueLayer();
        objectsValueLayer.setVisible(false);
        addLayer(objectsValueLayer);

        MrlZonePolygonLayer zonePolygonLayer = new MrlZonePolygonLayer();
        zonePolygonLayer.setVisible(false);
        addLayer(zonePolygonLayer);

        MrlPreRoutingPartitionsLayer preRoutingPartitionsLayer = new MrlPreRoutingPartitionsLayer();
        preRoutingPartitionsLayer.setVisible(false);
        addLayer(preRoutingPartitionsLayer);

        MrlPartitionsLayer partitionsLayer = new MrlPartitionsLayer();
        partitionsLayer.setVisible(false);
        addLayer(partitionsLayer);

//        MrlAirCellLayer airCellLayer = new MrlAirCellLayer();
//        airCellLayer.setVisible(false);
//        addLayer(airCellLayer);

        MrlRendezvousLayer mrlRendezvousLayer = new MrlRendezvousLayer();
        mrlRendezvousLayer.setVisible(false);
        addLayer(mrlRendezvousLayer);


        MrlHighwaysLayer highwaysLayer = new MrlHighwaysLayer();
        highwaysLayer.setVisible(false);
        addLayer(highwaysLayer);

        MrlClusterLayer clusterLayer = new MrlClusterLayer();
        clusterLayer.setVisible(false);
        addLayer(clusterLayer);

        MrlTargetPointsLayer mrlTargetPointsLayer = new MrlTargetPointsLayer();
        mrlTargetPointsLayer.setVisible(true);
        addLayer(mrlTargetPointsLayer);

        MrlPoliceTargetClustersLayer policeTargetClustersLayer = new MrlPoliceTargetClustersLayer();
        policeTargetClustersLayer.setVisible(false);
        addLayer(policeTargetClustersLayer);

        AreaIconLayer areaIconLayer = new MrlAreaIconLayer();
        areaIconLayer.setVisible(false);
        addLayer(areaIconLayer);

        addLayer(new PositionHistoryLayer());

        AreaNeighboursLayer neighboursLayer = new AreaNeighboursLayer();
        neighboursLayer.setVisible(false);
        addLayer(neighboursLayer);

        humans = new MrlAnimatedHumanLayer();
        addLayer(humans);

        MrlCommandLayer commands = new MrlCommandLayer();
        commands.setRenderMove(true);
        addLayer(commands);

        addLayer(new MrlLocationLayer());


    }

    @Override
    public void view(Object... objects) {
        super.view(objects);
        synchronized (lock) {
            done = false;
            humans.computeAnimation(FRAME_COUNT);
        }
    }
}