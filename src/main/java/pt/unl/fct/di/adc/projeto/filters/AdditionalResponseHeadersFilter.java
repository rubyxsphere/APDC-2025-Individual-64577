package pt.unl.fct.di.adc.projeto.filters;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AdditionalResponseHeadersFilter implements ContainerResponseFilter {

	private static final String HEADER_AC_ALLOW_METHODS = "Access-Control-Allow-Methods";
	private static final String HEADER_AC_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String HEADER_AC_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	private static final String ALLOWED_METHODS = "HEAD,GET,PUT,POST,DELETE,OPTIONS";
	private static final String ALLOWED_ORIGIN = "*";
	private static final String ALLOWED_HEADERS = "Content-Type, X-Requested-With, Authorization";

	public AdditionalResponseHeadersFilter() {}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		responseContext.getHeaders().add(HEADER_AC_ALLOW_METHODS, ALLOWED_METHODS);
		responseContext.getHeaders().add(HEADER_AC_ALLOW_ORIGIN, ALLOWED_ORIGIN);
		responseContext.getHeaders().add(HEADER_AC_ALLOW_HEADERS, ALLOWED_HEADERS);
	}
}