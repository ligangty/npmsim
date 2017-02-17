/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.npm.sim;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.apache.commons.io.IOUtils;
import org.commonjava.test.http.common.CommonMethod;
import org.commonjava.test.http.expect.ContentResponse;
import org.commonjava.test.http.expect.ExpectationServer;
import org.commonjava.test.http.expect.ExpectationServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by gli on 2/17/17.
 */
public class NPMRegistrySimulationServer
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Undertow server;

    private final HttpServlet servlet;

    private int port;

    public NPMRegistrySimulationServer( final int port )
    {
        this.port = port;
        this.servlet = new NPMRegistrySimulationServlet( null );
    }

    public NPMRegistrySimulationServer( final int port, final String baseResource )
    {
        this.port = port;
        this.servlet = new NPMRegistrySimulationServlet( baseResource );
    }

    public NPMRegistrySimulationServer start()
            throws IOException
    {
        final ServletInfo si = Servlets.servlet( "TEST", ExpectationServlet.class )
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

    public int getPort()
    {
        return port;
    }

    public static void main( String[] args )
            throws Exception
    {
        int port = 10240;
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
        NPMRegistrySimulationServer server = new NPMRegistrySimulationServer( port );

        server.start();
        System.out.println( "http server hosted at port:" + server.getPort() );

        final String jqueryMetaReqPath = server.formatUrl( "jquery" );
        final String jqueryJsonMeta = ResourceReader.getJson( "jquery.json" )
                                                    .replaceAll( "https://registry.npmjs.org",
                                                                 "http://localhost:" + server.getPort() );
        server.expect( jqueryMetaReqPath, 200, jqueryJsonMeta );

        final String jqueryPkgReqPath = server.formatUrl( "jquery", "-", "jquery-1.12.4.tgz" );
        final InputStream jqueryPkgStream = ResourceReader.getPkg( "jquery-1.12.4.tgz" );
        server.expect( jqueryPkgReqPath, 200, jqueryPkgStream );

    }

    public static class NPMRegistrySimulationServlet
            extends HttpServlet
    {
        private final Logger logger = LoggerFactory.getLogger( getClass() );

        private static final long serialVersionUID = 1L;

        private final String baseResource;

        private final Map<String, Integer> accessesByPath = new HashMap<>();

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

        public Map<String, Integer> getAccessesByPath()
        {
            return accessesByPath;
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

            String path = wholePath;
            if ( path.length() > 1 )
            {
                path = path.substring( 1 );
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
            final ContentResponse expectation = expectations.get( key );
            if ( expectation != null )
            {
                logger.info( "Responding via registered expectation: {}", expectation );

                if ( expectation.handler() != null )
                {
                    expectation.handler().handle( req, resp );
                    logger.info( "Using handler..." );
                    return;
                }
                else if ( expectation.body() != null )
                {
                    resp.setStatus( expectation.code() );

                    logger.info( "Set status: {} with body string", expectation.code() );
                    resp.getWriter().write( expectation.body() );
                }
                else if ( expectation.bodyStream() != null )
                {
                    resp.setStatus( expectation.code() );

                    logger.info( "Set status: {} with body InputStream", expectation.code() );
                    IOUtils.copy( expectation.bodyStream(), resp.getOutputStream() );
                }
                else
                {
                    resp.setStatus( expectation.code() );
                    logger.info( "Set status: {} with no body", expectation.code() );
                }

                return;
            }

            resp.setStatus( 404 );
        }

        public String getAccessKey( final CommonMethod method, final String path )
        {
            return getAccessKey( method.name(), path );
        }

        public Integer getAccessesFor( final String path )
        {
            return accessesByPath.get( getAccessKey( CommonMethod.GET, path ) );
        }

        public Integer getAccessesFor( final String method, final String path )
        {
            return accessesByPath.get( getAccessKey( method, path ) );
        }
    }
}
