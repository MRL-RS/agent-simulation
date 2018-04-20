package mrl.firebrigade.extinguishBehaviour;

import mrl.common.CommandException;
import mrl.common.TimeOutException;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.firebrigade.targetSelection.FireBrigadeTarget;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 3/11/13
 * Time: 6:56 PM
 * Author: Mostafa Movahedi
 */
public interface IExtinguishBehaviour {

    /**
     * Extinguish the specified target
     *
     * @param world  world model object
     * @param target target to extinguish
     */
    public void extinguish(MrlFireBrigadeWorld world, FireBrigadeTarget target) throws CommandException, TimeOutException;

}
