package org.commonjava.npm.sim;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Enumeration;

public class NPMRegistrySimulationServer
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final NPMRegistrySimulationServlet servlet;

    private int port;

    public NPMRegistrySimulationServer( final int port )
    {
        this.port = port;
        this.servlet = new NPMRegistrySimulationServlet( null );
    }

    public NPMRegistrySimulationServer start()
            throws IOException
    {
        final ServletInfo si = Servlets.servlet( "TEST", NPMRegistrySimulationServlet.class )
                                       .addMapping( "*" )
                                       .addMapping( "/*" )
                                       .setLoadOnStartup( 1 );

        si.setInstanceFactory( new ImmediateInstanceFactory<Servlet>( servlet ) );

        final DeploymentInfo di = new DeploymentInfo().addServlet( si )
                                                      .setDeploymentName( "TEST" )
                                                      .setContextPath( "/" )
                                                      .setClassLoader( Thread.currentThread().getContextClassLoader() );

        final DeploymentManager dm = Servlets.defaultContainer().addDeployment( di );
        dm.deploy();

        final Undertow server;
        try
        {
            server = Undertow.builder().setHandler( dm.start() ).addHttpListener( port, "127.0.0.1" ).build();
        }
        catch ( ServletException e )
        {
            throw new IOException( "Failed to start: " + e.getMessage(), e );
        }

        server.start();
        logger.info( "STARTED Test HTTP Server on 127.0.0.1:" + port );

        return this;
    }

    public String formatUrl( final String... subpath )
    {
        try
        {
            return UrlUtils.buildUrl( "http://127.0.0.1:" + port, servlet.getBaseResource(), subpath );
        }
        catch ( final MalformedURLException e )
        {
            throw new IllegalArgumentException( "Failed to build url to: " + Arrays.toString( subpath ), e );
        }
    }

    public int getPort()
    {
        return port;
    }

    public void addServiceHandler( final CommonMethod method, final String path, final ServiceHandler handler )
    {
        this.servlet.addHandler( method, path, handler );
    }

    public static void main( String[] args )
            throws Exception
    {
        int port = 8000;
        for ( int i = 0; i < args.length; i++ )
        {
            String param = args[i];
            if ( "--port".equals( param.trim() ) || "-p".equals( param.trim() ) )
            {
                try
                {
                    port = Integer.parseInt( args[i + 1] );
                }
                catch ( NumberFormatException e )
                {
                    port = 0;
                }
                break;
            }
        }
        if ( port <= 0 )
        {
            System.out.println( "Error port setting for port:" + port + "!" );
            System.exit( 0 );
        }
        final NPMRegistrySimulationServer server = new NPMRegistrySimulationServer( port );

        server.start();
        System.out.println( "http server hosted at port:" + server.getPort() );

        // serve for GET:/jquery
        final String jqueryMetaReqPath = server.formatUrl( "jquery" );
        final String jqueryJsonMeta = ResourceReader.getJson( "jquery.json" )
                                                    .replaceAll( "https://registry.npmjs.org",
                                                                 "http://localhost:" + server.getPort() );
        server.addServiceHandler( CommonMethod.GET, jqueryMetaReqPath, ( req, resp ) -> {
            printHeaders( req );
            resp.setStatus( 200 );
            server.logger.info( "Set status: {} with body string", 200 );
            resp.getWriter().write( jqueryJsonMeta );
        } );

        // serve for GET:/jquery/-/jquery-1.12.4.tgz
        final String jqueryPkgReqPath = server.formatUrl( "jquery", "-", "jquery-1.12.4.tgz" );
        server.addServiceHandler( CommonMethod.GET, jqueryPkgReqPath, ( req, resp ) -> {
            printHeaders( req );
            resp.setStatus( 200 );
            server.logger.info( "Set status: {} with body InputStream", 200 );
            InputStream body = ResourceReader.getPkg( "jquery-1.12.4.tgz" );
            if ( body != null )
            {
                IOUtils.copy( body, resp.getOutputStream() );
                body.close();
            }
        } );

        // serve for PUT:/npmsniff
        final String npmSnifMetaReqPath = server.formatUrl( "npmsniff" );
        server.addServiceHandler( CommonMethod.PUT, npmSnifMetaReqPath, ( req, resp ) -> {
            printHeaders( req );
            System.out.println( "request body: \n" + IOUtils.toString( req.getInputStream() ) );
            resp.setStatus( 200 );
            server.logger.info( "Set status: {} with body string", 200 );
            resp.getWriter().write( "{\"success\": true}" );
        } );

        final String fakeToken = "8d0afba2-0819-56fa-9834-2311c895faaf";
        // serve for PUT:/-/user/org.couchdb.user:npm
        final String loginReqPath = server.formatUrl( "-", "user", "org.couchdb.user:npm" );
        server.addServiceHandler( CommonMethod.PUT, loginReqPath, ( req, resp ) -> {
            printHeaders( req );
            System.out.println( "request body: \n" + IOUtils.toString( req.getInputStream() ) );
            resp.setStatus( 201 );
            server.logger.info( "Set status: {} with body string", 201 );
            resp.getWriter()
                .write( "{\"id\":\"org.couchdb.user:undefined\","
                                + "\"ok\": true,"
                                + "\"rev\":\"_we_dont_use_revs_any_more\","
                                + "\"token\": \""+fakeToken+"\"}" );
        } );

        // serve for DELETE:/-/user/token/$token
        final String logoutReqPath = server.formatUrl( "-", "user", "token", fakeToken );
        server.addServiceHandler( CommonMethod.DELETE, logoutReqPath, ( req, resp ) -> {
            printHeaders( req );
            System.out.println( "request body: \n" + IOUtils.toString( req.getInputStream() ) );
            resp.setStatus( 200 );
            server.logger.info( "Set status: {} with body string", 200 );
            resp.getWriter()
                .write( "{\"ok\":true}" );
        } );

    }

    private static void printHeaders( HttpServletRequest req )
    {
        Enumeration<String> headers = req.getHeaderNames();
        if ( headers.hasMoreElements() )
        {
            System.out.println( "The header for request as following:" );
        }
        while ( headers.hasMoreElements() )
        {
            String header = headers.nextElement();
            Enumeration<String> headerVals = req.getHeaders( header );
            while ( headerVals.hasMoreElements() )
            {
                System.out.println( header + ":    " + headerVals.nextElement() );
            }
        }
    }

}
