package org.codehaus.plexus;

/* ----------------------------------------------------------------------------
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.codehaus.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Plexus", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact codehaus@codehaus.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ----------------------------------------------------------------------------
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.codehaus.org/>.
 *
 * ----------------------------------------------------------------------------
 */

import com.werken.classworlds.ClassWorld;
import com.werken.classworlds.NoSuchRealmException;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.service.ServiceException;
import org.codehaus.plexus.classloader.DefaultResourceManager;
import org.codehaus.plexus.classloader.ResourceManagerFactory;
import org.codehaus.plexus.configuration.ConfigurationResourceException;
import org.codehaus.plexus.configuration.DefaultConfiguration;
import org.codehaus.plexus.configuration.XmlPullConfigurationBuilder;
import org.codehaus.plexus.context.DefaultContext;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.logging.LoggerManagerFactory;
import org.codehaus.plexus.service.repository.ComponentRepository;
import org.codehaus.plexus.service.repository.ComponentRepositoryFactory;
import org.codehaus.plexus.util.ContextMapAdapter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.InterpolationFilterReader;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/** The main Plexus container component.
 *
 *  @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 *  @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 *
 *  @version $Id$
 *
 *  @todo Make ClassWorlds optional so we can make the runtime tiny.
 *  @todo the container itself must be able to behave like a normal
 *        component so that we can deal with hierachies. In the majority of
 *        cases the derived container will take a lot of configuration information
 *        from the parent.
 */
