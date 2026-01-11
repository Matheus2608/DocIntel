package dev.matheus.middleware;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.container.PreMatching;

import java.util.List;

@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    // Allow multiple local dev origins (add more if you use other ports/hosts)
    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:5174",
            "http://localhost:3000"
    );

    private static final String ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS, HEAD";
    private static final String ALLOWED_HEADERS = "Origin, X-Requested-With, Content-Type, Accept, Authorization";
    private static final String EXPOSED_HEADERS = "location, content-disposition";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Handle preflight OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            String origin = requestContext.getHeaderString("Origin");
            Response.ResponseBuilder builder = Response.noContent(); // 204 for preflight

            if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
                builder.header("Access-Control-Allow-Origin", origin);
                // Tell caches that the response varies with Origin
                builder.header("Vary", "Origin");
            }

            builder.header("Access-Control-Allow-Methods", ALLOWED_METHODS);
            builder.header("Access-Control-Allow-Headers", ALLOWED_HEADERS);
            builder.header("Access-Control-Allow-Credentials", "true");
            builder.header("Access-Control-Max-Age", "3600");
            builder.header("Access-Control-Expose-Headers", EXPOSED_HEADERS);

            requestContext.abortWith(builder.build());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {

        // Add CORS headers to all responses
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        String origin = requestContext.getHeaderString("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            headers.putSingle("Access-Control-Allow-Origin", origin);
            headers.putSingle("Vary", "Origin");
        }

        headers.putSingle("Access-Control-Allow-Methods", ALLOWED_METHODS);
        headers.putSingle("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        headers.putSingle("Access-Control-Allow-Credentials", "true");
        headers.putSingle("Access-Control-Expose-Headers", EXPOSED_HEADERS);
    }
}