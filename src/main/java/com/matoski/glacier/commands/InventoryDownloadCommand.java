package com.matoski.glacier.commands;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.amazonaws.services.glacier.model.GetJobOutputRequest;
import com.amazonaws.services.glacier.model.GetJobOutputResult;
import com.amazonaws.services.glacier.model.GlacierJobDescription;
import com.amazonaws.services.glacier.model.ListJobsRequest;
import com.amazonaws.services.glacier.model.ListJobsResult;
import com.google.gson.Gson;
import com.matoski.glacier.cli.CommandInventoryDownload;
import com.matoski.glacier.enums.MetadataParsers;
import com.matoski.glacier.errors.VaultNameNotPresentException;
import com.matoski.glacier.pojo.Config;
import com.matoski.glacier.pojo.GlacierInventory;

public class InventoryDownloadCommand extends AbstractCommand {

    protected CommandInventoryDownload command;

    public InventoryDownloadCommand(Config config,
	    CommandInventoryDownload command)
	    throws VaultNameNotPresentException {
	super(config);
	this.command = command;

	if ((null == command.vaultName || command.vaultName.isEmpty())
		&& (null == config.getVault() || config.getVault().isEmpty())) {
	    throw new VaultNameNotPresentException();
	}

	if ((null == command.vaultName) || command.vaultName.isEmpty()) {
	    command.vaultName = config.getVault();
	}

    }

    public void run() {

	System.out.println("START: inventory-retrieve\n");

	String jobId = null;

	if (null == command.id || command.id.isEmpty()) {

	    // get a list of jobs and get the last successful one
	    ListJobsRequest request = new ListJobsRequest()
		    .withVaultName(command.vaultName);
	    ListJobsResult result = this.client.listJobs(request);

	    List<GlacierJobDescription> jobs = result.getJobList();
	    GlacierJobDescription job = null;

	    for (GlacierJobDescription j : jobs) {

		if (j.isCompleted()
			&& j.getStatusCode().equalsIgnoreCase("Succeeded")) {
		    job = j;
		}

	    }

	    jobId = job.getJobId();

	} else {

	    jobId = command.id;

	}

	if (null == jobId) {

	    System.out.println("ERROR: No completed InventoryJobs available");

	} else {

	    GetJobOutputRequest jobOutputRequest = new GetJobOutputRequest()
		    .withVaultName(command.vaultName).withJobId(jobId);

	    GetJobOutputResult jobOutputResult = client
		    .getJobOutput(jobOutputRequest);

	    System.out.println("Inventory downloaded.\n");

	    System.out.println(String.format("%1$10s: %2$s", "Job ID", jobId));
	    System.out.println(String.format("%1$10s: %2$s", "Vault",
		    command.vaultName));

	    System.out.println();
	    GlacierInventory inventory = null;

	    try {
		String json = IOUtils.toString(jobOutputResult.getBody());
		inventory = new Gson().fromJson(json, GlacierInventory.class);

		switch (MetadataParsers.from(command.metadata)) {
		case MT_AWS_GLACIER_B:
		    break;
		}

	    } catch (IOException e) {
		System.out.println("ERROR: Failed to read the input stream");
	    }

	}

	System.out.println("\nEND: inventory-retrieve");
    }
}
