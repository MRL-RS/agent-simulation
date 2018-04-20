package mrl.mrlPersonal;

import mrl.mrlPersonal.viewer.MrlViewer;
import rescuecore2.Constants;
import rescuecore2.components.ComponentConnectionException;
import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.config.Config;
import rescuecore2.config.ConfigException;
import rescuecore2.connection.ConnectionException;
import rescuecore2.misc.CommandLineOptions;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;

import java.io.IOException;

/**
 * Launcher for sample agents. This will launch as many instances of each of the sample agents as possible, all using one connction.
 */
public final class LaunchMRLViewer {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(LaunchMRLViewer.class);

    private LaunchMRLViewer() {
    }

    /**
     * Launch 'em!
     *
     * @param args The following arguments are understood: -p <port>, -h <hostname>, -fb <fire brigades>, -pf <police forces>, -at <ambulance teams>
     */
    public static void main(String[] args) {
        //Logger.setLogContext("mrlviewer");
        try {
            Registry.SYSTEM_REGISTRY.registerEntityFactory(StandardEntityFactory.INSTANCE);
            Registry.SYSTEM_REGISTRY.registerMessageFactory(StandardMessageFactory.INSTANCE);
            Registry.SYSTEM_REGISTRY.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
            Config config = new Config();
            args = CommandLineOptions.processArgs(args, config);
            int port = config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY, Constants.DEFAULT_KERNEL_PORT_NUMBER);
            String host = config.getValue(Constants.KERNEL_HOST_NAME_KEY, Constants.DEFAULT_KERNEL_HOST_NAME);

            ComponentLauncher launcher = new TCPComponentLauncher(host, port, config);
            connect(launcher, config);
        } catch (IOException e) {
            Logger.error("Error connecting agents", e);
        } catch (ConfigException e) {
            Logger.error("Configuration error", e);
        } catch (ConnectionException e) {
            Logger.error("Error connecting agents", e);
        } catch (InterruptedException e) {
            Logger.error("Error connecting agents", e);
        }
    }

    private static void connect(ComponentLauncher launcher, Config config) throws InterruptedException, ConnectionException {
        try {
            Logger.info("Connecting viewer ...");
            launcher.connect(new MrlViewer());
            Logger.info("success");
        } catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }
    }
}