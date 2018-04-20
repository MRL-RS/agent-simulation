package mrl.firebrigade.sterategy;

import mrl.common.CommandException;
import mrl.common.TimeOutException;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 11/28/12
 *         Time: 8:54 PM
 */
public interface IFireBrigadeActionStrategy {

    /**
     * Preforms FireBrigade action
     *
     * @throws mrl.common.CommandException
     */
    public void execute() throws CommandException, TimeOutException;

    /**
     * gets type of the action strategy
     */
    public FireBrigadeActionStrategyType getType();


}
