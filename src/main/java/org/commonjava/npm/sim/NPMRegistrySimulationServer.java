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
import org.commonjava.test.http.expect.ExpectationServlet;
import org.commonjava.test.http.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Arrays;

/**
 * Created by gli on 2/17/17.
 */
public class NPMRegistrySimulationServer
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Undertow server;

    private final NPMRegistrySimulationServlet servlet;

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
        final NPMRegistrySimulationServer server = new NPMRegistrySimulationServer( port );

        server.start();
        System.out.println( "http server hosted at port:" + server.getPort() );

        final String jqueryMetaReqPath = server.formatUrl( "jquery" );
        final String jqueryJsonMeta = ResourceReader.getJson( "jquery.json" )
                                                    .replaceAll( "https://registry.npmjs.org",
                                                                 "http://localhost:" + server.getPort() );
        server.addServiceHandler( CommonMethod.GET, jqueryMetaReqPath, new ServiceHandler()
        {
            @Override
            public void handle( HttpServletRequest req, HttpServletResponse resp )
                    throws ServletException, IOException
            {
                resp.setStatus( 200 );

                server.logger.info( "Set status: {} with body string", 200 );
                resp.getWriter()
                    .write( jqueryJsonMeta );
            }
        } );

        final String jqueryPkgReqPath = server.formatUrl( "jquery", "-", "jquery-1.12.4.tgz" );
        server.addServiceHandler( CommonMethod.GET, jqueryPkgReqPath, new ServiceHandler()
        {
            @Override
            public void handle( HttpServletRequest req, HttpServletResponse resp )
                    throws ServletException, IOException
            {
                resp.setStatus( 200 );

                server.logger.info( "Set status: {} with body InputStream", 200 );
                InputStream body = ResourceReader.getPkg( "jquery-1.12.4.tgz" );
                IOUtils.copy( body, resp.getOutputStream() );
                body.close();
            }
        } );

    }

}
