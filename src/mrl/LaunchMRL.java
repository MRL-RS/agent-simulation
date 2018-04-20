package mrl;

import mrl.ambulance.MrlAmbulanceCentre;
import mrl.ambulance.MrlAmbulanceTeam;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.firebrigade.MrlFireBrigade;
import mrl.firebrigade.MrlFireStation;
import mrl.platoon.MrlCentre;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.MrlPoliceForce;
import mrl.police.MrlPoliceOffice;
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

import java.io.*;
import java.text.DecimalFormat;

/**
 * Launcher for sample agents. This will launch as many instances of each of the sample agents as possible, all using one connction.
 */
public final class LaunchMRL {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(LaunchMRL.class);

    private static final String FIRE_BRIGADE_FLAG = "-fb";
    private static final String POLICE_FORCE_FLAG = "-pf";
    private static final String AMBULANCE_TEAM_FLAG = "-at";
    private static final String FIRE_STATION_FLAG = "-fs";
    private static final String POLICE_OFFICE_FLAG = "-po";
    private static final String AMBULANCE_CENTRE_FLAG = "-ac";
    private static final String PRECOMPUTE_FLAG = "-precompute";
    private static final String ALL_ON_THREAD_FLAG = "-thr";
    private static final String LAUNCH_VIEWER_FLAG = "-vi";
    private static final String ENHANCED_FLAG = "-enhanced";
    private static final String CIVILIAN_FLAG = "-cv";
    private static boolean firstAgentConnected = false;
    private static Boolean finished = false;
    private static Boolean ambulanceTeamFinished = false;
    private static Boolean policeForceFinished = false;
    private static Boolean fireBrigadeFinished = false;
    private static Boolean fireStationFinished = false;
    private static Boolean ambulanceCenterFinished = false;
    private static Boolean policeOfficeFinished = false;
    private static Boolean enhancedConnection = false;
    private static int agentC = 0;
    private static int DELAY = 1000;
    private static final int NUMBER_OF_CLUSTERS = 3;
    public static boolean shouldPrecompute = false;

    public static boolean DEBUG_MODE;  // This will be initialized in LaunchMRL. By default, this is false until -vi argument found in program arguments.

    private LaunchMRL() {
    }

    /**
     * Launch 'em!
     *
     * @param args The following arguments are understood: -p <port>, -h <hostname>, -fb <fire brigades>, -pf <police forces>, -at <ambulance teams>
     */
    public static void main(String[] args) {
        launchAgents(args);
    }

