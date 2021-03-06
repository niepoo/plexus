package org.codehaus.plexus.cache.test.examples.wine;

/*
 * Copyright 2001-2007 The Codehaus.
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

import java.io.Serializable;

/**
 * @since 5 February, 2007
 * @version $Id$
 * @author <a href="mailto:Olivier.LAMY@accor.com">Olivier Lamy</a>
 */
public class Wine
    implements Serializable
{
    private String name;

    private String localisation;

    public Wine( String name, String localisation )
    {
        this.name = name;
        this.localisation = localisation;
    }

    public String getLocalisation()
    {
        return localisation;
    }

    public void setLocalisation( String localisation )
    {
        this.localisation = localisation;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

}
