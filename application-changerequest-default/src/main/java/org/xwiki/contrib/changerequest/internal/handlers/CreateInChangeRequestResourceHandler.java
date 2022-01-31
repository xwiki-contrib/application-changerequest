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
package org.xwiki.contrib.changerequest.internal.handlers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.validation.EntityNameValidationConfiguration;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.script.ScriptContextManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.CreateActionRequestHandler;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

/**
 * Default handler for managing URLs such as {@code basewiki/createcr/Space/Page}.
 * Those URls allow to create a new page without edit rights in order to be requested in a change request.
 * This class is greatly inspired from {@link com.xpn.xwiki.web.CreateAction} but with a big limitation: it doesn't
 * allow to use template providers that are immediately saving the page.
 *
 * @version $Id$
 * @since 0.10
 */
@Component
@Named("createcr")
@Singleton
public class CreateInChangeRequestResourceHandler extends AbstractResourceReferenceHandler<EntityResourceAction>
{
    /**
     * The name of the parent parameter.
     */
    private static final String PARENT = "parent";

    /**
     * The name of the space reference parameter.
     */
    private static final String SPACE_REFERENCE = "spaceReference";

    /**
     * The name parameter.
     */
    private static final String NAME = "name";

    /**
     * The name of the template field inside the template provider, or the template parameter which can be sent
     * directly, without passing through the template provider.
     */
    private static final String TEMPLATE = "template";

    /**
     * Internal name for a flag determining if we are creating a Nested Space or a terminal document.
     */
    private static final String IS_SPACE = "isSpace";

    /**
     * Space homepage document name.
     */
    private static final String WEBHOME = "WebHome";

    /**
     * The action to perform when creating a new page from a template.
     *
     * @version $Id$
     */
    private enum ActionOnCreate
    {
        /**
         * Go to edit mode without saving.
         */
        EDIT("edit"),

        /**
         * Save and then go to edit mode.
         */
        SAVE_AND_EDIT("saveandedit"),

        /**
         * Save and then go to view mode.
         */
        SAVE_AND_VIEW("saveandview");

        private static final Map<String, ActionOnCreate> BY_ACTION = new HashMap<>();

        static {
            for (ActionOnCreate actionOnCreate : values()) {
                BY_ACTION.put(actionOnCreate.action, actionOnCreate);
            }
        }

        private final String action;

        ActionOnCreate(String action)
        {
            this.action = action;
        }

        public static ActionOnCreate valueOfAction(String action)
        {
            return BY_ACTION.get(action);
        }
    }

    private static final LocalDocumentReference TEMPLATE_PROVIDER_CLASS_REFERENCE =
        new LocalDocumentReference("XWiki", "TemplateProviderClass");

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private EntityNameValidationManager entityNameValidationManager;

    @Inject
    private EntityNameValidationConfiguration entityNameValidationConfiguration;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Inject
    private Provider<DocumentReference> documentReferenceProvider;

    @Inject
    private CSRFToken csrfToken;

    @Inject
    private Logger logger;

    @Override
    public List<EntityResourceAction> getSupportedResourceReferences()
    {
        return Collections.singletonList(new EntityResourceAction("createcr"));
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        XWikiContext context = this.contextProvider.get();
        CreateActionRequestHandler handler = new CreateActionRequestHandler(context);

        try {
            // Read the request and extract the passed information.
            handler.processRequest();

            // Save the determined values so we have them available in the action template.
            ScriptContext scontext = this.scriptContextManager.getCurrentScriptContext();
            scontext.setAttribute(SPACE_REFERENCE, handler.getSpaceReference(), ScriptContext.ENGINE_SCOPE);
            scontext.setAttribute(NAME, handler.getName(), ScriptContext.ENGINE_SCOPE);
            scontext.setAttribute(IS_SPACE, handler.isSpace(), ScriptContext.ENGINE_SCOPE);
            // put the available templates on the context, for the .vm to not compute them again
            // Note that we filter out the providers that do not use the edit action.
            scontext.setAttribute("availableTemplateProviders",
                this.filterTemplateProviders(handler.getAvailableTemplateProviders()),
                ScriptContext.ENGINE_SCOPE);
            scontext.setAttribute("recommendedTemplateProviders",
                this.filterTemplateProviders(handler.getRecommendedTemplateProviders()),
                ScriptContext.ENGINE_SCOPE);

            if (isCreateReady(handler)) {
                XWikiDocument newDocument = context.getWiki().getDocument(handler.getNewDocumentReference(), context);
                // create is finally valid, can be executed
                doCreate(context, newDocument, handler.isSpace(), handler.getTemplateProvider());
            } else {
                // We are directly relying on Utils#parseTemplate because we want the plugin manager to properly
                // handle the javascript placeholders and it avoids duplicating code.
                Utils.parseTemplate("changerequest/createcr", true, context);
            }
        } catch (XWikiException | ChangeRequestException e) {
            throw new ResourceReferenceHandlerException(
                String.format("Error when processing request for createcr of reference [%s].", reference), e);
        }
        chain.handleNext(reference);
    }

