package mrl.world.routing.grid;

import javolution.util.FastSet;
import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Mostafa Shabani.
 * Date: Sep 13, 2010
 * Time: 11:23:28 AM
 */
public class Grid {
    private int id;
    // grid center
    private Pair<Integer, Integer> position;
    // 4 vertex grid. ke az paeen smate chap shoroo va pad sa@t gard edame dare.
    private ArrayList<Pair<Integer, Integer>> vertices = new ArrayList<Pair<Integer, Integer>>();
    // oon area-ei ke tooshe.
    private EntityID selfAreaId;
    private boolean isPassable;
    // ye list az blockade haei ke dar in grid gharar dare.
    private List<EntityID> blockades = new ArrayList<EntityID>();
    // age nazdike yek edge-e passable bashe true mishe.
    private boolean onPassableEdge;
    // oon area haei ke in grid rooye edge shon gharar dare toosh rikhte mishe.
    private List<EntityID> neighbourAreaIds = new ArrayList<EntityID>();
    public int selfValue = 1;
    // meghdare aslie faseleye hamsaye ha az in grid
    private Set<Pair<Grid, Integer>> mainNeighbours = new FastSet<Pair<Grid, Integer>>();
    // grid haye hamsayeye in grid
    private List<Pair<Grid, Integer>> neighbours = new ArrayList<Pair<Grid, Integer>>(); // all around neighbour
    // polygin in grid
    private Polygon polygon = new Polygon();
    // te'dade intersect haye in grid ba blockade ha.
    private int intersectCounter;
    // for A* ...
    private Integer parent;
    private int cost;
    private int depth;
    private int heuristic;
    private int g;


    public Grid(int id, EntityID areaId, Pair<Integer, Integer> position) {
        this.id = id;
        this.selfAreaId = areaId;
        this.position = position;
        this.isPassable = true;
        this.onPassableEdge = false;
    }

    public void setPosition(Pair<Integer, Integer> position) {
        this.position = position;
    }

    public void setPassable(boolean passable, EntityID blockadeId) {
        isPassable = passable;
        if (blockadeId != null) {
            blockades.add(blockadeId);
            changeNeighbourValue(10);
            selfValue += 1;
        }
        if (passable) {
            resetNeighbourValue();
            blockades.clear();
        }
    }

    public void addNeighbour(Pair<Grid, Integer> neighbour) {
        this.mainNeighbours.add(neighbour);
    }

    public void addVertex(Pair<Integer, Integer> vertex) {
        this.vertices.add(vertex);
    }

    public void addNeighbourAreaId(EntityID neighbourAreaId) {
        // area haei ke in grid rooye edge shon gharar dare add mishn.
        this.neighbourAreaIds.add(neighbourAreaId);
        this.onPassableEdge = true;
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }

    public void increaseIntersectCounter() {
        this.intersectCounter++;
    }

    public void resetIntersectCounter() {
        intersectCounter = 0;
    }

    public void setParent(Integer parent) {
        this.parent = parent;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setHeuristic(int heuristic) {
        this.heuristic = heuristic;
    }

    public void setG(int g) {
        this.g = g;
    }

    public int getId() {
        return id;
    }

    public Pair<Integer, Integer> getPosition() {
        return position;
    }

    public EntityID getSelfAreaId() {
        return selfAreaId;
    }

    public boolean isPassable() {
        return isPassable;
    }

    public List<EntityID> getBlockades() {
        return blockades;
    }

    public boolean isOnPassableEdge() {
        return onPassableEdge;
    }

    public List<Pair<Grid, Integer>> getNeighbours() {
        if (neighbours.isEmpty()) {
            neighbours.addAll(mainNeighbours);
        }
        return neighbours;
    }

    public ArrayList<Pair<Integer, Integer>> getVertices() {
        return vertices;
    }

    public List<EntityID> getNeighbourAreaIds() {
        // areaye hamsaye ro ke rooye edgesh gharar dare barmigardoone.
        return neighbourAreaIds;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public int getIntersectCounter() {
        return intersectCounter;
    }

    public Integer getParent() {
        return parent;
    }

    public int getCost() {
        return cost;
    }

    public int getDepth() {
        return depth;
    }

    public int getHeuristic() {
        return heuristic;
    }

    public int getG() {
        return g;
    }

    public void changeNeighbourValue(double v) {
        List<Pair<Grid, Integer>> newNeighbours = new ArrayList<Pair<Grid, Integer>>();

        for (Pair<Grid, Integer> grid : neighbours) {
            newNeighbours.add(new Pair<Grid, Integer>(grid.first(), (int) Math.round(grid.second() * v)));
        }

        neighbours = newNeighbours;
    }

    private void resetNeighbourValue() {
        neighbours.clear();
        neighbours.addAll(mainNeighbours);
        selfValue = 1;
    }
}