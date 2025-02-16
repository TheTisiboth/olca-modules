package org.openlca.ilcd.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response.Status.Family;

import org.openlca.ilcd.commons.IDataSet;
import org.openlca.ilcd.commons.Ref;
import org.openlca.ilcd.descriptors.CategorySystemList;
import org.openlca.ilcd.descriptors.DataStockList;
import org.openlca.ilcd.descriptors.Descriptor;
import org.openlca.ilcd.descriptors.DescriptorList;
import org.openlca.ilcd.lists.CategorySystem;
import org.openlca.ilcd.methods.LCIAMethod;
import org.openlca.ilcd.sources.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

/**
 * A client interface of a Soda4LCA service end-point.
 */
public class SodaClient implements DataStore {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final SodaConnection con;
	private final List<Cookie> cookies = new ArrayList<>();
	private final XmlBinder binder = new XmlBinder();

	private Client client;
	private boolean isConnected = false;

	public SodaClient(SodaConnection con) {
		this.con = con;
	}

	public void connect() {
		log.info("Create ILCD network connection {}", con);
		client = Client.create();
		authenticate();
		isConnected = true;
	}

	private void authenticate() {
		if (con.user == null || con.user.trim().isEmpty()
				|| con.password == null || con.password.trim().isEmpty()) {
			log.info("no user or password -> anonymous access");
			return;
		}
		log.info("Authenticate user: {}", con.user);
		ClientResponse response = client.resource(con.url).path("authenticate")
				.path("login").queryParam("userName", con.user)
				.queryParam("password", con.password).get(ClientResponse.class);
		eval(response);
		log.trace("Server response: {}", response.getEntity(String.class));
		for (NewCookie c : response.getCookies()) {
			cookies.add(c.toCookie());
		}
	}

	public AuthInfo getAuthentication() {
		checkConnection();
		log.trace("Get authentication information.");
		WebResource r = resource("authenticate", "status");
		ClientResponse response = cookies(r).get(ClientResponse.class);
		eval(response);
		return response.getEntity(AuthInfo.class);
	}

	public DataStockList getDataStockList() {
		checkConnection();
		log.trace("get data stock list: /datastocks");
		WebResource r = resource("datastocks");
		return cookies(r).get(DataStockList.class);
	}

	public CategorySystemList getCategorySystemList() {
		checkConnection();
		log.trace("get category system list: /categorySystems");
		WebResource r = resource("categorySystems");
		return cookies(r).get(CategorySystemList.class);
	}

	public CategorySystem getCategorySystem(String name) {
		checkConnection();
		log.trace("get category system list: /categorySystems/{}", name);
		WebResource r = resource("categorySystems", name);
		return cookies(r).get(CategorySystem.class);
	}

	@Override
	public <T extends IDataSet> T get(Class<T> type, String id) {
		checkConnection();
		WebResource r = resource(Dir.get(type), id).queryParam("format", "xml");
		log.info("Get resource: {}", r.getURI());
		ClientResponse response = cookies(r).get(ClientResponse.class);
		eval(response);
		try {
			return binder.fromStream(type, response.getEntityInputStream());
		} catch (Exception e) {
			throw new RuntimeException("Failed to load resource " + id
					+ " of type " + type, e);
		}
	}

	@Override
	public void put(IDataSet ds) {
		checkConnection();
		WebResource r = resource(Dir.get(ds.getClass()));
		log.info("Publish resource: {}/{}", r.getURI(), ds.getUUID());
		try {
			byte[] bytes = binder.toByteArray(ds);
			Builder builder = cookies(r).type(MediaType.APPLICATION_XML);
			if (con.dataStockId != null) {
				log.trace("post to data stock {}", con.dataStockId);
				builder = builder.header("stock", con.dataStockId);
			}
			ClientResponse response = builder.post(ClientResponse.class, bytes);
			eval(response);
			log.trace("Server response: {}", fetchMessage(response));
		} catch (Exception e) {
			throw new RuntimeException("Failed to upload data set + " + ds, e);
		}
	}

	@Override
	public void put(Source source, File[] files) {
		checkConnection();
		log.info("Publish source with files {}", source);
		try {
			FormDataMultiPart multiPart = new FormDataMultiPart();
			if (con.dataStockId != null) {
				log.trace("post to data stock {}", con.dataStockId);
				multiPart.field("stock", con.dataStockId);
			}
			byte[] bytes = binder.toByteArray(source);
			ByteArrayInputStream xmlStream = new ByteArrayInputStream(bytes);
			FormDataBodyPart xmlPart = new FormDataBodyPart("file", xmlStream,
					MediaType.MULTIPART_FORM_DATA_TYPE);
			multiPart.bodyPart(xmlPart);
			addFiles(files, multiPart);
			WebResource r = resource("sources/withBinaries");
			ClientResponse resp = cookies(r).type(
					MediaType.MULTIPART_FORM_DATA_TYPE)
					.post(ClientResponse.class, multiPart);
			eval(resp);
			log.trace("Server response: {}", fetchMessage(resp));
		} catch (Exception e) {
			throw new RuntimeException("Failed to upload source with file", e);
		}
	}

