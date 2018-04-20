package mrl.mrlPersonal.viewer;

import rescuecore2.standard.view.*;

/**
 * Created by Mostafa Shabani.
 * Date: Jun 23, 2011
 * Time: 11:19:57 PM
 */
public class MrlStandardWorldModelViewer extends MrlLayerViewComponent {
    /**
     * Construct a standard world model viewer.
     */
    public MrlStandardWorldModelViewer() {
        addDefaultLayers();
    }

    @Override
    public String getViewerName() {
        return "MRL Standard world model viewer";
    }

    /**
     * Add the default layer set, i.e. nodes, roads, buildings, humans and commands.
     */
    public void addDefaultLayers() {
        addLayer(new BuildingLayer());
        addLayer(new RoadLayer());
        addLayer(new AreaNeighboursLayer());
        addLayer(new RoadBlockageLayer());
        addLayer(new BuildingIconLayer());
        addLayer(new HumanLayer());
        addLayer(new CommandLayer());
        addLayer(new PositionHistoryLayer());
    }
}