public class DefaultPlexusContainer
    extends AbstractLogEnabled
    implements PlexusContainer
{
    // ----------------------------------------------------------------------
    //  Instance Members
    // ----------------------------------------------------------------------

    /** Logger Manager used for this container. */
    private LoggerManager loggerManager;

    /** Context used for this container. */
    private DefaultContext context;

    /** Service Repository used for this container. */
    private ComponentRepository componentRepository;

    /** Configuration for this container. */
    private Configuration configuration;

    /** The configuration resource. */
    private Reader configurationReader;

    /**
     *  Typically Plexus will use a ClassWorld for all its class loading and
     *  resource requirements, but it remains to be seen if this will be possible
     *  in environments like j2me. We need to be able to initialize the Plexus
     *  resource manager with either a class world or a standard class loader.
     */
    private ClassWorld classWorld;

    /** Class loader used for this container if a class world is not available. */
    private ClassLoader classLoader;

    /**
     *  Resource manager for this container. It is available via the context using
     *  plexus:resource-manager key.
     */
    private DefaultResourceManager resourceManager;

    /** Default Configuration Builder. */
    private XmlPullConfigurationBuilder builder;

    /** Default configuration. */
    private Configuration defaultConfiguration;

    // ----------------------------------------------------------------------
    //  Constructors
    // ----------------------------------------------------------------------

    /**
     *  Constuct.
     */
    public DefaultPlexusContainer()
    {
        builder = new XmlPullConfigurationBuilder();
    }

    // ----------------------------------------------------------------------
    // Lifecylce Management
    // ----------------------------------------------------------------------

    /**
     * - Initialize ClassLoader
     * - Initialize the default configuration
     * - Initialize the configuration
     * - Initialize logger manager
     * - Initialize service repository
     * - Initialize resource manager
     * - Initialize the context. Values put into the context at this point won't
     *   be interpolated into the configuration.  This may need to change later.
     * - Initialize lifecycle handler
     *
     * @throws Exception
     */
    public void initialize()
        throws Exception
    {
        initializeClassLoader();
        initializeDefaultConfiguration();
        initializeConfiguration();
        initializeLoggerManager();
        initializeComponentRepository();
        initializeResourceManager();
        initializeContext();
        initializeSystemProperties();
    }

    public void start()
        throws Exception
    {
        loadOnStart();
    }

    public void dispose()
    {
        componentRepository.dispose();
    }

    // ----------------------------------------------------------------------
    // Pre-initialization - can only be called prior to initialization
    // ----------------------------------------------------------------------

    public void addContextValue( Object key, Object value )
    {
        getContext().put( key, value );
    }

    public void setClassLoader( ClassLoader classLoader )
    {
        this.classLoader = classLoader;
    }

    public void setClassWorld( ClassWorld classWorld )
    {
        this.classWorld = classWorld;
    }

    /** @see PlexusContainer#setConfigurationResource(Reader) */
    public void setConfigurationResource( Reader configuration )
        throws ConfigurationResourceException
    {
        this.configurationReader = configuration;
    }

    // ----------------------------------------------------------------------
    // Post-initialization - can only be called post initialization
    // ----------------------------------------------------------------------

    public ClassLoader getClassLoader()
    {
        if ( classLoader == null )
        {
            throw new IllegalStateException( "This container must be assigned a ClassLoader." );
        }

        return classLoader;
    }

    public ComponentRepository getComponentRepository()
    {
        return componentRepository;
    }

    // ----------------------------------------------------------------------
    // Implementation
    // ----------------------------------------------------------------------

    /**
     *  Load specifies roles during server startup.
     */
    protected void loadOnStart()
        throws Exception
    {
        Configuration[] loadOnStartServices = configuration.getChild( "load-on-start" ).getChildren( "service" );

        for ( int i = 0; i < loadOnStartServices.length; i++ )
        {
            String role = loadOnStartServices[i].getAttribute( "role" );
            String id = loadOnStartServices[i].getAttribute( "id", "" );

            getLogger().info( "Loading on start [role,id]: " + "[" + role + "," + id + "]" );

            try
            {
                if ( id.length() == 0 )
                {
                    getComponentRepository().lookup( role );
                }
                else
                {
                    getComponentRepository().lookup( role, id );
                }
            }
            catch ( ServiceException e )
            {
                getLogger().error( "Cannot load-on-start " + role, e );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Initialization Implementation
    // ----------------------------------------------------------------------

    private void initializeClassLoader()
        throws Exception
    {
        if ( getClassWorld() != null )
        {
            try
            {
                classLoader = getClassWorld().getRealm( "core" ).getClassLoader();
            }
            catch ( NoSuchRealmException e )
            {
            }
        }
        else
        {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
    }

    /**
     * Initialize the context.
     */
    private void initializeContext()
    {
        addContextValue( PlexusConstants.COMMON_CLASSLOADER, getClassLoader() );
    }

    /**
     * Initialize the configuration.
     *
     * @throws Exception
     */
    private void initializeDefaultConfiguration()
        throws Exception
    {
        InputStream is = getClassLoader().getResourceAsStream( "org/codehaus/plexus/plexus.conf" );

        if ( is == null )
        {
            throw new IllegalStateException( "The internal default plexus.conf is missing. " +
                                             "This is highly irregular, your plexus JAR is " +
                                             "most likely corrupt." );
        }

        setDefaultConfiguration( builder.parse( new InputStreamReader( is ) ) );
    }

    /**
     * Initialize the configuration.
     *
     * @throws Exception
     */
    private void initializeConfiguration()
        throws Exception
    {
        setConfiguration( builder.parse( getInterpolationConfigurationReader( getConfigurationReader() ) ) );

        processConfigurationsDirectory();
    }

    private Reader getInterpolationConfigurationReader( Reader reader )
    {
        InterpolationFilterReader interpolationFilterReader =
            new InterpolationFilterReader( reader, new ContextMapAdapter( getContext() ) );

        return interpolationFilterReader;
    }

    /**
     * Process any additional component configuration files that have been
     * specified. The specified directory is scanned recursively so configurations
     * can be within nested directories to help with component organization.
     */
    private void processConfigurationsDirectory()
        throws Exception
    {
        String s = getConfiguration().getChild( "configurations-directory" ).getValue( null );

        if ( s != null )
        {
            DefaultConfiguration componentsConfiguration =
                (DefaultConfiguration) getConfiguration().getChild( "components" );

            File configurationsDirectory = new File( s );

            if ( configurationsDirectory.exists()
                &&
                configurationsDirectory.isDirectory() )
            {
                DirectoryScanner scanner = new DirectoryScanner();
                scanner.setBasedir( configurationsDirectory );
                scanner.setIncludes( new String[]{"**/*.conf", "**/*.xml"} );
                scanner.scan();

                String[] confs = scanner.getIncludedFiles();

                for ( int i = 0; i < confs.length; i++ )
                {
                    File conf = new File( configurationsDirectory, confs[i] );

                    Configuration c = builder.parse( getInterpolationConfigurationReader( new FileReader( conf ) ) );

                    componentsConfiguration.addAllChildren( c.getChild( "components" ) );
                }
            }
        }
    }

    /**
     * Initialize Logging.
     *
     * @throws Exception
     */
    private void initializeLoggerManager()
        throws Exception
    {
        LoggerManager loggerManager =
            LoggerManagerFactory.create( getDefaultConfiguration(),
                                         getConfiguration(),
                                         getClassLoader() );

        enableLogging( loggerManager.getRootLogger() );
        setLoggerManager( loggerManager );
    }

    /**
     * Intialize the service repository.
     *
     * @throws Exception
     */
    private void initializeComponentRepository()
        throws Exception
    {
        ComponentRepository componentRepository =
            ComponentRepositoryFactory.create( getDefaultConfiguration(),
                                               getConfiguration(),
                                               getLoggerManager(),
                                               this,
                                               getClassLoader(),
                                               getContext() );

        setComponentRepository( componentRepository );
    }

    /**
     * Initialize the resource manager.
     *
     * @throws Exception
     */
    private void initializeResourceManager()
        throws Exception
    {
        DefaultResourceManager rm =
            ResourceManagerFactory.create( getDefaultConfiguration(),
                                           getConfiguration(),
                                           getLoggerManager(),
                                           getClassLoader() );

        // This needs to be completely clarified. If the container becomes the boundary
        // and barrier between all behaviour in plexus then the subsystems like classworlds
        // can't undermine the barrier. This behaviour is also dependent on composite
        // and primitive components a la SOFA.
        setResourceManager( rm );
        setClassLoader( rm.getPlexusClassLoader() );
        Thread.currentThread().setContextClassLoader( getClassLoader() );
    }

    /**
     * Initialize system properties.
     *
     * If the application needs to setup any system properties than they will
     * be initialized here.
     *
     * @throws Exception
     */
    private void initializeSystemProperties()
        throws Exception
    {
        Configuration[] systemProperties =
            getConfiguration().getChild( "system-properties" ).getChildren( "property" );

        for ( int i = 0; i < systemProperties.length; ++i )
        {
            String name = systemProperties[i].getAttribute( "name" );
            String value = systemProperties[i].getAttribute( "value" );
            System.getProperties().setProperty( name, value );

            getLogger().info( "Setting system property: [ " + name + ", " + value + " ]" );
        }
    }

    // ----------------------------------------------------------------------
    // Internal Accessors
    // ----------------------------------------------------------------------

    /**
     * Set the logger manager.
     *
     * @param loggerManager
     */
    private void setLoggerManager( LoggerManager loggerManager )
    {
        this.loggerManager = loggerManager;
    }

    /**
     * Get the logger manager.
     *
     * @return The logger manager.
     */
    private LoggerManager getLoggerManager()
    {
        return loggerManager;
    }

    /**
     *
     * @return
     */
    private Configuration getConfiguration()
    {
        return configuration;
    }

    private void setConfiguration( Configuration configuration )
    {
        this.configuration = configuration;
    }

    private Reader getConfigurationReader()
    {
        return configurationReader;
    }

    /**
     *
     * @param resourceManager
     */
    private void setResourceManager( DefaultResourceManager resourceManager )
    {
        this.resourceManager = resourceManager;
    }

    /**
     *  Retrieve the <code>ResourceManager</code>.
     *
     *  @return The resource manager.
     */
    private DefaultResourceManager getResourceManager()
    {
        return resourceManager;
    }

    /**
     *
     * @param context
     */
    private void setContext( DefaultContext context )
    {
        this.context = context;
    }

    /**
     *
     * @return
     */
    private DefaultContext getContext()
    {
        if ( context == null )
        {
            context = new DefaultContext();
        }

        return context;
    }

    /**
     *
     * @param componentRepository
     */
    private void setComponentRepository( ComponentRepository componentRepository )
    {
        this.componentRepository = componentRepository;
    }

    /**
     *
     * @return
     */
    private Configuration getDefaultConfiguration()
    {
        return defaultConfiguration;
    }

    /**
     *
     * @param defaultConfiguration
     */
    private void setDefaultConfiguration( Configuration defaultConfiguration )
    {
        this.defaultConfiguration = defaultConfiguration;
    }

    /**
     *
     * @return
     */
    private ClassWorld getClassWorld()
    {
        return classWorld;
    }
}


