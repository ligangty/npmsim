package org.commonjava.npm.sim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class NPMRegistrySimulationServlet
        extends HttpServlet
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final long serialVersionUID = 1L;

    private final String baseResource;

    private final Map<String, ServiceHandler> handlers = new HashMap<>();

    private final Map<String, Integer> accessesByPath = new HashMap<>();

    NPMRegistrySimulationServlet(){
        this("/");
    }

    public NPMRegistrySimulationServlet( final String baseResource )
    {
        String br = baseResource;
        if ( br == null )
        {
            br = "/";
        }
        else if ( !br.startsWith( "/" ) )
        {
            br = "/" + br;
        }
        this.baseResource = br;
    }

    public String getBaseResource()
    {
        return baseResource;
    }

    public String getAccessKey( final String method, final String path )
    {
        return method.toUpperCase() + " " + path;
    }

    private String getPath( final String path )
    {
        String realPath = path;
        try
        {
            final URL u = new URL( path );
            realPath = u.getPath();
        }
        catch ( final MalformedURLException e )
        {
        }

        return realPath;
    }

    @Override
    protected void service( final HttpServletRequest req, final HttpServletResponse resp )
            throws ServletException, IOException
    {
        String wholePath;
        try
        {
            wholePath = new URI( req.getRequestURI() ).getPath();
        }
        catch ( final URISyntaxException e )
        {
            throw new ServletException( "Cannot parse request URI", e );
        }

        final String key = getAccessKey( req.getMethod(), wholePath );

        logger.info( "Looking up expectation for: {}", key );

        final Integer i = accessesByPath.get( key );
        if ( i == null )
        {
            accessesByPath.put( key, 1 );
        }
        else
        {
            accessesByPath.put( key, i + 1 );
        }

        logger.info( "Looking for expectation: '{}'", key );
        final ServiceHandler handler = handlers.get( key );
        if ( handler != null )
        {
            logger.info( "Responding via registered expectation: {}", handler );

            handler.handle( req, resp );
            logger.info( "Using handler..." );
            return;
        }

        resp.setStatus( 404 );
    }

    public String getAccessKey( final CommonMethod method, final String path )
    {
        return getAccessKey( method.name(), path );
    }

    public void addHandler( final CommonMethod method, final String path, final ServiceHandler handler )
    {
        handlers.put( getAccessKey( method, getPath( path ) ), handler );
    }

}
