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
package org.xwiki.contrib.changerequest.internal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.EditForm;
import com.xpn.xwiki.web.Utils;

/**
 * Default handler for managing URLs such as {@code basewiki/editcr/Space/Page}.
 * Those URls allow to edit a page without edit rights in order to create a new change request.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Named("editcr")
@Singleton
public class EditChangeRequestResourceHandler extends AbstractResourceReferenceHandler<EntityResourceAction>
{
    private static final String TDOC = "tdoc";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentmixedReferenceResolver;

    @Inject
    private ContextualAuthorizationManager autorization;

    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Override
    public List<EntityResourceAction> getSupportedResourceReferences()
    {
        return Collections.singletonList(new EntityResourceAction("editcr"));
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        XWikiContext context = this.contextProvider.get();
        String changerequestId = context.getRequest().getParameter("changerequest");

        try {
            if (!StringUtils.isEmpty(changerequestId)) {
                Optional<ChangeRequest> changeRequestOptional = this.changeRequestStorageManager.load(changerequestId);
                changeRequestOptional.ifPresent(changeRequest -> {
                    DocumentReference currentReference = context.getDoc().getDocumentReferenceWithLocale();
                    changeRequest.getLatestFileChangeFor(currentReference).ifPresent(fileChange -> {
                        context.setDoc((XWikiDocument) fileChange.getModifiedDocument());
                    });
                });
            }
            this.prepareEditedDocument(context);
            // We pretend to be in edit action to avoid getting redirection in templates that checks the action
            // we need to call that only after the documents are prepared though to avoid getting blocked by the
            // security checks.
            context.setAction("edit");
            // We are directly relying on Utils#parseTemplate because we want the plugin manager to properly
            // handle the javascript placeholders and it avoids duplicating code.
            Utils.parseTemplate("changerequest/editcr", true, context);
        } catch (XWikiException e) {
            throw new ResourceReferenceHandlerException("Error when parsing editcr template.", e);
        } catch (ChangeRequestException e) {
            throw new ResourceReferenceHandlerException(
                String.format("Error when trying to load change request with id [%s]", changerequestId), e);
        }
        chain.handleNext(reference);
    }

    // FIXME: All methods below have been taken from EditAction and XWikiAction in XWiki platform.
    // It would be nicer to create an EditResourceReferenceHandler in platform and to make that one inherits from it.

    /**
     * Determines the edited document (translation) and updates it based on the template specified on the request and
     * any additional request parameters that overwrite the default values from the template.
     *
     * @param context the XWiki context
     * @return the edited document
     * @throws XWikiException if something goes wrong
     */
    protected XWikiDocument prepareEditedDocument(XWikiContext context) throws XWikiException
    {
        EditForm editForm = new EditForm();
        editForm.setRequest(context.getRequest());
        editForm.readRequest();
        context.setForm(editForm);
        // Determine the edited document (translation).
        XWikiDocument editedDocument = getEditedDocument(context);

        // Update the edited document based on the template specified on the request.
        readFromTemplate(editedDocument, editForm.getTemplate(), context);

        // The default values from the template can be overwritten by additional request parameters.
        updateDocumentTitleAndContentFromRequest(editedDocument, context);
        editedDocument.readObjectsFromForm(editForm, context);

        // Set the current user as creator, author and contentAuthor when the edited document is newly created to avoid
        // using XWikiGuest instead (because those fields were not previously initialized).
        if (editedDocument.isNew()) {
            editedDocument.setCreatorReference(context.getUserReference());
            editedDocument.setAuthorReference(context.getUserReference());
            editedDocument.setContentAuthorReference(context.getUserReference());
        }

        // Expose the edited document on the XWiki context and the Velocity context.
        putDocumentOnContext(editedDocument, context);

        return editedDocument;
    }

    /**
     * Helper used by various actions to initialize a document by copying a template to it.
     *
     * @param document the document to update
     * @param template the template to copy
     * @param context the XWiki context
     * @return true if the document was updated, false otherwise (for example when the current user does not have view
     *         right on the template document)
     * @throws XWikiException when failing to copy the template
     * @since 12.10.6
     * @since 13.2RC1
     */
    protected boolean readFromTemplate(XWikiDocument document, String template, XWikiContext context)
        throws XWikiException
    {
        DocumentReference templateReference = resolveTemplate(template);

        if (templateReference != null) {
            document.readFromTemplate(templateReference, context);

            return true;
        }

        return false;
    }

    /**
     * Helper used resolve the template passed to the action if the current user have access to it.
     *
     * @param template the template to copy
     * @return the reference of the template if not empty and the current user have access to it
     * @since 12.10.6
     * @since 13.2RC1
     */
    protected DocumentReference resolveTemplate(String template)
    {
        if (StringUtils.isNotBlank(template)) {
            DocumentReference templateReference = this.currentmixedReferenceResolver.resolve(template);

            // Make sure the current user have access to the template document before copying it
            if (this.autorization.hasAccess(Right.VIEW, templateReference)) {
                return templateReference;
            }
        }

        return null;
    }

    /**
     * There are three important use cases:
     * <ul>
     * <li>editing or creating the original translation (for the default language)</li>
     * <li>editing an existing document translation</li>
     * <li>creating a new translation.</li>
     * </ul>
     * Most of the code deals with the really bad way the default language can be specified (empty string, 'default' or
     * a real language code).
     *
     * @param context the XWiki context
     * @return the edited document translation based on the language specified on the request
     * @throws XWikiException if something goes wrong
     */
    private XWikiDocument getEditedDocument(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = context.getDoc();
        boolean hasTranslation = doc != context.get(TDOC);

        // We have to clone the context document because it is cached and the changes we are going to make are valid
        // only for the duration of the current request.
        doc = doc.clone();
        context.put("doc", doc);

        EditForm editForm = (EditForm) context.getForm();
        doc.readDocMetaFromForm(editForm, context);

        String language = context.getWiki().getLanguagePreference(context);
        if (doc.isNew() && doc.getDefaultLanguage().equals("")) {
            doc.setDefaultLanguage(language);
        }

        String languageToEdit = StringUtils.isEmpty(editForm.getLanguage()) ? language : editForm.getLanguage();

        // If no specific language is set or if it is "default" then we edit the current doc.
        if (languageToEdit == null || languageToEdit.equals("default")) {
            languageToEdit = "";
        }
        // If the document is new or if the language to edit is the default language then we edit the default
        // translation.
        if (doc.isNew() || doc.getDefaultLanguage().equals(languageToEdit)) {
            languageToEdit = "";
        }
        // If the doc does not exist in the language to edit and the language was not explicitly set in the URL then
        // we edit the default document translation. This prevents use from creating unneeded translations.
        if (!hasTranslation && StringUtils.isEmpty(editForm.getLanguage())) {
            languageToEdit = "";
        }

        // Initialize the translated document.
        XWikiDocument tdoc;
        if (languageToEdit.equals("")) {
            // Edit the default document translation (default language).
            tdoc = doc;
            if (doc.isNew()) {
                doc.setDefaultLanguage(language);
                doc.setLanguage("");
            }
        } else if (!hasTranslation && context.getWiki().isMultiLingual(context)) {
            // Edit a new translation.
            tdoc = new XWikiDocument(doc.getDocumentReference());
            tdoc.setLanguage(languageToEdit);
            tdoc.setDefaultLocale(doc.getDefaultLocale());
            // Mark the translation. It's important to know whether a document is a translation or not, especially
            // for the sheet manager which needs to access the objects using the default document not one of its
            // translations.
            tdoc.setTitle(doc.getTitle());
            tdoc.setContent(doc.getContent());
            tdoc.setSyntax(doc.getSyntax());
            tdoc.setAuthorReference(context.getUserReference());
            tdoc.setStore(doc.getStore());
        } else {
            // Edit an existing translation. Clone the translated document object to be sure that the changes we are
            // going to make will last only for the duration of the current request.
            tdoc = ((XWikiDocument) context.get(TDOC)).clone();
        }

        return tdoc;
    }

    /**
     * Updates the title and content of the given document with values taken from the 'title' and 'content' request
     * parameters or based on the document section specified on the request.
     *
     * @param document the document whose title and content should be updated
     * @param context the XWiki context
     * @throws XWikiException if something goes wrong
     */
    private void updateDocumentTitleAndContentFromRequest(XWikiDocument document, XWikiContext context)
        throws XWikiException
    {
        // Check if section editing is enabled and if a section is specified.
        boolean sectionEditingEnabled = context.getWiki().hasSectionEdit(context);
        int sectionNumber = sectionEditingEnabled ? NumberUtils.toInt(context.getRequest().getParameter("section")) : 0;
        this.scriptContextManager.getCurrentScriptContext()
            .setAttribute("sectionNumber", sectionNumber, ScriptContext.ENGINE_SCOPE);

        // Update the edited content.
        EditForm editForm = (EditForm) context.getForm();
        if (editForm.getContent() != null) {
            document.setContent(editForm.getContent());
        } else if (sectionNumber > 0) {
            document.setContent(document.getContentOfSection(sectionNumber));
        }

        // Update the edited title.
        if (editForm.getTitle() != null) {
            document.setTitle(editForm.getTitle());
        } else if (sectionNumber > 0 && document.getSections().size() > 0) {
            // The edited content is either the content of the specified section or the content provided on the
            // request. We assume the content provided on the request is meant to overwrite the specified section.
            // In both cases the document content is currently having one section, so we can take its title.
            String sectionTitle = document.getDocumentSection(1).getSectionTitle();
            if (StringUtils.isNotBlank(sectionTitle)) {
                // We cannot edit the page title while editing a page section so this title is for display only.
                String sectionPlainTitle = document.getRenderedContent(sectionTitle, document.getSyntax().toIdString(),
                    Syntax.PLAIN_1_0.toIdString(), context);
                document.setTitle(localizePlainOrKey("core.editors.content.titleField.sectionEditingFormat",
                    document.getRenderedTitle(Syntax.PLAIN_1_0, context), sectionNumber, sectionPlainTitle));
            }
        }
    }

    protected String localizePlainOrKey(String key, Object... parameters)
    {
        return StringUtils.defaultString(this.contextualLocalizationManager.getTranslationPlain(key, parameters), key);
    }

    /**
     * Exposes the given document in the XWiki context and the Velocity context under the 'tdoc' and 'cdoc' keys.
     *
     * @param document the document to expose
     * @param context the XWiki context
     */
    private void putDocumentOnContext(XWikiDocument document, XWikiContext context)
    {
        context.put(TDOC, document);
        // Old XWiki applications that are still using the inline action might expect the cdoc (content document) to be
        // properly set on the context. Let's expose the given document also as cdoc for backward compatibility.
        context.put("cdoc", context.get(TDOC));
    }
}
