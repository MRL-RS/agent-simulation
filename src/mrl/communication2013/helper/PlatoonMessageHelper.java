package mrl.communication2013.helper;

import mrl.communication2013.message.MessageManager;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/20/13
 * Time: 4:34 PM
 * Author: Mostafa Movahedi
 */
public class PlatoonMessageHelper {
    protected MrlWorld world;
    protected MessageManager messageManager;
    protected MrlPlatoonAgent platoonAgent;

    public PlatoonMessageHelper(MrlWorld world, MrlPlatoonAgent platoonAgent, MessageManager messageManager) {
        this.world = world;
        this.messageManager = messageManager;
        this.platoonAgent = platoonAgent;
    }
}
