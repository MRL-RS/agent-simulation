package mrl.police;

import mrl.world.routing.path.Path;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;

/**
 * User: pooyad
 * Date: 7/5/11
 * Time: 7:38 PM
 */
public class TargetToGoOBJ implements Comparable {
    EntityID target;
    List<EntityID> reporters;
    int firstTime;
    double priority;
    Path path;

    public TargetToGoOBJ(EntityID target, int firstTime, Path path) {
        this.target = target;
        this.firstTime = firstTime;
        this.path = path;
        reporters = new ArrayList<EntityID>();
    }

    public void addReporter(EntityID reporter, int value) {
        if (!reporters.contains(reporter)) {
            reporters.add(reporter);
            priority += ((500f / (double) firstTime) * value);
        }
    }

    @Override
    public int compareTo(Object o) {
        TargetToGoOBJ t2 = (TargetToGoOBJ) o;
        if (priority < t2.priority)
            return 1;
        if (priority == t2.priority)
            return 0;

        return -1;
    }
}
