package com.greendelta.cloud.api;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;

import com.google.gson.JsonObject;
import com.greendelta.cloud.model.data.CommitDescriptor;
import com.greendelta.cloud.model.data.DatasetDescriptor;
import com.greendelta.cloud.model.data.FetchRequestData;
import com.greendelta.cloud.util.WebRequests.WebRequestException;
import com.sun.jersey.api.client.ClientResponse.Status;

public class RepositoryClient {

	private final RepositoryConfig config;
	private String sessionId;

	public RepositoryClient(RepositoryConfig config) {
		this.config = config;
	}
	
	public RepositoryConfig getConfig() {
		return config;
	}

	public void createUser(String username, String password, String adminKey)
			throws WebRequestException {
		executeLoggedIn(() -> {
			CreateUserInvocation invocation = new CreateUserInvocation();
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			invocation.setUsername(username);
			invocation.setPassword(password);
			invocation.setAdminKey(adminKey);
			invocation.execute();
		});
	}

	public void deleteUser(String username, String adminKey)
			throws WebRequestException {
		executeLoggedIn(() -> {
			DeleteUserInvocation invocation = new DeleteUserInvocation();
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			invocation.setUsername(username);
			invocation.setAdminKey(adminKey);
			invocation.execute();
		});
	}

	private void login() throws WebRequestException {
		LoginInvocation invocation = new LoginInvocation();
		invocation.setBaseUrl(config.getBaseUrl());
		invocation.setUsername(config.getUsername());
		invocation.setPassword(config.getPassword());
		sessionId = invocation.execute();
	}

	public void clear() throws WebRequestException {
		if (sessionId == null)
			return;
		LogoutInvocation invocation = new LogoutInvocation();
		invocation.setBaseUrl(config.getBaseUrl());
		invocation.setSessionId(sessionId);
		try {
			invocation.execute();
		} catch (WebRequestException e) {
			if (e.getErrorCode() != Status.UNAUTHORIZED.getStatusCode())
				throw e;
			// else, user was not logged anymore in due to session timeout
		}
		sessionId = null;
	}

	public void createRepository(String name) throws WebRequestException {
		executeLoggedIn(() -> {
			CreateRepositoryInvocation invocation = new CreateRepositoryInvocation();
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			invocation.setName(name);
			invocation.execute();
		});
	}

	public void deleteRepository(String name) throws WebRequestException {
		executeLoggedIn(() -> {
			DeleteRepositoryInvocation invocation = new DeleteRepositoryInvocation();
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			invocation.setName(name);
			invocation.execute();
		});
	}

	public void shareRepositoryWith(String repositoryName, String shareWithUser)
			throws WebRequestException {
		executeLoggedIn(() -> {
			ShareRepositoryInvocation invocation = new ShareRepositoryInvocation();
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			invocation.setRepositoryName(repositoryName);
			invocation.setShareWithUser(shareWithUser);
			invocation.execute();
		});
	}

	public void unshareRepositoryWith(String repositoryName,
			String unshareWithUser) throws WebRequestException {
		executeLoggedIn(() -> {
			UnshareRepositoryInvocation invocation = new UnshareRepositoryInvocation();
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			invocation.setRepositoryName(repositoryName);
			invocation.setUnshareWithUser(unshareWithUser);
			invocation.execute();
		});
	}

	public List<String> getAccessListForRepository(String repositoryId)
			throws WebRequestException {
		return executeLoggedIn(() -> {
			RepositoryAccessListInvocation invocation = new RepositoryAccessListInvocation();
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			invocation.setRepositoryId(repositoryId);
			return invocation.execute();
		});
	}

	public List<String> getAccessListForUser() throws WebRequestException {
		return executeLoggedIn(() -> {
			RepositoryAccessListInvocation invocation = new RepositoryAccessListInvocation();
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			return invocation.execute();
		});
	}

