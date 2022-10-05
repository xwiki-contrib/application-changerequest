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
package org.xwiki.contrib.changerequest;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Define various checks to be performed whenever a filechange is saved: either checks for changes to be added in an
 * existing change request, or changes when a change request is about to be created.
 *
 * @version $Id$
 * @since 0.9
 */
@Unstable
@Role
public interface FileChangeSavingChecker
{
    /**
     * Class defining a result of the saving checker.
     * This class only aims at holding a reason of the failure in case of failure.
     * In case of success, it doesn't provide any explanation.
     * Note that the explanation should always be a translation key.
     *
     * @version $Id$
     */
    class SavingCheckerResult
    {
        private final boolean canBeSaved;
        private final String reason;

        /**
         * Default constructor whenever the result is a success.
         */
        public SavingCheckerResult()
        {
            this.canBeSaved = true;
            this.reason = "";
        }

        /**
         * Default constructor whenever the result is a failure.
         * @param reason translation key of the explanation why the save cannot be performed.
         */
        public SavingCheckerResult(String reason)
        {
            this.canBeSaved = false;
            this.reason = reason;
        }

        /**
         * @return {@code true} if the check is successful.
         */
        public boolean canBeSaved()
        {
            return this.canBeSaved;
        }

        /**
         * @return the reason why the check was not successful.
         */
        public String getReason()
        {
            return this.reason;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            SavingCheckerResult that = (SavingCheckerResult) o;

            return new EqualsBuilder()
                .append(canBeSaved, that.canBeSaved)
                .append(reason, that.reason)
                .isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder(17, 69).append(canBeSaved).append(reason).toHashCode();
        }

        @Override
        public String toString()
        {
            return new ToStringBuilder(this)
                .append("canBeSaved", canBeSaved)
                .append("reason", reason)
                .toString();
        }
    }


    /**
     * Check if the given document reference can be added to the given change request.
     *
     * @param changeRequest the change request in which to add new changes.
     * @param documentReference the reference of the document with new changes.
     * @param changeType the type of change to be added in the change request.
     * @return {@code true} if the document can be added to the change request, {@code false} it there's an
     *          incompatibility.
     */
    SavingCheckerResult canChangeOnDocumentBeAdded(ChangeRequest changeRequest, DocumentReference documentReference,
        FileChange.FileChangeType changeType);

    /**
     * Check if the filechange can be added to the given change request.
     *
     * @param changeRequest the change request in which to add new changes.
     * @param fileChange the file change that might be added.
     * @return {@code true} if the filechange can be added to the change request, {@code false} it there's an
     *          incompatibility.
     * @since 0.14
     */
    default SavingCheckerResult canChangeOnDocumentBeAdded(ChangeRequest changeRequest, FileChange fileChange)
    {
        return canChangeOnDocumentBeAdded(changeRequest, fileChange.getTargetEntity(), fileChange.getType());
    }

    /**
     * Check if a change request can be created with the given filechange.
     *
     * @param fileChange the filechange about to be saved for creating the change request.
     * @return {@code true} iff the change request can be created with the given filechange.
     */
    SavingCheckerResult canChangeRequestBeCreatedWith(FileChange fileChange);
}
