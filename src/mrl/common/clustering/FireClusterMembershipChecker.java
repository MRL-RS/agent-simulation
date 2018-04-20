package mrl.common.clustering;

import mrl.world.object.MrlBuilding;

/**
 * This implementation of {@link IClusterMembershipChecker} checks for membership of {@link MrlBuilding} objects in
 * fire clusters based on real temperature of objects.<br/>
 * Acceptable Object types:
 * <ul>
 * <li>{@link MrlBuilding}</li>
 * </ul>
 *
 * @author Siavash
 * @see FireClusterMembershipEstimator
 */
public class FireClusterMembershipChecker implements IClusterMembershipChecker {

    /**
     * Checks membership of the provided {@code object} in any fire cluster. Any {@link MrlBuilding} with real temperature higher
     * than 25 are considered members.
     *
     * @param object Object to be checked for membership. object must be of type: {@link MrlBuilding}.
     * @return true if provided {@link MrlBuilding} object is considered member of a cluster, false otherwise.
     * @throws IllegalArgumentException if object is null or not of type: {@link MrlBuilding}.
     */
    @Override
    public boolean checkMembership(Object object) {
        boolean member = false;
        MrlBuilding building = null;
        if (object instanceof MrlBuilding) {
            building = (MrlBuilding) object;
            if (building.getSelfBuilding() != null
                    && building.getSelfBuilding().isTemperatureDefined()
                    && building.getRealTemperature() > 25) {
                member = true;
            }
        } else {
            throw new IllegalArgumentException("Argument object of type:" + object.getClass() + " is illegal.");
        }
        return member;
    }

}
