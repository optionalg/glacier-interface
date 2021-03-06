package com.matoski.glacier;

import com.beust.jcommander.JCommander;
import com.google.gson.JsonSyntaxException;
import com.matoski.glacier.base.GenericCommand;
import com.matoski.glacier.cli.*;
import com.matoski.glacier.commands.*;
import com.matoski.glacier.enums.CliCommands;
import com.matoski.glacier.errors.RegionNotSupportedException;
import com.matoski.glacier.errors.VaultNameNotPresentException;
import com.matoski.glacier.pojo.Config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * The main application, this is used to load the application and all the capabilities.
 *
 * @author Ilija Matoski (ilijamt@gmail.com)
 */
public class Main {

    /**
     * A list of available commands, contains a list of objects extending from
     * {@linkplain GenericCommand}.
     */
    private static final HashMap<Integer, Object> commands = new HashMap<Integer, Object>();

    /**
     * The commander parser
     */
    private static JCommander commander = null;

    /**
     * Initializes the applications commands, this creates a list of available commands that can be
     * used.
     */
    public static void init() {

        commands.put(CliCommands.Version.ordinal(), new CommandVersion());
        commands.put(CliCommands.Help.ordinal(), new CommandHelp());
        commands.put(CliCommands.ListJournal.ordinal(), new CommandListJournal());
        commands.put(CliCommands.ListVaults.ordinal(), new CommandListVaults());
        commands.put(CliCommands.CreateVault.ordinal(), new CommandCreateVault());
        commands.put(CliCommands.DeleteVault.ordinal(), new CommandDeleteVault());
        commands.put(CliCommands.ListVaultJobs.ordinal(), new CommandListVaultJobs());
        commands.put(CliCommands.VaultJobInfo.ordinal(), new CommandVaultJobInfo());
        commands.put(CliCommands.InventoryRetrieve.ordinal(), new CommandInventoryRetrieval());
        commands.put(CliCommands.InventoryDownload.ordinal(), new CommandInventoryDownload());
        commands.put(CliCommands.UploadArchive.ordinal(), new CommandUploadArchive());
        commands.put(CliCommands.DeleteArchive.ordinal(), new CommandDeleteArchive());
        commands.put(CliCommands.ListMultipartUploads.ordinal(), new CommandListMultipartUploads());
        commands.put(CliCommands.MultipartUploadInfo.ordinal(), new CommandMultipartUploadInfo());
        commands.put(CliCommands.AbortMultipartUpload.ordinal(), new CommandAbortMultipartUpload());
        commands.put(CliCommands.DownloadJob.ordinal(), new CommandDownloadJob());
        commands.put(CliCommands.InitDownload.ordinal(), new CommandInitDownload());
        commands.put(CliCommands.PurgeVault.ordinal(), new CommandPurgeVault());
        commands.put(CliCommands.Sync.ordinal(), new CommandSync());
        commands.put(CliCommands.VerifyJournal.ordinal(), new CommandVerifyJournal());

    }

    public static void processHelp() {
        processHelp(CliCommands.Help.getPropertyName());
    }

    public static void processHelp(String command) {

        CliCommands cmd = CliCommands.Help;
        if (command != null) {
            cmd = CliCommands.from(command);
        }

        switch (cmd) {
            case Help:
                CommandHelp help = (CommandHelp) commands.get(CliCommands.Help.ordinal());
                if (help.command.isEmpty()) {
                    // No extra commands supplied so we display the whole help
                    commander.usage();
                } else {
                    Iterator<String> iterator = help.command.iterator();
                    while (iterator.hasNext()) {
                        commander.usage(iterator.next());
                    }
                }
                break;

            default:
                commander.usage(command);
                break;
        }

    }