    public static ComponentLauncher launchAgents(String[] args) {
        //Logger.setLogContext("mrl");
        long start = System.currentTimeMillis();
        try {
            Registry.SYSTEM_REGISTRY.registerEntityFactory(StandardEntityFactory.INSTANCE);
            Registry.SYSTEM_REGISTRY.registerMessageFactory(StandardMessageFactory.INSTANCE);
            Registry.SYSTEM_REGISTRY.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
            Config config = new Config();
            args = CommandLineOptions.processArgs(args, config);
            config.setValue("random.seed", "1");


            int port = config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY, Constants.DEFAULT_KERNEL_PORT_NUMBER);
            String host = config.getValue(Constants.KERNEL_HOST_NAME_KEY, Constants.DEFAULT_KERNEL_HOST_NAME);
            int fb = -1;
            int pf = -1;
            int at = -1;
            int fs = -1;
            int po = -1;
            int ac = -1;            // CHECKSTYLE:OFF:ModifiedControlVariable
            boolean pc = false;
            boolean thr = false;
            boolean launchViewer = false;
            for (int i = 0; i < args.length; ++i) {
                if (args[i].equals(FIRE_BRIGADE_FLAG)) {
                    fb = Integer.parseInt(args[++i]);
                } else if (args[i].equals(FIRE_STATION_FLAG)) {
                    fs = Integer.parseInt(args[++i]);
                } else if (args[i].equals(POLICE_FORCE_FLAG)) {
                    pf = Integer.parseInt(args[++i]);
                } else if (args[i].equals(POLICE_OFFICE_FLAG)) {
                    po = Integer.parseInt(args[++i]);
                } else if (args[i].equals(AMBULANCE_TEAM_FLAG)) {
                    at = Integer.parseInt(args[++i]);
                } else if (args[i].equals(AMBULANCE_CENTRE_FLAG)) {
                    ac = Integer.parseInt(args[++i]);
                } else if (args[i].equals(PRECOMPUTE_FLAG)) {
                    pc = true;
                    shouldPrecompute = pc;
                } else if (args[i].equals(ALL_ON_THREAD_FLAG)) {
                    thr = true;
                } else if (args[i].equals(LAUNCH_VIEWER_FLAG)) {
                    launchViewer = true;
                    LaunchMRL.DEBUG_MODE = true;
                } else if(args[i].equals(ENHANCED_FLAG)){
                    enhancedConnection = true;
                } else {
                    Logger.warn("Unrecognised option: " + args[i]);
                }
            }
            // CHECKSTYLE:ON:ModifiedControlVariable
            ComponentLauncher launcher = new TCPComponentLauncher(host, port, config);
            if(enhancedConnection) {
                try {
                    if(shouldPrecompute){
                        pf = 1;
                        fb = 1;
                        at = 1;
                    }else{
                        String filePath = String.format("%s%s%s", MRLConstants.PRECOMPUTE_DIRECTORY, MrlPlatoonAgent.INTEGER_DATA, MrlFireBrigade.FIRE_BRIGADE_COUNT_EXTENSION);
                        fb = (int) Util.readObject(filePath);
                        filePath = String.format("%s%s%s", MRLConstants.PRECOMPUTE_DIRECTORY, MrlPlatoonAgent.INTEGER_DATA, MrlPoliceForce.POLICE_FORCE_COUNT_EXTENSION);
                        pf = (int) Util.readObject(filePath);
                        filePath = String.format("%s%s%s", MRLConstants.PRECOMPUTE_DIRECTORY, MrlPlatoonAgent.INTEGER_DATA, MrlAmbulanceTeam.AMBULANCE_TEAM_COUNT_EXTENSION);
                        at = (int) Util.readObject(filePath);
                        fb = fb / NUMBER_OF_CLUSTERS + 1;
                        pf = pf / NUMBER_OF_CLUSTERS + 1;
                        at = at / NUMBER_OF_CLUSTERS + 1;
                    }
                    connectEnhanced(launcher, fb, fs, pf, po, at, ac, config, pc, thr, launchViewer);
                }catch (FileNotFoundException e){
                    connect(launcher, fb, fs, pf, po, at, ac, config, pc, thr, launchViewer);
                }
            } else {
                connect(launcher, fb, fs, pf, po, at, ac, config, pc, thr, launchViewer);
            }
            long totalTime = System.currentTimeMillis() - start;
//            System.out.println("total connecting time = " + getTime(totalTime));
            System.out.println("-------======:::::: AGENTS CONNECTED (time = " + getTime(totalTime) + ") ::::::======-------");

            return launcher;
        } catch (IOException | ClassNotFoundException | InterruptedException | ConnectionException e) {
            Logger.error("Error connecting agents", e);
        } catch (ConfigException e) {
            Logger.error("Configuration error", e);
        }

