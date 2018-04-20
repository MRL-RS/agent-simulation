package mrl.partition;

import rescuecore2.standard.entities.StandardEntity;

import java.util.Comparator;

/**
 * Created by P.D.G
 * User: root
 * Date: Jan 4, 2010
 * Time: 4:26:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class AgentDef {

    private StandardEntity standardEntity;
    private int type;  // type=0 is PoliceForce 1 is FireBrigade and 2 is AmbulanceTeam

    public AgentDef(StandardEntity standardEntity, int type) {
        this.standardEntity = standardEntity;
        this.type = type;
    }

    public StandardEntity getStandardEntity() {
        return standardEntity;
    }

    public int getType() {
        return type;
    }


    public static Comparator AgentsIDComparator = new Comparator() {
        public int compare(Object o1, Object o2) {

            AgentDef a1 = (AgentDef) o1;
            AgentDef a2 = (AgentDef) o2;

            if (a1.getStandardEntity().getID().getValue() > a2.getStandardEntity().getID().getValue())
                return 1;
            if (a1.getStandardEntity().getID() == a2.getStandardEntity().getID())
                return 0;

            return -1;

        }
    };
}