    /**
     * Entry point for application.
     *
     * @param args The command line arguments
     */
    public static void main(String[] args) {

        System.out.println(String.format("Glacier Interface (v%s), Copyright 2014, Ilija Matoski",
                Constants.VERSION));
        System.out.println();

        Arguments arguments = new Arguments();
        String command = CliCommands.Help.getPropertyName();

        init();

        try {

            commander = new JCommander(arguments);
            for (Entry<Integer, Object> entry : commands.entrySet()) {
                commander.addCommand(entry.getValue());
            }

            commander.parse(args);
            command = commander.getParsedCommand();

        } catch (Exception e) {

            if (commander.getParsedCommand() == null) {
                System.err.println(String.format("ERROR: Unknown command, try help"));
            } else {
                commander.usage(commander.getParsedCommand());
                System.err.print("ERROR: ");
                System.err.println(e.getMessage());
            }

            System.exit(1);
        }

        Config config = null;
        Boolean hasConfig = !(null == arguments.config);

        if (hasConfig) {
            try {
                config = Config.fromFile(arguments.config);
                config.merge(arguments);
            } catch (JsonSyntaxException e) {
                System.err.println("ERROR: Invalid format in the configuration file");
                System.exit(1);
            } catch (FileNotFoundException e) {
                System.err.println("ERROR: Config file cannot be found");
                System.exit(1);
            } catch (IOException e) {
                System.err.println("ERROR: Config file cannot be found");
                System.exit(1);
            }
        } else {
            config = Config.fromArguments(arguments);
        }

        Boolean validCommand = null != command;
        Boolean validConfig = false;
        CliCommands cliCommand = CliCommands.Help;

        if (validCommand) {
            cliCommand = CliCommands.from(command);
            switch (cliCommand) {
                case Help:
                case Version:
                case ListJournal:
                case VerifyJournal:
                    validConfig = true;
                    break;

                default:
                    validConfig = config.valid(false);
                    break;
            }
        }

        System.out.println(String.format("Current working directory: %s", config.getDirectory()));
        System.out.println(String.format("Command: %s", cliCommand));
        System.out.println();

        if (!validCommand || !validConfig) {

            processHelp(command);

            if (cliCommand != CliCommands.Help && !validConfig) {

                System.out.println("ERROR: Missing one or more required parameters");
                if (null == config.getKey()) {
                    System.out.println("\t--aws-key");
                }

                if (null == config.getSecretKey()) {
                    System.out.println("\t--aws-secret-key");
                }

                if (null == config.getRegion()) {
                    System.out.println("\t--aws-region");
                }

            }

        } else {

            try {

                final int commandOrdinal = cliCommand.ordinal();

                switch (cliCommand) {

                    case Version:
                        new VersionCommand(config, (CommandVersion) commands.get(commandOrdinal)).run();
                        break;

                    case Help:
                        processHelp();
                        break;

                    case ListJournal:
                        new ListJournalCommand(config, (CommandListJournal) commands.get(commandOrdinal))
                                .run();
                        break;

                    case ListVaults:
                        new ListVaultsCommand(config, (CommandListVaults) commands.get(commandOrdinal)).run();
                        break;

                    case CreateVault:
                        new CreateVaultCommand(config, (CommandCreateVault) commands.get(commandOrdinal))
                                .run();
                        break;

                    case DeleteVault:
                        new DeleteVaultCommand(config, (CommandDeleteVault) commands.get(commandOrdinal))
                                .run();
                        break;

                    case ListVaultJobs:
                        new ListVaultJobsCommand(config, (CommandListVaultJobs) commands.get(commandOrdinal))
                                .run();
                        break;

                    case VaultJobInfo:
                        new VaultJobInfoCommand(config, (CommandVaultJobInfo) commands.get(commandOrdinal))
                                .run();
                        break;

                    case InventoryRetrieve:
                        new InventoryRetrievalCommand(config,
                                (CommandInventoryRetrieval) commands.get(commandOrdinal)).run();
                        break;

                    case InventoryDownload:
                        new InventoryDownloadCommand(config,
                                (CommandInventoryDownload) commands.get(commandOrdinal)).run();
                        break;

                    case DeleteArchive:
                        new DeleteArchiveCommand(config, (CommandDeleteArchive) commands.get(commandOrdinal))
                                .run();
                        break;

                    case UploadArchive:
                        new UploadArchiveCommand(config, (CommandUploadArchive) commands.get(commandOrdinal))
                                .run();
                        break;

                    case ListMultipartUploads:
                        new ListMultipartUploadsCommand(config,
                                (CommandListMultipartUploads) commands.get(commandOrdinal)).run();
                        break;

                    case MultipartUploadInfo:
                        new MultipartUploadInfoCommand(config,
                                (CommandMultipartUploadInfo) commands.get(commandOrdinal)).run();
                        break;

                    case AbortMultipartUpload:
                        new AbortMultipartUploadCommand(config,
                                (CommandAbortMultipartUpload) commands.get(commandOrdinal)).run();
                        break;

                    case DownloadJob:
                        new DownloadJobCommand(config, (CommandDownloadJob) commands.get(commandOrdinal))
                                .run();
                        break;

                    case InitDownload:
                        new InitDownloadCommand(config, (CommandInitDownload) commands.get(commandOrdinal))
                                .run();
                        break;

                    case PurgeVault:
                        new PurgeVaultCommand(config, (CommandPurgeVault) commands.get(commandOrdinal)).run();
                        break;

                    case Sync:
                        new SyncCommand(config, (CommandSync) commands.get(commandOrdinal)).run();
                        break;

                    case VerifyJournal:
                        new VerifyJournalCommand(config, (CommandVerifyJournal) commands.get(commandOrdinal))
                                .run();
                        break;

                    default:
                        break;

                }
            } catch (FileNotFoundException e) {
                System.err.println(String.format("ERROR: %s", e.getMessage()));
            } catch (IllegalArgumentException e) {
                System.err.println(String.format("ERROR: %s", e.getMessage()));
            } catch (RuntimeException e) {
                System.err.println(String.format("ERROR: %s", e.getMessage()));
            } catch (RegionNotSupportedException e) {
                System.err.println(String.format("Service glacier not support in region: %s",
                        config.getRegion()));
            } catch (VaultNameNotPresentException e) {
                System.err.println("ERROR: Missing one or more required parameters");
                System.err.println("\t--aws-vault or --vault");
            } catch (Exception e) {
                System.err.println(e);
                System.exit(1);
            }
        }

        if (null != arguments.createConfig) {
            try {
                config.createConfigurationFile(arguments.createConfig);
                System.out.println(String.format("Created a configuration file: %s",
                        arguments.createConfig));
            } catch (IOException e) {
                System.err.println("ERROR: Failed to write the configuration");
                e.printStackTrace();
                System.exit(1);
            }
        }

        System.out.println();
        System.out.println("Finished");

    }

}