	private void addFiles(File[] files, FormDataMultiPart multiPart)
			throws Exception {
		if (files == null)
			return;
		for (File file : files) {
			if (file == null)
				continue;
			FileInputStream is = new FileInputStream(file);
			FormDataBodyPart part = new FormDataBodyPart(file.getName(),
					is, MediaType.MULTIPART_FORM_DATA_TYPE);
			multiPart.bodyPart(part);
		}
	}

	public InputStream getExternalDocument(String sourceId, String fileName) {
		checkConnection();
		WebResource r = resource("sources", sourceId, fileName);
		log.info("Get external document {} for source {}", fileName, sourceId);
		try {
			return cookies(r).type(MediaType.APPLICATION_OCTET_STREAM).get(
					InputStream.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get file " + fileName +
					"for source " + sourceId, e);
		}
	}

	@Override
	public <T extends IDataSet> boolean delete(Class<T> type, String id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T extends IDataSet> Iterator<T> iterator(Class<T> type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends IDataSet> boolean contains(Class<T> type, String id) {
		checkConnection();
		WebResource r = resource(Dir.get(type), id)
				.queryParam("format", "xml");
		log.trace("Contains resource {} ?", r.getURI());
		ClientResponse response = cookies(r).head();
		log.trace("Server response: {}", response);
		return response.getStatus() == Status.OK.getStatusCode();
	}

	/** Includes also the version in the check. */
	public boolean contains(Ref ref) {
		if (ref == null || ref.type == null || ref.uuid == null)
			return false;
		checkConnection();
		WebResource r = resource(Dir.get(ref.getDataSetClass()), ref.uuid)
				.queryParam("format", "xml");
		if (ref.version != null)
			r = r.queryParam("version", ref.version);
		ClientResponse response = cookies(r).head();
		return response.getStatus() == Status.OK.getStatusCode();
	}

	public DescriptorList search(Class<?> type, String name) {
		try {
			checkConnection();
			String term = name == null ? "" : name.trim();
			WebResource r = con.dataStockId == null
					? resource(Dir.get(type))
					: resource("datastocks", con.dataStockId, Dir.get(type));
			r = r.queryParam("search", "true")
					.queryParam("name", term);
			log.trace("Search resources: {}", r.getURI());
			return cookies(r).get(DescriptorList.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public List<Descriptor> getDescriptors(Class<?> type) {
		log.debug("get descriptors for {}", type);
		try {
			checkConnection();
			WebResource r = con.dataStockId == null
					? resource(Dir.get(type))
					: resource("datastocks", con.dataStockId, Dir.get(type));
			r = r.queryParam("pageSize", "1000");
			List<Descriptor> list = new ArrayList<>();
			int total;
			int idx = 0;
			do {
				log.debug("get descriptors for {} @startIndex={}", type, idx);
				r = r.queryParam("startIndex", Integer.toString(idx));
				DescriptorList data = cookies(r).get(DescriptorList.class);
				total = data.totalSize;
				int fetched = data.descriptors.size();
				if (fetched == 0)
					break;
				list.addAll(data.descriptors);
				idx += fetched;
			} while (list.size() < total);
			return list;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private WebResource resource(String... path) {
		WebResource r = client.resource(con.url);
		for (String p : path) {
			r = r.path(p);
		}
		return r;
	}

	private Builder cookies(WebResource r) {
		Builder b = r.getRequestBuilder();
		for (Cookie c : cookies)
			b.cookie(c);
		return b;
	}

	private void checkConnection() {
		if (!isConnected) {
			connect();
		}
	}

	private void eval(ClientResponse resp) {
		if (resp == null)
			throw new IllegalArgumentException("Client response is NULL.");
		Status status = Status.fromStatusCode(resp.getStatus());
		Family family = status.getFamily();
		if (family == Family.CLIENT_ERROR || family == Family.SERVER_ERROR) {
			String message = status.getReasonPhrase() + ": "
					+ fetchMessage(resp);
			throw new RuntimeException(message);
		}
	}

	private String fetchMessage(ClientResponse response) {
		if (response.hasEntity())
			return response.getEntity(String.class);
		return "";
	}

	@Override
	public void close() {
		client.destroy();
	}
}
