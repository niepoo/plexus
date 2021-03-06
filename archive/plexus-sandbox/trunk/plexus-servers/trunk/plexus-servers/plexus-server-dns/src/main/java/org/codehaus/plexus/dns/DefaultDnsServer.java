package org.codehaus.plexus.dns;

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Credibility;
import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.FindServer;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @plexus.component
 */
public class DefaultDnsServer
    extends AbstractLogEnabled
    implements Initializable
{
    /**
     * A resolver instance used to retrieve DNS records.  This
     * is a reference to a third party library object.
     */
    private Resolver resolver;

    /**
     * A TTL cache of results received from the DNS server.  This
     * is a reference to a third party library object.
     */
    private Cache cache;

    /** Whether the DNS response is required to be authoritative */
    private byte dnsCredibility;

    /** The DNS servers to be used by this service */
    private List dnsServers;

    /** The MX Comparator used in the MX sort. */
    private Comparator mxComparator = new MXRecordComparator();

    private boolean autodiscover;

    private boolean authoritative;
    
    public void initialize()
        throws InitializationException
    {
        if ( autodiscover )
        {
            getLogger().info( "Autodiscovery is enabled - trying to discover your system's DNS Servers" );

            String[] serversArray = FindServer.servers();

            if ( serversArray != null )
            {
                for ( int i = 0; i < serversArray.length; i++ )
                {
                    dnsServers.add( serversArray[i] );
                    getLogger().info( "Adding autodiscovered server " + serversArray[i] );
                }
            }
        }

        if ( dnsServers == null )
        {
            dnsServers = new ArrayList();

            getLogger().info( "No DNS servers have been specified or found by autodiscovery - adding 127.0.0.1" );

            dnsServers.add( "127.0.0.1" );
        }

        // TODO: Check to see if the credibility field is being used correctly.  From the
        //       docs I don't think so
        dnsCredibility = authoritative ? Credibility.AUTH_ANSWER : Credibility.NONAUTH_ANSWER;

        getLogger().debug( "DefaultDnsServer init..." );

        // If no DNS servers were configured, default to local host
        if ( dnsServers.isEmpty() )
        {
            try
            {
                dnsServers.add( InetAddress.getLocalHost().getHostName() );
            }
            catch ( UnknownHostException ue )
            {
                dnsServers.add( "127.0.0.1" );
            }
        }

        //Create the extended resolver...
        final String[] serversArray = (String[]) dnsServers.toArray( new String[0] );

        if ( getLogger().isInfoEnabled() )
        {
            for ( int c = 0; c < serversArray.length; c++ )
            {
                getLogger().info( "DNS Server is: " + serversArray[c] );
            }
        }

        try
        {
            resolver = new ExtendedResolver( serversArray );
        }
        catch ( UnknownHostException uhe )
        {
            getLogger().fatalError(
                "DNS service could not be initialized.  The DNS servers specified are not recognized hosts.", uhe );

            throw new InitializationException( "Host are not recognized." );
        }

        cache = new Cache( DClass.IN );

        getLogger().debug( "DefaultDnsServer ...init end" );
    }

    /**
     * <p>Return a prioritized unmodifiable list of MX records
     * obtained from the server.</p>
     *
     * @param hostname domain name to look up
     * @return a unmodifiable list of MX records corresponding to
     *         this mail domain name
     */
    public Collection findMXRecords( String hostname )
    {
        Record answers[] = lookup( hostname, Type.MX );
        List servers = new ArrayList();
        try
        {
            if ( answers == null )
            {
                return servers;
            }

            MXRecord mxAnswers[] = new MXRecord[answers.length];
            for ( int i = 0; i < answers.length; i++ )
            {
                mxAnswers[i] = (MXRecord) answers[i];
            }

            Arrays.sort( mxAnswers, mxComparator );

            for ( int i = 0; i < mxAnswers.length; i++ )
            {
                servers.add( mxAnswers[i].getTarget().toString() );
                getLogger().debug(
                    new StringBuffer( "Found MX record " ).append( mxAnswers[i].getTarget().toString() ).toString() );
            }
            return Collections.unmodifiableCollection( servers );
        }
        finally
        {
            //If we found no results, we'll add the original domain name if
            //it's a valid DNS entry
            if ( servers.size() == 0 )
            {
                StringBuffer logBuffer = new StringBuffer( 128 )
                    .append( "Couldn't resolve MX records for domain " )
                    .append( hostname )
                    .append( "." );
                getLogger().error( logBuffer.toString() );
                try
                {
                    InetAddress.getByName( hostname );
                    servers.add( hostname );
                }
                catch ( UnknownHostException uhe )
                {
                    // The original domain name is not a valid host,
                    // so we can't add it to the server list.  In this
                    // case we return an empty list of servers
                    logBuffer = new StringBuffer( 128 )
                        .append( "Couldn't resolve IP address for host " )
                        .append( hostname )
                        .append( "." );
                    getLogger().error( logBuffer.toString() );
                }
            }
        }
    }

    /**
     * Looks up DNS records of the specified type for the specified name.
     * <p/>
     * This method is a public wrapper for the private implementation
     * method
     *
     * @param name the name of the host to be looked up
     * @param type the type of record desired
     */
    public Record[] lookup( String name,
                            short type )
    {
        return rawDNSLookup( name, false, type );
    }

    /**
     * Looks up DNS records of the specified type for the specified name
     *
     * @param namestr   the name of the host to be looked up
     * @param querysent whether the query has already been sent to the DNS servers
     * @param type      the type of record desired
     */
    private Record[] rawDNSLookup( String namestr,
                                   boolean querysent,
                                   short type )
    {
        Name name;
        
        try
        {
            name = Name.fromString( namestr, Name.root );
        }
        catch ( TextParseException tpe )
        {
            // TODO: Figure out how to handle this correctly.
            getLogger().error( "Couldn't parse name " + namestr, tpe );
            return null;
        }
        short dclass = DClass.IN;

        Record[] answers;
        int answerCount = 0, n = 0;

        SetResponse cached = cache.lookupRecords( name, type, dnsCredibility );
        if ( cached.isSuccessful() )
        {
            getLogger().debug( new StringBuffer( 256 )
                .append( "Retrieving MX record for " )
                .append( name ).append( " from cache" )
                .toString() );
            RRset[] rrsets = cached.answers();
            answerCount = 0;
            for ( int i = 0; i < rrsets.length; i++ )
            {
                answerCount += rrsets[i].size();
            }

            answers = new Record[answerCount];

            for ( int i = 0; i < rrsets.length; i++ )
            {
                Iterator iter = rrsets[i].rrs();
                while ( iter.hasNext() )
                {
                    Record r = (Record) iter.next();
                    answers[n++] = r;
                }
            }
        }
        else if ( cached.isNXDOMAIN() || cached.isNXRRSET() )
        {
            return null;
        }
        else if ( querysent )
        {
            return null;
        }
        else
        {
            getLogger().debug( new StringBuffer( 256 )
                .append( "Looking up MX record for " )
                .append( name )
                .toString() );
            Record question = Record.newRecord( name, type, dclass );
            Message query = Message.newQuery( question );
            Message response = null;

            try
            {
                response = resolver.send( query );
            }
            catch ( Exception ex )
            {
                getLogger().warn( "Query error!", ex );
                return null;
            }

            short rcode = response.getHeader().getRcode();
            if ( rcode == Rcode.NOERROR || rcode == Rcode.NXDOMAIN )
            {
                cache.addMessage( response );
            }

            if ( rcode != Rcode.NOERROR )
            {
                return null;
            }

            return rawDNSLookup( namestr, true, type );
        }

        return answers;
    }

    private static class MXRecordComparator
        implements Comparator
    {

        public int compare( Object a,
                            Object b )
        {
            MXRecord ma = (MXRecord) a;
            MXRecord mb = (MXRecord) b;
            return ma.getPriority() - mb.getPriority();
        }
    }
}
