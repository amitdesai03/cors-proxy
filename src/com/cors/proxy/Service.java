package com.cors.proxy;

import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandler;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.GZIPContentEncodingFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;

@Path("/forward")
public class Service {

	@POST
	@Path("{url : .+}")
	public Response post(@Context HttpHeaders headers,
			@Context UriInfo uriInfo, String requestBody) {
		String url = processURL(uriInfo);

		ClientResponse response = getBuilder(url, headers).method(
				HttpMethod.POST, ClientResponse.class, requestBody);

		return clientResponseToResponse(response);
	}

	@GET
	@Path("{url : .+}")
	public Response get(@Context HttpHeaders headers, @Context UriInfo uriInfo) {
		String url = processURL(uriInfo);

		ClientResponse response = getBuilder(url, headers).method(
				HttpMethod.GET, ClientResponse.class);

		return clientResponseToResponse(response);
	}

	@PUT
	@Path("{url : .+}")
	public Response put(@Context HttpHeaders headers, String requestBody,
			@Context UriInfo uriInfo) {
		String url = processURL(uriInfo);

		ClientResponse response = getBuilder(url, headers).method(
				HttpMethod.PUT, ClientResponse.class, requestBody);

		return clientResponseToResponse(response);
	}

	@DELETE
	@Path("{url : .+}")
	public Response delete(@Context HttpHeaders headers,
			@Context UriInfo uriInfo) {
		String url = processURL(uriInfo);

		ClientResponse response = getBuilder(url, headers).method(
				HttpMethod.DELETE, ClientResponse.class);

		return clientResponseToResponse(response);
	}

	private static Response clientResponseToResponse(ClientResponse r) {
		// copy the status code
		ResponseBuilder rb = Response.status(r.getStatus());
		// copy all the headers
		for (Entry<String, List<String>> entry : r.getHeaders().entrySet()) {
			for (String value : entry.getValue()) {
				rb.header(entry.getKey(), value);
			}
		}
		// copy the entity
		rb.entity(r.getEntity(new GenericType<String>() {
		}));
		// return the response
		return rb.build();
	}

	private WebResource.Builder getBuilder(String url, HttpHeaders headers) {
		LoggingFilter logging = new LoggingFilter(Logger.getAnonymousLogger());
		ClientConfig config = new DefaultClientConfig();
        config.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, true);
		Client client = Client.create(config);
		client.addFilter(new GZIPContentEncodingFilter(false));
		client.setFollowRedirects(true);
		WebResource webResource = client.resource(url);
		webResource.addFilter(logging);
		webResource.addFilter(new RedirectFilter());
		
		WebResource.Builder builder = webResource.getRequestBuilder();

		for (String key : headers.getRequestHeaders().keySet()) {
			List<String> values = headers.getRequestHeaders().get(key);
			String finalValue = "";
			for (String value : values) {
				finalValue += value + ",";
			}
			finalValue = finalValue.substring(0, finalValue.length() - 1);
			builder = builder.header(key, finalValue);
		}
		return builder;
	}

	private static String processURL(UriInfo uriInfo) {
		String path = uriInfo.getRequestUri().getPath();
		path = path.substring(path.indexOf("forward") + 8);
		path = path.replace("//", "://");

		String query = uriInfo.getRequestUri().getRawQuery();
		if (query != null && !query.trim().equals("")) {
			path = path + "?" + query;
		}
		return path;
	}
	
	class RedirectFilter extends ClientFilter {

	    @Override
	    public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
	        ClientHandler ch = getNext();
	        ClientResponse resp = ch.handle(cr);

	        if (resp.getStatusInfo().getFamily() != Response.Status.Family.REDIRECTION) {
	            return resp;
	        }
	        else {
	            // try location
	            String redirectTarget = resp.getHeaders().getFirst("Location");
	            cr.setURI(UriBuilder.fromUri(redirectTarget).build());
	            return ch.handle(cr);
	        }

	    }

	}
}