	public boolean requestCommit() throws WebRequestException {
		return executeLoggedIn(() -> {
			CommitRequestInvocation invocation = new CommitRequestInvocation();
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			invocation.setRepositoryId(config.getRepositoryId());
			invocation.setLatestCommitId(config.getLatestCommitId());
			try {
				invocation.execute();
			} catch (WebRequestException e) {
				if (e.getErrorCode() == Status.CONFLICT.getStatusCode())
					return false;
				throw e;
			}
			return true;
		});
	}

	public void commit(String commitMessage, Collection<CategorizedEntity> data)
			throws WebRequestException {
		commit(commitMessage, data, Collections.emptyList());
	}

	public void commit(String commitMessage,
			Collection<CategorizedEntity> data,
			Collection<DatasetDescriptor> deleted) throws WebRequestException {
		executeLoggedIn(() -> {
			CommitInvocation invocation = new CommitInvocation();
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			invocation.setRepositoryId(config.getRepositoryId());
			invocation.setLatestCommitId(config.getLatestCommitId());
			invocation.setCommitMessage(commitMessage);
			for (CategorizedEntity entity : data)
				invocation.add(entity);
			for (DatasetDescriptor entity : deleted)
				invocation.addDelete(entity);
			config.setLatestCommitId(invocation.execute());
		});
	}

	public List<CommitDescriptor> fetchNewCommitHistory()
			throws WebRequestException {
		return executeLoggedIn(() -> {
			CommitHistoryInvocation invocation = new CommitHistoryInvocation();
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			invocation.setRepositoryId(config.getRepositoryId());
			invocation.setLatestCommitId(config.getLatestCommitId());
			return invocation.execute();
		});
	}

	public List<FetchRequestData> getReferences(String commitId)
			throws WebRequestException {
		return executeLoggedIn(() -> {
			ReferencesInvocation invocation = new ReferencesInvocation();
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			invocation.setRepositoryId(config.getRepositoryId());
			invocation.setCommitId(commitId);
			return invocation.execute();
		});
	}

	public List<FetchRequestData> requestFetch() throws WebRequestException {
		return executeLoggedIn(() -> {
			FetchRequestInvocation invocation = new FetchRequestInvocation();
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			invocation.setRepositoryId(config.getRepositoryId());
			invocation.setLatestCommitId(config.getLatestCommitId());
			return invocation.execute();
		});
	}

	public void fetch() throws WebRequestException {
		// TODO other store implementation?
		executeLoggedIn(() -> {
			FetchInvocation invocation = new FetchInvocation(
					config.getDatabase());
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			invocation.setRepositoryId(config.getRepositoryId());
			invocation.setLatestCommitId(config.getLatestCommitId());
			config.setLatestCommitId(invocation.execute());
		});
	}

	public JsonObject getDataset(ModelType type, String refId)
			throws WebRequestException {
		return getDataset(type, refId, null);
	}

	public JsonObject getDataset(ModelType type, String refId, String commitId)
			throws WebRequestException {
		return executeLoggedIn(() -> {
			DatasetContentInvocation invocation = new DatasetContentInvocation();
			invocation.setBaseUrl(config.getBaseUrl());
			invocation.setSessionId(sessionId);
			invocation.setRepositoryId(config.getRepositoryId());
			invocation.setType(type);
			invocation.setRefId(refId);
			invocation.setCommitId(commitId);
			return invocation.execute();
		});
	}

	private void executeLoggedIn(Invocation runnable)
			throws WebRequestException {
		if (sessionId == null)
			login();
		try {
			runnable.run();
		} catch (WebRequestException e) {
			if (e.getErrorCode() == Status.UNAUTHORIZED.getStatusCode()) {
				login();
				runnable.run();
			} else
				throw e;
		}
	}

	private <T> T executeLoggedIn(InvocationWithResult<T> runnable)
			throws WebRequestException {
		if (sessionId == null)
			login();
		try {
			return runnable.run();
		} catch (WebRequestException e) {
			if (e.getErrorCode() == Status.UNAUTHORIZED.getStatusCode()) {
				login();
				return runnable.run();
			} else
				throw e;
		}
	}

	private interface Invocation {
		public void run() throws WebRequestException;
	}

	private interface InvocationWithResult<T> {
		public T run() throws WebRequestException;
	}

}
