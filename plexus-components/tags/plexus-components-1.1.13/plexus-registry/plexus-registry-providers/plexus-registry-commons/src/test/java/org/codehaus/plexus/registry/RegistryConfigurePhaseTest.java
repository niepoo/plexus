package org.codehaus.plexus.registry;

/*
 * Copyright 2007 The Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.PlexusTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Test the Plexus lifecycle phase that configures components from the registry if possible.
 */
public class RegistryConfigurePhaseTest
    extends PlexusTestCase
{
    public void testConfiguration()
        throws Exception
    {
        Component component = (Component) lookup( Component.ROLE, "default" );

        assertEquals( "value11", component.getConfigKey() );

        assertEquals( 1, component.getConfigNumber() );

        Properties properties = new Properties();
        properties.setProperty( "key1", "value12" );
        properties.setProperty( "key2", "value13" );
        assertEquals( properties, component.getConfigProperties() );

        Map map = new HashMap();
        map.put( "key1", "value14" );
        map.put( "key2", "value15" );
        assertEquals( map, component.getConfigMap() );

        List list = new ArrayList();
        list.add( "s1" );
        list.add( "s2" );
        assertEquals( list, component.getConfigList() );

        assertEquals( "baz", component.getConfigNested().getFoo() );

        assertEquals( "value1", component.getKey() );

        assertEquals( 2, component.getNumber() );

        map = new HashMap();
        map.put( "org.jpox.PreserveCase", "always" );
        map.put( "key", "value3" );
        assertEquals( map, component.getMap() );

        properties = new Properties();
        properties.setProperty( "org.jpox.PreserveCase", "always" );
        properties.setProperty( "key", "value2" );
        assertEquals( properties, component.getProperties() );

        list = new ArrayList();
        list.add( "S1" );
        list.add( "S2" );
        assertEquals( list, component.getList() );

        assertEquals( "bar", component.getNested().getFoo() );

        list = new ArrayList();
        list.add( "v1" );
        list.add( "v2" );
        list.add( "v3" );
        list.add( "v4" );
        assertEquals( list, component.getMergeList() );

        properties = new Properties();
        properties.setProperty( "key31", "value31" );
        properties.setProperty( "key32", "value32" );
        assertEquals( properties, component.getMergeProperties() );
    }
}
