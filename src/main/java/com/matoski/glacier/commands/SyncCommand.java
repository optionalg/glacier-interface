package com.matoski.glacier.commands;

import com.amazonaws.Protocol;
import com.matoski.glacier.base.AbstractCommand;
import com.matoski.glacier.cli.CommandSync;
import com.matoski.glacier.enums.Metadata;
import com.matoski.glacier.errors.RegionNotSupportedException;
import com.matoski.glacier.errors.VaultNameNotPresentException;
import com.matoski.glacier.pojo.Config;
import com.matoski.glacier.pojo.journal.State;
import com.matoski.glacier.util.AmazonGlacierBaseUtil;
import com.matoski.glacier.util.Helpers;
import com.matoski.glacier.util.upload.AmazonGlacierUploadUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

/**
 * Synchronizes a directory
 *
 * @author Ilija Matoski (ilijamt@gmail.com)
 */
public class SyncCommand extends AbstractCommand<CommandSync> {

    /**
     * List of files.
     */
    protected Collection<String> files = new LinkedList<String>();
    /**
     * Metadata.
     */
    protected Metadata metadata;
    /**
     * Journal.
     */
    private State journal;

    /**
     * Constructor.
     *
     * @param config  Application config
     * @param command The command configuration
     * @throws VaultNameNotPresentException Vault not present in config
     * @throws RegionNotSupportedException  Region not supported
     */
    public SyncCommand(Config config, CommandSync command) throws VaultNameNotPresentException,
            RegionNotSupportedException {
        super(config, command);

        Boolean validVaultName = null != command.vaultName;
        Boolean validVaultNameConfig = null != config.getVault();

        if (!validVaultName && !validVaultNameConfig) {
            throw new VaultNameNotPresentException();
        }

        if (validVaultNameConfig) {
            command.vaultName = config.getVault();
        }

        if (command.partSize % 2 != 0 && command.partSize != 1) {
            throw new IllegalArgumentException("Part size has to be a multiple of 2");
        }

        command.partSize = command.partSize * (int) AmazonGlacierBaseUtil.MINIMUM_PART_SIZE;

        try {
            this.journal = State.load(command.journal);
        } catch (IOException e) {
            System.out.println(String.format("Creating a new journal: %s", command.journal));
            this.journal = new State();
            this.journal.setMetadata(command.metadata);
            this.journal.setName(command.vaultName);
            this.journal.setDate(new Date());
            this.journal.setFile(command.journal);
        }

    }

    @Override
    public void setProtocol() {
        if (this.command.forceHttpConnection) {
            protocol = Protocol.HTTP;
        }
    }

    @Override
    public void run() {

        System.out.println("START: sync\n");

        AmazonGlacierUploadUtil upload = new AmazonGlacierUploadUtil(credentials, client, region);
        Boolean exists = false;
        String processFile;

        Collection<File> result = FileUtils.listFiles(new File(config.getDirectory()), null, true);

        for (File file : result) {

            if (!Helpers.verifyIsValidStateFile(file.getAbsolutePath())) {
                processFile = file.getAbsolutePath().replace(config.getDirectory() + "/", "");
                files.add(processFile);
            }

        }

        System.out.println(String.format("%s files found%n", files.size()));

        for (String fileName : files) {

            exists = journal.isFileInArchive(fileName);

            if (!command.uploadReplaceModified && exists) {
                System.out.println(String.format("%s is already present in the journal, skipping",
                        fileName));
                continue;
            }

            upload.uploadArchive(journal, command.vaultName, fileName, false, command.concurrent,
                    command.retryFailedUpload, command.partSize, command.uploadReplaceModified,
                    command.dryRun);
        }

        System.out.println("\nEND: sync");
    }
}
