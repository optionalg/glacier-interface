package com.matoski.glacier.commands;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.matoski.glacier.base.AbstractCommand;
import com.matoski.glacier.cli.CommandUploadArchive;
import com.matoski.glacier.enums.ArchiveState;
import com.matoski.glacier.enums.GenericValidateEnum;
import com.matoski.glacier.enums.Metadata;
import com.matoski.glacier.errors.RegionNotSupportedException;
import com.matoski.glacier.errors.UploadTooManyPartsException;
import com.matoski.glacier.errors.VaultNameNotPresentException;
import com.matoski.glacier.pojo.Archive;
import com.matoski.glacier.pojo.Config;
import com.matoski.glacier.pojo.journal.State;
import com.matoski.glacier.util.AmazonGlacierBaseUtil;
import com.matoski.glacier.util.upload.AmazonGlacierUploadUtil;

/**
 * Upload archive
 * 
 * @author ilijamt
 */
public class UploadArchiveCommand extends AbstractCommand<CommandUploadArchive> {

    /**
     * Metadata
     */
    protected Metadata metadata;

    /**
     * The helper utility for uploading
     */
    protected AmazonGlacierUploadUtil upload;

    /**
     * The journal, we use this for storing the data
     */
    protected State journal;

    /**
     * Constructor
     * 
     * @param config
     * @param command
     * 
     * @throws VaultNameNotPresentException
     * @throws RegionNotSupportedException
     */
    public UploadArchiveCommand(Config config, CommandUploadArchive command) throws VaultNameNotPresentException,
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

	this.metadata = Metadata.from(command.metadata);
	this.upload = new AmazonGlacierUploadUtil(credentials, client, region);

	try {
	    this.journal = State.load(command.journal);
	} catch (IOException e) {
	    System.out.println(String.format("Creating a new journal: %s", command.journal));
	    this.journal = new State();
	    this.journal.setMetadata(metadata);
	    this.journal.setName(command.vaultName);
	    this.journal.setDate(new Date());
	    this.journal.setFile(command.journal);
	}

	command.partSize = command.partSize * (int) AmazonGlacierBaseUtil.MINIMUM_PART_SIZE;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {

	System.out.println("START: upload-archive\n");

	if (command.files.isEmpty()) {
	    System.out.println("ERROR: No files specified");
	} else {

	    Archive archive = null;
	    Boolean upload = true;
	    Boolean exists = false;

	    for (String fileName : command.files) {

		upload = true;
		exists = journal.isFileInArchive(fileName);

		// check if this file exists in the journal
		if (exists) {

		    System.out.println(String.format("%s is already present in the journal", fileName));
		    System.out.println(String.format("Verifying ..."));

		    Archive testArchive = journal.getByName(fileName);

		    GenericValidateEnum validSize = State.archiveValidateFileSize(testArchive);
		    GenericValidateEnum validModifiedDate = State.archiveValidateLastModified(testArchive);

		    System.out.println(String.format("%s size is %s", fileName, validSize));
		    System.out.println(String.format("%s modified date is %s", fileName, validModifiedDate));
		    System.out.println(String.format("Verifying hash ..."));

		    GenericValidateEnum validTreeHash = State.archiveValidateTreeHash(testArchive);

		    System.out.println(String.format("Hash is: %s", validTreeHash));
		    System.out.println();

		    upload = command.forceUpload;

		}

		if (upload) {

		    System.out.println(String.format("Processing: %s (size: %s)", fileName, new File(Config.getInstance().getDirectory(),
			    fileName).length()));

		    try {
			archive = this.upload.UploadMultipartFile(fileName, new File(Config.getInstance().getDirectory(), fileName),
				command.concurrent, command.retryFailedUpload, command.partSize, command.vaultName, metadata);

			if (command.forceUpload && exists) {
			    archive.setState(ArchiveState.FORCE_UPLOAD);
			}

			this.journal.addArchive(archive);
			this.journal.save();

		    } catch (UploadTooManyPartsException e) {
			e.printStackTrace();
		    } catch (IOException e) {
			e.printStackTrace();
		    } catch (RegionNotSupportedException e) {
			e.printStackTrace();
		    }

		} else {
		    System.out.println(String.format("Skipping upload for %s", fileName));
		}

	    }

	}

	System.out.println("\nEND: upload-archive");
    }
}