        return null;
    }

    private static String getTime(long totalTime) {
        int min = (int) ((totalTime / 1000 / 60));
        int sec = (int) ((totalTime / 1000) % 60);
        int mil = (int) (totalTime % 1000);
        DecimalFormat format = new DecimalFormat("00");
        return format.format(min) + ":" + format.format(sec) + "." + mil;
    }

    private static void connect(ComponentLauncher launcher, int fb, int fs, int pf, int po, int at, int ac, Config config, boolean precompute, boolean thr, boolean launchViewer) throws InterruptedException, ConnectionException {
        try {
            if (precompute) {
                File data = new File("precompute");
                if (!data.exists() || !data.isDirectory()) {
                    data.mkdir();
                } else {
                    for (File f : data.listFiles()) {
                        if (!f.isDirectory()) {
                            f.delete();
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        int i = 0;
        try {
            finished = false;
            while (fb-- != 0 && !finished) {
                Logger.info("Connecting fire brigade " + (i++) + "...");
                agentThread(launcher, new MrlFireBrigade(), thr);
            }
        } catch (ComponentConnectionException e) {
            agentC--;
            Logger.info("failed: " + e.getMessage());
        }
        try {
            finished = false;
            while (fs-- != 0 && !finished) {
                Logger.info("Connecting fire station " + (i++) + "...");
                agentThread(launcher, new MrlFireStation(), thr);
            }
        } catch (ComponentConnectionException e) {
            agentC--;
            Logger.info("failed: " + e.getMessage());
        }
        try {
            finished = false;
            while (pf-- != 0 && !finished) {
                Logger.info("Connecting police force " + (i++) + "...");
                agentThread(launcher, new MrlPoliceForce(), thr);
            }
        } catch (ComponentConnectionException e) {
            agentC--;
            Logger.info("failed: " + e.getMessage());
        }
        try {
            finished = false;
            while (po-- != 0 && !finished) {
                Logger.info("Connecting police office " + (i++) + "...");
                agentThread(launcher, new MrlPoliceOffice(), thr);
            }
        } catch (ComponentConnectionException e) {
            agentC--;
            Logger.info("failed: " + e.getMessage());
        }

        try {
            finished = false;
            while (at-- != 0 && !finished) {
                Logger.info("Connecting ambulance team " + (i++) + "...");
                agentThread(launcher, new MrlAmbulanceTeam(), thr);
            }
        } catch (ComponentConnectionException e) {
            agentC--;
            Logger.info("failed: " + e.getMessage());
        }
        try {
            finished = false;
            while (ac-- != 0 && !finished) {
                Logger.info("Connecting ambulance center " + (i++) + "...");
                agentThread(launcher, new MrlAmbulanceCentre(), thr);
            }
        } catch (ComponentConnectionException e) {
            agentC--;
            Logger.info("failed: " + e.getMessage());
        }

        while (agentC != 0) {
            Thread.sleep(1000);
        }

        if (launchViewer) {    //todo:uncomment for MRL VIEWER
            try {
                Logger.info("Connecting viewer ...");
                launcher.connect(MrlPersonalData.VIEWER_DATA.viewerConstructor());
                Logger.info("success");
            } catch (ComponentConnectionException e) {
                Logger.info("failed: " + e.getMessage());
            }
        }

//        System.out.println("-------======:::::: ALL AGENTS CONNECTED ::::::======-------");

        try {
            FileWriter fileWriter = new FileWriter("messageDebug.txt", true);
            BufferedWriter bw = new BufferedWriter(fileWriter);
            bw.close();
        } catch (Exception ignore) {
        }
    }

    private static void connectEnhanced(ComponentLauncher launcher, int fb, int fs, int pf, int po, int at, int ac, Config config, boolean precompute, boolean thr, boolean launchViewer) throws InterruptedException, ConnectionException {
        try {
            if (precompute) {
                File data = new File("precompute");
                if (!data.exists() || !data.isDirectory()) {
                    data.mkdir();
                } else {
                    for (File f : data.listFiles()) {
                        if (!f.isDirectory()) {
                            f.delete();
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        int i = 0;

        while (fb != 0 || at != 0 || pf != 0) {
            try {
                if (fb != 0 && !fireBrigadeFinished) {
                    fb--;
                    Logger.info("Connecting agent " + (i++) + "...");
                    agentThread(launcher, new MrlFireBrigade(), thr);
                }
            } catch (ComponentConnectionException e) {
                agentC--;
                Logger.info("failed: " + e.getMessage());
            }
            try {
                if (at != 0 && !ambulanceTeamFinished) {
                    at--;
                    Logger.info("Connecting agent " + (i++) + "...");
                    agentThread(launcher, new MrlAmbulanceTeam(), thr);
                }
            } catch (ComponentConnectionException e) {
                agentC--;
                Logger.info("failed: " + e.getMessage());
            }
            try {
                if (pf != 0 && !policeForceFinished) {
                    pf--;
                    Logger.info("Connecting agent " + (i++) + "...");
                    agentThread(launcher, new MrlPoliceForce(), thr);
                }
            } catch (ComponentConnectionException e) {
                agentC--;
                Logger.info("failed: " + e.getMessage());
            }
        }

        try {
            finished = false;
            while (fs-- != 0 && !finished) {
                Logger.info("Connecting fire station " + (i++) + "...");
                agentThread(launcher, new MrlFireStation(), thr);
            }
        } catch (ComponentConnectionException e) {
            agentC--;
            Logger.info("failed: " + e.getMessage());
        }
        try {
            finished = false;
            while (po-- != 0 && !finished) {
                Logger.info("Connecting police office " + (i++) + "...");
                agentThread(launcher, new MrlPoliceOffice(), thr);
            }
        } catch (ComponentConnectionException e) {
            agentC--;
            Logger.info("failed: " + e.getMessage());
        }

        try {
            finished = false;
            while (ac-- != 0 && !finished) {
                Logger.info("Connecting ambulance center " + (i++) + "...");
                agentThread(launcher, new MrlAmbulanceCentre(), thr);
            }
        } catch (ComponentConnectionException e) {
            agentC--;
            Logger.info("failed: " + e.getMessage());
        }

        while (agentC != 0) {
            Thread.sleep(1000);
        }

        if (launchViewer) {    //todo:uncomment for MRL VIEWER
            try {
                Logger.info("Connecting viewer ...");
                launcher.connect(MrlPersonalData.VIEWER_DATA.viewerConstructor());
                Logger.info("success");
            } catch (ComponentConnectionException e) {
                Logger.info("failed: " + e.getMessage());
            }
        }

//        System.out.println("-------======:::::: ALL AGENTS CONNECTED ::::::======-------");

        try {
            FileWriter fileWriter = new FileWriter("messageDebug.txt", true);
            BufferedWriter bw = new BufferedWriter(fileWriter);
            bw.close();
        } catch (Exception ignore) {
        }
    }

    private static void agentThread(final ComponentLauncher launcher, final MrlCentre mrlCentre, boolean thr) throws InterruptedException, ComponentConnectionException, ConnectionException {
        if (!firstAgentConnected) {
            if (thr) {
                firstAgentConnected = true;
            }
            agentC++;
            launcher.connect(mrlCentre);
            agentC--;
            Logger.info("success");
        } else {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        agentC++;
                        launcher.connect(mrlCentre);
                        agentC--;
                        Logger.info("success");
                    } catch (InterruptedException ignore) {
                    } catch (ConnectionException e) {
//                        agentC --;
//                        finished = true;
                    } catch (ComponentConnectionException e) {
                        agentC--;
                        finished = true;
                        if(mrlCentre instanceof MrlFireStation){
                            fireStationFinished = true;
                        } else if(mrlCentre instanceof MrlPoliceOffice){
                            policeOfficeFinished = true;
                        } else if(mrlCentre instanceof MrlAmbulanceCentre){
                            ambulanceCenterFinished = true;
                        }
                    }
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
            Thread.sleep(DELAY);
        }
    }

    private static void agentThread(final ComponentLauncher launcher, final MrlPlatoonAgent<?> agent, boolean thr) throws InterruptedException, ComponentConnectionException, ConnectionException {
        if (!firstAgentConnected) {
            if (thr) {
                firstAgentConnected = true;
            }
            agentC++;
            launcher.connect(agent);
            agentC--;
            Logger.info("success");
        } else {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        agentC++;
                        launcher.connect(agent);
                        agentC--;
                        Logger.info("success");
                    } catch (InterruptedException ignore) {
                        ignore.printStackTrace();
                    } catch (ConnectionException e) {
//                        agentC --;
//                        finished = true;
                    } catch (ComponentConnectionException e) {
                        agentC--;
                        finished = true;
                        if(agent instanceof MrlFireBrigade){
                            fireBrigadeFinished = true;
                        } else if(agent instanceof MrlAmbulanceTeam){
                            ambulanceTeamFinished = true;
                        } else if(agent instanceof MrlPoliceForce){
                            policeForceFinished = true;
                        }

                    }
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
            Thread.sleep(DELAY);
        }
    }
}