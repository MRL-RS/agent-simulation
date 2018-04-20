package mrl.police.strategies;

import mrl.common.CommandException;
import mrl.common.TimeOutException;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 11/28/12
 *         Time: 8:54 PM
 */
public interface IPoliceActionStrategy {

    /**
     * Preforms police action
     *
     * @throws mrl.common.CommandException
     */
    public void execute() throws CommandException, TimeOutException;

    /**
     * This method handles works that should be done after each  aftershock
     */
    public void doAftershockWork();

}
