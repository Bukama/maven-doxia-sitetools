package org.apache.maven.doxia.siterenderer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlDefinitionDescription;
import com.gargoylesoftware.htmlunit.html.HtmlDefinitionList;
import com.gargoylesoftware.htmlunit.html.HtmlDefinitionTerm;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlHeader2;
import com.gargoylesoftware.htmlunit.html.HtmlHeader4;
import com.gargoylesoftware.htmlunit.html.HtmlListItem;
import com.gargoylesoftware.htmlunit.html.HtmlOrderedList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlParagraph;
import com.gargoylesoftware.htmlunit.html.HtmlUnorderedList;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.io.xpp3.DecorationXpp3Reader;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @author <a href="mailto:evenisse@codehaus.org>Emmanuel Venisse</a>
 * @version $Id$
 */
public class DefaultSiteRendererTest
    extends PlexusTestCase
{
    private static final String OUTPUT = "target/output";

    private Renderer renderer;

    /**
     * @see org.codehaus.plexus.PlexusTestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        super.setUp();

        renderer = (Renderer) lookup( Renderer.ROLE );

        // Safety
        FileUtils.deleteDirectory( getTestFile( OUTPUT ) );
    }

    /**
     * @see org.codehaus.plexus.PlexusTestCase#tearDown()
     */
    protected void tearDown()
        throws Exception
    {
        release( renderer );
        super.tearDown();
    }

    /**
     * @throws Exception
     */
    public void testRender()
        throws Exception
    {
        // ----------------------------------------------------------------------
        // Render the site
        // ----------------------------------------------------------------------
        DecorationModel decoration = new DecorationXpp3Reader()
            .read( new FileReader( getTestFile( "src/test/site/site.xml" ) ) );

        SiteRenderingContext context = new SiteRenderingContext();
        context.setTemplateName( "default-site.vm" );
        context.setTemplateClassLoader( getClassLoader() );
        context.setUsingDefaultTemplate( true );
        Map templateProp = new HashMap();
        templateProp.put( "outputEncoding", "UTF-8" );
        context.setTemplateProperties( templateProp );
        context.setDecoration( decoration );
        context.addSiteDirectory( getTestFile( "src/test/site" ) );

        renderer.render( renderer.locateDocumentFiles( context ).values(), context, getTestFile( OUTPUT ) );

        // ----------------------------------------------------------------------
        // Verify specific pages
        // ----------------------------------------------------------------------
        verifyCdcPage();
        verifyNestedItemsPage();
        verifyMultipleBlock();
        verifyMacro();
    }

    /**
     * @throws Exception
     */
    public void verifyCdcPage()
        throws Exception
    {
        File nestedItems = getTestFile( "target/output/cdc.html" );
        assertNotNull( nestedItems );
        assertTrue( nestedItems.exists() );
    }

    /**
     * @throws Exception
     */
    public void verifyNestedItemsPage()
        throws Exception
    {
        File nestedItems = getTestFile( "target/output/nestedItems.html" );
        assertNotNull( nestedItems );
        assertTrue( nestedItems.exists() );

        // HtmlUnit
        WebClient webClient = new WebClient();
        HtmlPage page = (HtmlPage) webClient.getPage( nestedItems.toURL() );
        assertNotNull( page );

        HtmlElement element = page.getHtmlElementById( "contentBox" );
        assertNotNull( element );
        HtmlDivision division = (HtmlDivision) element;
        assertNotNull( division );

        Iterator elementIterator = division.getAllHtmlChildElements();

        // ----------------------------------------------------------------------
        // Verify link
        // ----------------------------------------------------------------------

        HtmlDivision div = (HtmlDivision) elementIterator.next();
        assertNotNull( div );
        assertEquals( div.getAttributeValue( "class" ), "section" );

        HtmlHeader2 h2 = (HtmlHeader2) elementIterator.next();
        assertNotNull( h2 );
        assertEquals( h2.asText(), "List Section" );

        HtmlAnchor a = (HtmlAnchor) elementIterator.next();
        assertNotNull( a );
        assertEquals( a.getAttributeValue( "name" ), "List_Section" );

        // ----------------------------------------------------------------------
        // Unordered lists
        // ----------------------------------------------------------------------
        div = (HtmlDivision) elementIterator.next();
        assertNotNull( div );
        assertEquals( div.getAttributeValue( "class" ), "section" );

        HtmlHeader4 h4 = (HtmlHeader4) elementIterator.next();
        assertNotNull( h4 );
        assertEquals( h4.asText(), "Unordered lists" );

        HtmlParagraph p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Below is an unordered list, followed by six paragraphs." );

        HtmlUnorderedList ul = (HtmlUnorderedList) elementIterator.next();
        assertNotNull( ul );
        // TODO: how can this be?
        //assertEquals( ul.getFirstChild().asText(), "" );

        HtmlListItem li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 1." );

        ul = (HtmlUnorderedList) elementIterator.next();
        assertNotNull( ul );
        //assertEquals( ul.getFirstChild().asText(), "" );

        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        // No paragraph renderer
        assertEquals( li.getFirstChild().asText(), "Item 11." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        // No paragraph renderer
        assertEquals( li.getFirstChild().asText(), "Item 12." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 13." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 14." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 2." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 3." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 4." );

        ul = (HtmlUnorderedList) elementIterator.next();
        assertNotNull( ul );
        //assertEquals( ul.getFirstChild().asText(), "" );

        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 41." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 42." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 43." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 44." );

        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 1 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 2 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 3 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 4 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 5 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 6 below list." );

        // ----------------------------------------------------------------------
        // Ordered lists
        // ----------------------------------------------------------------------
        div = (HtmlDivision) elementIterator.next();
        assertNotNull( div );
        assertEquals( div.getAttributeValue( "class" ), "section" );

        h4 = (HtmlHeader4) elementIterator.next();
        assertNotNull( h4 );
        assertEquals( h4.asText(), "Ordered lists" );

        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Below is an ordered list, followed by six paragraphs." );

        HtmlOrderedList ol = (HtmlOrderedList) elementIterator.next();
        assertNotNull( ol );
        //assertEquals( ol.getFirstChild().asText(), "" );

        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 1." );

        ol = (HtmlOrderedList) elementIterator.next();
        assertNotNull( ol );
        //assertEquals( ol.getFirstChild().asText(), "" );

        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 11." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 12." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 13." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 14." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 2." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 3." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 4." );

        ol = (HtmlOrderedList) elementIterator.next();
        assertNotNull( ol );
        //assertEquals( ol.getFirstChild().asText(), "" );

        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 41." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 42." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 43." );
        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "Item 44." );

        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 1 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 2 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 3 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 4 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 5 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 6 below list." );

        // ----------------------------------------------------------------------
        // Definition lists
        // ----------------------------------------------------------------------
        div = (HtmlDivision) elementIterator.next();
        assertNotNull( div );
        assertEquals( div.getAttributeValue( "class" ), "section" );

        h4 = (HtmlHeader4) elementIterator.next();
        assertNotNull( h4 );
        assertEquals( h4.asText(), "Definition lists" );

        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Below is a definition list, followed by six paragraphs." );

        HtmlDefinitionList dl = (HtmlDefinitionList) elementIterator.next();
        assertNotNull( dl );
        //assertEquals( dl.getFirstChild().asText(), "" );

        HtmlDefinitionTerm dt = (HtmlDefinitionTerm) elementIterator.next();
        assertNotNull( dt );
        assertEquals( dt.getFirstChild().asText(), "Term 1." );
        HtmlDefinitionDescription dd = (HtmlDefinitionDescription) elementIterator.next();
        assertNotNull( dd );
        assertEquals( dd.getFirstChild().asText(), "Description 1." );

        dt = (HtmlDefinitionTerm) elementIterator.next();
        assertNotNull( dt );
        assertEquals( dt.getFirstChild().asText(), "Term 2." );
        dd = (HtmlDefinitionDescription) elementIterator.next();
        assertNotNull( dd );
        assertEquals( dd.getFirstChild().asText(), "Description 2." );

        dl = (HtmlDefinitionList) elementIterator.next();
        assertNotNull( dl );
        //assertEquals( dl.getFirstChild().asText(), "" );
        dt = (HtmlDefinitionTerm) elementIterator.next();
        assertNotNull( dt );
        assertEquals( dt.getFirstChild().asText(), "Term 21." );
        dd = (HtmlDefinitionDescription) elementIterator.next();
        assertNotNull( dd );
        assertEquals( dd.getFirstChild().asText(), "Description 21." );

        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 1 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 2 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 3 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 4 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 5 below list." );
        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "Paragraph 6 below list." );

        assertFalse( elementIterator.hasNext() );
    }

    /**
     * @throws Exception
     */
    public void verifyMultipleBlock()
        throws Exception
    {
        File multipleblock = getTestFile( "target/output/multipleblock.html" );
        assertNotNull( multipleblock );
        assertTrue( multipleblock.exists() );

        // HtmlUnit
        WebClient webClient = new WebClient();
        HtmlPage page = (HtmlPage) webClient.getPage( multipleblock.toURL() );
        assertNotNull( page );

        HtmlElement element = page.getHtmlElementById( "contentBox" );
        assertNotNull( element );
        HtmlDivision division = (HtmlDivision) element;
        assertNotNull( division );

        Iterator elementIterator = division.getAllHtmlChildElements();

        // ----------------------------------------------------------------------
        // Verify link
        // ----------------------------------------------------------------------

        HtmlDivision div = (HtmlDivision) elementIterator.next();
        assertNotNull( div );
        assertEquals( div.getAttributeValue( "class" ), "section" );

        HtmlHeader2 h2 = (HtmlHeader2) elementIterator.next();
        assertNotNull( h2 );
        assertEquals( h2.asText(), "section name" );

        HtmlAnchor a = (HtmlAnchor) elementIterator.next();
        assertNotNull( a );
        assertEquals( a.getAttributeValue( "name" ), "section_name" );

        // ----------------------------------------------------------------------
        // Paragraph
        // ----------------------------------------------------------------------

        HtmlParagraph p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "text" );

        // ----------------------------------------------------------------------
        // Unordered list
        // ----------------------------------------------------------------------

        HtmlUnorderedList ul = (HtmlUnorderedList) elementIterator.next();
        assertNotNull( ul );
        //assertEquals( ul.getFirstChild().asText(), "" );

        HtmlListItem li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "list1" );

        // ----------------------------------------------------------------------
        // Paragraph
        // ----------------------------------------------------------------------

        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "text2" );

        // ----------------------------------------------------------------------
        // Unordered list
        // ----------------------------------------------------------------------

        ul = (HtmlUnorderedList) elementIterator.next();
        assertNotNull( ul );
        //assertEquals( ul.getFirstChild().asText(), "" );

        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "list1" );

        // ----------------------------------------------------------------------
        // Paragraph
        // ----------------------------------------------------------------------

        p = (HtmlParagraph) elementIterator.next();
        assertNotNull( p );
        assertEquals( p.asText(), "text3" );

        // ----------------------------------------------------------------------
        // Unordered list
        // ----------------------------------------------------------------------

        ul = (HtmlUnorderedList) elementIterator.next();
        assertNotNull( ul );
        //assertEquals( ul.getFirstChild().asText(), "" );

        li = (HtmlListItem) elementIterator.next();
        assertNotNull( li );
        assertEquals( li.getFirstChild().asText(), "list1" );

        assertFalse( elementIterator.hasNext() );
    }

    /**
     * @throws Exception
     */
    public void verifyMacro()
        throws Exception
    {
        File macro = getTestFile( "target/output/macro.html" );
        assertNotNull( macro );
        assertTrue( macro.exists() );

        String content = IOUtil.toString( new FileReader( macro ) );
        assertEquals( content.indexOf( "</macro>" ), -1 );
    }
}