    private List<Document> filterTemplateProviders(List<Document> templateProviders)
    {
        return templateProviders.stream().filter(this::filterTemplateProvider).collect(Collectors.toList());
    }

    private boolean filterTemplateProvider(Document templateProvider)
    {
        XWikiContext context = this.contextProvider.get();
        try {
            // we retrieve the actual doc, to be sure to be able to retrieve the xobject.
            XWikiDocument providerXWikiDoc =
                context.getWiki().getDocument(templateProvider.getDocumentReference(), context);
            BaseObject templateProviderObject = providerXWikiDoc.getXObject(TEMPLATE_PROVIDER_CLASS_REFERENCE);
            if (templateProviderObject != null) {
                ActionOnCreate actionOnCreate = getActionOnCreate(templateProviderObject);
                return actionOnCreate == ActionOnCreate.EDIT;
            }
        } catch (XWikiException e) {
            logger.error("Error while loading [{}] to filter the template providers.",
                templateProvider.getDocumentReference(), e);
        }
        return false;
    }

    private boolean isCreateReady(CreateActionRequestHandler handler) throws XWikiException
    {
        XWikiContext context = contextProvider.get();
        DocumentReference newDocumentReference = handler.getNewDocumentReference();
        if (newDocumentReference == null) {
            return false;
        } else {
            XWikiDocument newDocument = context.getWiki().getDocument(newDocumentReference, context);
            boolean isNameOK = !handler.isDocumentPathTooLong(newDocumentReference)
                && this.isEntityReferenceNameValid(newDocumentReference);
            boolean isTemplateOK = handler.hasTemplate()
                && handler.isTemplateProviderAllowedToCreateInCurrentSpace();
            boolean isDocumentOK = !handler.isDocumentAlreadyExisting(newDocument)
                && !StringUtils.isBlank(handler.getType());
            return isNameOK && isTemplateOK && isDocumentOK;
        }
    }

