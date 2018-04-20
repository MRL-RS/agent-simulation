package mrl.world.object;

import java.util.ArrayList;
import java.util.List;

/**
 * User: MRL
 * Date: 11/10/13
 * Time: 5:07 PM
 *
 * @Author: Mostafa Shabani
 */
public class AirCell {
    private int cellX;
    private int cellY;
    private double temperature;
    private double totalEnergy;
    private double totalArea;
    private double buildingArea;
    private List<MrlBuilding> buildings = new ArrayList<MrlBuilding>();

    public AirCell(int cellX, int cellY, double totalArea) {
        this.cellX = cellX;
        this.cellY = cellY;
        this.temperature = 0;
        this.totalEnergy = 0;
        this.totalArea = totalArea / 1000000;
    }

    public int getCellX() {
        return cellX;
    }

    public int getCellY() {
        return cellY;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
        totalEnergy = 0;
    }

    public double getTotalEnergy() {
        return totalEnergy;
    }

    public void addTotalEnergy(double totalEnergy) {
        this.totalEnergy += totalEnergy;
    }

    public List<MrlBuilding> getBuildings() {
        return buildings;
    }

    public void addBuilding(MrlBuilding building) {
        this.buildingArea += building.getSelfBuilding().getGroundArea();
        if (building.getSelfBuilding().getGroundArea() <= 0) {
            System.err.println(building + " .getGroundArea() = " + building.getSelfBuilding().getGroundArea());
        }
        this.buildings.add(building);
    }

    public double getTotalArea() {
        return totalArea;
    }

    public double getBuildingArea() {
        return buildingArea;
    }
}
