package mrl.ambulance.targetSelector;

import rescuecore2.standard.entities.StandardEntity;

import java.util.Set;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 2/21/13
 *         Time: 1:46 PM
 */
public interface ITargetSelector {

    /**
     * Finds best target between specified possible targets
     *
     * @param victims targets to search between them
     * @return best target to select
     */
    public StandardEntity nextTarget(Set<StandardEntity> victims);

}