    /**
     * Ensure that the given entity reference is valid according to the configured name strategy. Always returns true if
     * the name strategy is not found.
     *
     * @param entityReference the entity reference name to validate
     * @return {@code true} if the entity reference name is valid according to the name strategy.
     */
    private boolean isEntityReferenceNameValid(EntityReference entityReference)
    {
        if (this.entityNameValidationManager.getEntityReferenceNameStrategy() != null
            && this.entityNameValidationConfiguration.useValidation()) {
            if (!this.entityNameValidationManager.getEntityReferenceNameStrategy().isValid(entityReference)) {
                Object[] args = {this.localEntityReferenceSerializer.serialize(entityReference)};
                XWikiException invalidNameException = new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_APP_DOCUMENT_NAME_INVALID,
                    "Cannot create document {0} because its name does not respect the name strategy of the wiki.", null,
                    args);
                ScriptContext scontext = this.scriptContextManager.getCurrentScriptContext();
                scontext.setAttribute("createException", invalidNameException, ScriptContext.ENGINE_SCOPE);
                return false;
            }
        }
        return true;
    }

    /**
     * Actually executes the create, after all preconditions have been verified.
     *
     * @param context the context of this action
     * @param newDocument the document to be created
     * @param isSpace whether the document is a space webhome or a page
     * @param templateProvider the template provider to create from
     * @throws XWikiException in case anything goes wrong accessing xwiki documents
     */
    private void doCreate(XWikiContext context, XWikiDocument newDocument, boolean isSpace, BaseObject templateProvider)
        throws XWikiException, ChangeRequestException
    {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();

        String parent = getParent(request, doc, isSpace, context);

        // get the title of the page to create, as specified in the parameters
        String title = getTitle(request, newDocument, isSpace);

        // get the template from the template parameter, to allow creation directly from template, without
        // forcing to create a template provider for each template creation
        String template = getTemplate(templateProvider, request);

        // Read from the template provide the action to perform when creating the page.
        ActionOnCreate actionOnCreate = getActionOnCreate(templateProvider);

        String action = null;
        switch (actionOnCreate) {
            case SAVE_AND_EDIT:
            case SAVE_AND_VIEW:
                // TODO: Provide support for those, with adding the changes in a change request.
                throw new ChangeRequestException("The save&edit and save&view modes from template providers are not yet"
                    + "supported in ChangeRequest.");

            case EDIT:
            default:
                // Contrarily to the standard CreateAction we don't allow to use inline mode since it's not supported
                // in change request yet.
                action = "editcr";
                break;
        }

        // Perform a redirection to the selected action of the document to create.
        String redirectParams = getRedirectParameters(parent, title, template, actionOnCreate);
        String redirectURL = newDocument.getURL(action, redirectParams, context);
        redirectURL = context.getResponse().encodeRedirectURL(redirectURL);
        if (context.getRequest().getParameterMap().containsKey("ajax")) {
            // If this template is displayed from a modal popup, send a header in the response notifying that a
            // redirect must be performed in the calling page.
            context.getResponse().setHeader("redirect", redirectURL);
        } else {
            // Perform the redirect
            sendRedirect(context.getResponse(), redirectURL);
        }
    }

    /**
     * Perform a redirect to the given URL.
     * @param response the response to use to perform the redirect
     * @param url the location of the redirect
     * @throws XWikiException in case of IOException when performing the redirect.
     */
    protected void sendRedirect(XWikiResponse response, String url) throws XWikiException
    {
        try {
            if (url != null) {
                response.sendRedirect(response.encodeRedirectURL(url));
            }
        } catch (IOException e) {
            Object[] args = {url};
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_REDIRECT_EXCEPTION,
                "Exception while sending redirect to page {0}", e, args);
        }
    }

    private String getRedirectParameters(String parent, String title, String template, ActionOnCreate actionOnCreate)
    {
        if (actionOnCreate == ActionOnCreate.SAVE_AND_EDIT) {
            // We don't need to pass any parameters because the document is saved before the redirect using the
            // parameter values.
            return null;
        }

        String redirectParams = "template=" + Util.encodeURI(template, null);
        if (parent != null) {
            redirectParams += "&parent=" + Util.encodeURI(parent, null);
        }
        if (title != null) {
            redirectParams += "&title=" + Util.encodeURI(title, null);
        }
        if (actionOnCreate == ActionOnCreate.SAVE_AND_VIEW) {
            // Add the CSRF token because we redirect to save action.
            redirectParams += "&form_token=" + Util.encodeURI(this.csrfToken.getToken(), null);
        }

        return redirectParams;
    }

    /**
     * @param templateProvider the set template provider, if any
     * @param request the request on which to fallback
     * @return the string reference of the document to use as template or {@code ""} if none set
     */
    private String getTemplate(BaseObject templateProvider, XWikiRequest request)
    {
        String result = "";

        if (templateProvider != null) {
            result = templateProvider.getStringValue(TEMPLATE);
        } else if (request.getParameter(TEMPLATE) != null) {
            result = request.getParameter(TEMPLATE);
        }

        return result;
    }

    /**
     * @param request the current request for which this action is executed
     * @param doc the current document
     * @param isSpace {@code true} if the request is to create a space, {@code false} if a page should be created
     * @param context the XWiki context
     * @return the serialized reference of the parent to create the document for
     */
    private String getParent(XWikiRequest request, XWikiDocument doc, boolean isSpace, XWikiContext context)
    {
        // This template can be passed a parent document reference in parameter (using the "parent" parameter).
        // If a parent parameter is passed, use it to set the parent when creating the new Page or Space.
        // If no parent parameter was passed:
        // * use the current document
        // ** if we're creating a new page and if the current document exists or
        // * use the Main space's WebHome
        // ** if we're creating a new page and the current document does not exist.
        String parent = request.getParameter(PARENT);
        if (StringUtils.isEmpty(parent)) {
            if (doc.isNew()) {
                // Use the Main space's WebHome.
                DocumentReference parentRef =
                    this.documentReferenceProvider.get().setWikiReference(context.getWikiReference());

                parent = this.localEntityReferenceSerializer.serialize(parentRef);
            } else {
                // Use the current document.
                DocumentReference parentRef = doc.getDocumentReference();

                parent = this.localEntityReferenceSerializer.serialize(parentRef);
            }
        }

        return parent;
    }

    /**
     * @param request the current request for which this action is executed
     * @param newDocument the document to be created
     * @param isSpace {@code true} if the request is to create a space, {@code false} if a page should be created
     * @return the title of the page to be created. If no request parameter is set, the page name is returned for a new
     *         page and the space name is returned for a new space
     */
    private String getTitle(XWikiRequest request, XWikiDocument newDocument, boolean isSpace)
    {
        String title = request.getParameter("title");
        if (StringUtils.isEmpty(title)) {
            if (isSpace) {
                title = newDocument.getDocumentReference().getLastSpaceReference().getName();
            } else {
                title = newDocument.getDocumentReference().getName();
                // Avoid WebHome titles for pages that are really space homepages.
                if (WEBHOME.equals(title)) {
                    title = newDocument.getDocumentReference().getLastSpaceReference().getName();
                }
            }
        }

        return title;
    }

    /**
     * @param templateProvider the template provider for this creation
     * @return the {@link ActionOnCreate} based on the action defined in the template provider, or
     *         {@link ActionOnCreate#EDIT} by default.
     */
    private ActionOnCreate getActionOnCreate(BaseObject templateProvider)
    {
        if (templateProvider != null) {
            String action = templateProvider.getStringValue("action");
            ActionOnCreate actionOnCreate = ActionOnCreate.valueOfAction(action);
            if (actionOnCreate != null) {
                return actionOnCreate;
            }
        }

        // Default action when creating a page from a template.
        return ActionOnCreate.EDIT;
    }
}
