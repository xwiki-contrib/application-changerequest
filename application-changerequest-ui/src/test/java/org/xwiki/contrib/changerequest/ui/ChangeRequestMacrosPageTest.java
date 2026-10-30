/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.changerequest.ui;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionDiffBlock;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestLineDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.FileDiffLocation;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.LineDiffLocation;
import org.xwiki.contrib.changerequest.discussions.script.ChangeRequestDiscussionScriptService;
import org.xwiki.contrib.changerequest.script.ChangeRequestScriptService;
import org.xwiki.diff.display.UnifiedDiffBlock;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.internal.macro.script.PermissionCheckerListener;
import org.xwiki.rendering.internal.macro.velocity.VelocityMacroPermissionPolicy;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Page tests for {@code ChangeRequest.Code.ChangeRequestMacros}.
 *
 * @version $Id$
 */
@ComponentList({
    // Make the velocity macro honor the restricted mode and the script right.
    PermissionCheckerListener.class,
    VelocityMacroPermissionPolicy.class
})
@HTML50ComponentList
@XWikiSyntax21ComponentList
class ChangeRequestMacrosPageTest extends PageTest
{
    private static final DocumentReference MACROS_REFERENCE =
        new DocumentReference("xwiki", List.of("ChangeRequest", "Code"), "ChangeRequestMacros");

    private static final DocumentReference CHANGE_REQUEST_REFERENCE =
        new DocumentReference("xwiki", List.of("ChangeRequest", "Data", "CR1"), "WebHome");

    private XWikiDocument macrosDocument;

    @BeforeEach
    void setUp() throws Exception
    {
        this.macrosDocument = loadPage(MACROS_REFERENCE);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.SCRIPT)).thenReturn(true);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(eq(Right.SCRIPT), any()))
            .thenReturn(true);
    }

    @Test
    void displayDescription() throws Exception
    {
        XWikiDocument changeRequestDocument = new XWikiDocument(CHANGE_REQUEST_REFERENCE);
        changeRequestDocument.setSyntax(Syntax.XWIKI_2_1);
        changeRequestDocument.setContent("Some **description**\n\n"
            + "{{velocity}}#set ($x = 'EXEC')${x}UTED{{/velocity}}");
        this.xwiki.saveDocument(changeRequestDocument, this.context);
        changeRequestDocument = this.xwiki.getDocument(CHANGE_REQUEST_REFERENCE, this.context);
        this.context.setDoc(changeRequestDocument);

        // Display the description the way the change request sheet does: in the context of the change request
        // document, with the macros page as the secure document.
        String sheet = this.macrosDocument.getContent()
            + "\n\n{{velocity}}\n{{html clean=\"false\"}}\n#displayDescription()\n{{/html}}\n{{/velocity}}";
        String result =
            changeRequestDocument.getRenderedContent(sheet, Syntax.XWIKI_2_1, this.macrosDocument, true, this.context);

        assertThat(result, containsString("<span class=\"description-content\">"));
        assertThat(result, containsString("<strong>description</strong>"));
        assertThat(result, not(containsString("EXECUTED")));
    }

    @Test
    void displayDiffBlockMetadataWithSpecialCharacters() throws Exception
    {
        String attachmentName =
            "{{/html}}{{html clean=\"false\"}}<img src=x id=attachmentName />{{/html}}{{html clean=\"false\"}}";
        String propertyName =
            "{{/html}}{{html clean=\"false\"}}<img src=x id=propertyName />{{/html}}{{html clean=\"false\"}}";

        DocumentReference targetReference = new DocumentReference("xwiki", "Space", "Target");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(new FileDiffLocation("diff1", "Space.Target"),
            LineDiffLocation.DiffDocumentPart.ATTACHMENT, "Space.Target@" + attachmentName, propertyName,
            1, LineDiffLocation.LineChange.ADDED);
        ChangeRequestDiscussionDiffBlock diffBlock = new ChangeRequestDiscussionDiffBlock(new UnifiedDiffBlock<>(),
            new ChangeRequestLineDiffReference("CR1", lineDiffLocation));

        ChangeRequestDiscussionScriptService discussionScriptService = mock(ChangeRequestDiscussionScriptService.class);
        when(discussionScriptService.getDiffBlockMetadata(any())).thenReturn(diffBlock);
        when(discussionScriptService.getPageTitle(diffBlock)).thenReturn("Target");
        when(discussionScriptService.getDiffBlockReference(diffBlock))
            .thenReturn(new EntityReference(attachmentName, EntityType.ATTACHMENT, targetReference));
        ChangeRequestScriptService changeRequestScriptService = mock(ChangeRequestScriptService.class);
        when(changeRequestScriptService.<ScriptService>get("discussion")).thenReturn(discussionScriptService);
        this.componentManager.registerComponent(ScriptService.class, "changerequest", changeRequestScriptService);

        XWikiDocument changeRequestDocument = new XWikiDocument(CHANGE_REQUEST_REFERENCE);
        this.context.setDoc(changeRequestDocument);

        // Display the diff block metadata the way the discussion page does: inside an HTML macro, with the macros page
        // as the secure document.
        String sheet = this.macrosDocument.getContent()
            + "\n\n{{velocity}}\n{{html clean=\"false\"}}\n"
            + "#displayDiffBlockMetadata($services.changerequest.discussion.getDiffBlockMetadata($NULL))\n"
            + "{{/html}}\n{{/velocity}}";
        String result =
            changeRequestDocument.getRenderedContent(sheet, Syntax.XWIKI_2_1, this.macrosDocument, true, this.context);

        assertThat(result, containsString("<div class=\"diff-reference-detail-location\">"));
        assertThat(result, containsString("&#60;img src=x id=attachmentName /&#62;"));
        assertThat(result, containsString("&#60;img src=x id=propertyName /&#62;"));
        assertThat(result, not(containsString("<img")));
    }
}
