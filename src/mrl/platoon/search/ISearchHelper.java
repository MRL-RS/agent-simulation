package mrl.platoon.search;

import mrl.common.CommandException;
import rescuecore2.worldmodel.EntityID;

import java.util.List;

/**
 * Created by Mostafa Shabani
 * Date: 6/23/11
 * Time: 4:21 PM
 */
public interface ISearchHelper {

    public void breadthFirstSearch(boolean inPartition) throws CommandException;


    public List<EntityID> listBasedBreadthFirstSearch(boolean inPartition) throws CommandException;

    public void zoneSearch() throws CommandException;

    public void rendezvousAction() throws CommandException;

    public void stopNearCiviliansAndReport() throws CommandException;

}
